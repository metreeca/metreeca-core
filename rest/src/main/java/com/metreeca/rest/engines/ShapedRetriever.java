/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.engines;

import com.metreeca.form.*;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Pruner;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.query.TupleQueryResultHandlerException;
import org.eclipse.rdf4j.query.algebra.Compare;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.math.BigInteger;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;

import static org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil.compare;


final class ShapedRetriever {

	// !!! log compilation/execution times

	private static final Logger logger=Logger.getLogger(ShapedRetriever.class.getName()); // !!! migrate logging to Graph?


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final RepositoryConnection connection;


	public ShapedRetriever(final RepositoryConnection connection) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		this.connection=connection;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Collection<Statement> retrieve(final IRI resource, final Query query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return query.map(new Query.Probe<Collection<Statement>>() {

			@Override public Collection<Statement> probe(final Edges edges) { return edges(resource, edges); }

			@Override public Collection<Statement> probe(final Stats stats) { return stats(resource, stats); }

			@Override public Collection<Statement> probe(final Items items) { return items(resource, items); }

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> edges(final IRI resource, final Edges edges) {

		final Shape shape=edges.getShape();
		final List<Order> orders=edges.getOrders();
		final int offset=edges.getOffset();
		final int limit=edges.getLimit();

		final Object root=0; // root identifier // !!! review

		final Collection<Statement> template=new ArrayList<>();
		final Collection<Statement> model=new LinkedHashSet<>();

		// construct results are serialized with no ordering guarantee >> transfer data as tuples to preserve ordering

		connection.prepareTupleQuery(compile(new _SPARQL() {

			@Override public Object code() {

				final Shape pattern=shape.map(new Redactor(Form.mode, Form.convey)).map(new Optimizer());
				final Shape selector=shape.map(new Redactor(Form.mode, Form.filter)).map(new Pruner()).map(new Optimizer());

				link(pattern, root);
				link(selector, root);

				final List<String> variables=template(pattern, template);

				return list(

						"# edges query\f",

						prefixes(),

						"select ", variables.isEmpty() ? var(root) : projection(variables), " where {\f",

						filter(selector, orders, offset, limit), "\f",
						pattern(pattern), "\f",
						sorters(var(root), orders), "\f", // !!! (€) don't extract if already present in pattern

						"\f} order by ", criteria(var(root), orders)

				);

			}

		})).evaluate(new AbstractTupleQueryResultHandler() {

			@Override public void handleSolution(final BindingSet bindings) {

				final Value match=bindings.getValue(root.toString());

				if ( match != null ) {

					if ( !match.equals(resource) ) {
						model.add(statement(resource, LDP.CONTAINS, match));
					}

					template.forEach(statement -> {

						final Resource subject=statement.getSubject();
						final Value object=statement.getObject();

						final Value source=subject instanceof BNode ? bindings.getValue(((BNode)subject).getID()) : subject;
						final Value target=object instanceof BNode ? bindings.getValue(((BNode)object).getID()) : object;

						if ( source instanceof Resource && target != null ) {
							model.add(statement((Resource)source, statement.getPredicate(), target));
						}

					});

				}

			}

		});

		return model;
	}

	private Collection<Statement> stats(final IRI resource, final Stats stats) {

		final Shape shape=stats.getShape();
		final List<IRI> path=stats.getPath();

		final Model model=new LinkedHashModel();

		final Collection<Literal> counts=new ArrayList<>();
		final Collection<Value> mins=new ArrayList<>();
		final Collection<Value> maxs=new ArrayList<>();

		connection.prepareTupleQuery(compile(new _SPARQL() {

			@Override public Object code() {

				final Shape selector=shape
						.map(new Redactor(Form.mode, Form.filter))
						.map(new Pruner())
						.map(new Optimizer());

				final Object source=var(id(selector));
				final Object target=path.isEmpty() ? source : var(id());

				return list(

						"# stats query\f",

						prefixes(),

						"select ?type (count(distinct ", target, ") as ?count)"
								+" (min(", target, ") as ?min) (max(", target, ") as ?max) {\f",

						roots(selector), "\f",
						filters(selector), "\f",

						// !!! use filter(selector, emptySet(), 0, 0) to support sampling

						path.isEmpty() ? null : list(source, path(path), target), '\f',

						// !!! review datatype for blanks/iris

						"bind(if(isBlank(", target, "), rdfs:Resource, if(isIRI(", target, "), rdfs:Resource,"
								+" datatype(", target, "))) as ?type)\f",

						"} group by ?type order by desc(?count) ?type\n"

						// !!! support ordering/slicing?

				);

			}

		})).evaluate(new AbstractTupleQueryResultHandler() {

			@Override public void handleSolution(final BindingSet bindings) {

				final Resource type=(Resource)bindings.getValue("type");
				final Literal count=(Literal)bindings.getValue("count");
				final Value min=bindings.getValue("min");
				final Value max=bindings.getValue("max");

				if ( type != null ) { model.add(resource, Form.stats, type); }
				if ( type != null && count != null ) { model.add(type, Form.count, count); }
				if ( type != null && min != null ) { model.add(type, Form.min, min); }
				if ( type != null && max != null ) { model.add(type, Form.max, max); }

				counts.add(count);
				mins.add(min);
				maxs.add(max);

			}

		});

		model.add(resource, Form.count, literal(BigInteger.valueOf(counts.stream()
				.filter(Objects::nonNull)
				.mapToLong(Literal::longValue)
				.sum())));

		mins.stream()
				.filter(Objects::nonNull)
				.reduce((x, y) -> compare(x, y, Compare.CompareOp.LT) ? x : y)
				.ifPresent(min -> model.add(resource, Form.min, min));

		maxs.stream()
				.filter(Objects::nonNull)
				.reduce((x, y) -> compare(x, y, Compare.CompareOp.GT) ? x : y)
				.ifPresent(max -> model.add(resource, Form.max, max));

		return model;
	}

	private Collection<Statement> items(final IRI resource, final Items items) {

		final Shape shape=items.getShape();
		final List<IRI> path=items.getPath();

		final Model model=new LinkedHashModel();

		connection.prepareTupleQuery(compile(new _SPARQL() {

			@Override public Object code() {

				final Shape selector=shape
						.map(new Redactor(Form.mode, Form.filter))
						.map(new Pruner())
						.map(new Optimizer());

				final Object source=var(id(selector));
				final Object target=path.isEmpty() ? source : var(id());

				return list(

						"# items query\f",

						prefixes(),

						"select"
								+" (", target, " as ?value)"
								+" (sample(?l) as ?label)"
								+" (sample(?n) as ?notes)"
								+" (count(distinct ", source, ") as ?count)",

						" {\f",

						roots(selector), "\f",
						filters(selector), "\f",

						// !!! use filter(selector, emptySet(), 0, 0) to support sampling

						path.isEmpty() ? null : list(source, path(path), target), '\f',

						"optional { ", target, " rdfs:label ?l }\f", // !!! language
						"optional { ", target, " rdfs:comment ?n }\f", // !!! language

						"} group by ", target, " having ( ?count > 0 ) order by desc(?count) ?value\n"

						// !!! support ordering/slicing?

				);

			}

		})).evaluate(new AbstractTupleQueryResultHandler() {
			@Override public void handleSolution(final BindingSet bindings) throws TupleQueryResultHandlerException {

				final Value value=bindings.getValue("value");
				final Value count=bindings.getValue("count");
				final Value label=bindings.getValue("label");
				final Value notes=bindings.getValue("notes");

				final BNode item=bnode();

				if ( item != null ) { model.add(resource, Form.items, item); }
				if ( item != null && value != null ) { model.add(item, Form.value, value); }
				if ( item != null && count != null ) { model.add(item, Form.count, count); }

				// !!! manage multiple languages

				if ( label != null ) { model.add((Resource)value, RDFS.LABEL, label); }
				if ( notes != null ) { model.add((Resource)value, RDFS.COMMENT, notes); }

			}
		});

		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String compile(final _SPARQL sparql) {

		final String query=sparql.compile();

		if ( logger.isLoggable(Level.FINE) ) {
			logger.fine("evaluating SPARQL query "+indent(query, true)+(query.endsWith("\n") ? "" : "\n"));
		}

		return query;
	}

}
