/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.spec.sparql;

import com.metreeca.spec.Query;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.probes.Optimizer;
import com.metreeca.spec.probes.Pruner;
import com.metreeca.spec.queries.Edges;
import com.metreeca.spec.queries.Items;
import com.metreeca.spec.queries.Stats;
import com.metreeca.spec.shifts.Step;
import com.metreeca.spec.things._Cell;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
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

import static com.metreeca.spec.Shape.mode;
import static com.metreeca.spec.things.Lists.list;
import static com.metreeca.spec.things.Strings.indent;
import static com.metreeca.spec.things.Values.bnode;
import static com.metreeca.spec.things.Values.literal;

import static org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil.compare;


final class SPARQLReader {

	// !!! log compilation/execution times

	private static final Logger logger=Logger.getLogger(SPARQLReader.class.getName()); // !!! migrate logging to Graph?


	private final RepositoryConnection connection; // !!! as argument?


	public SPARQLReader(final RepositoryConnection connection) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		this.connection=connection;
	}


	public _Cell process(final Query query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return query.accept(new Query.Probe<_Cell>() {

			@Override public _Cell visit(final Edges edges) { return graph(edges); }

			@Override public _Cell visit(final Stats stats) { return stats(stats); }

			@Override public _Cell visit(final Items items) { return items(items); }

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Cell graph(final Edges edges) {

		final Shape shape=edges.getShape();
		final List<Query.Order> orders=edges.getOrders();
		final int offset=edges.getOffset();
		final int limit=edges.getLimit();

		final Object root=0; // root identifier // !!! review

		final Model model=new LinkedHashModel();
		final Collection<Value> focus=new LinkedHashSet<>();

		final Collection<Statement> template=new ArrayList<>();

		// construct results are serialized with no ordering guarantee >> transfer data as tuples to preserve ordering

		connection.prepareTupleQuery(compile(new SPARQL() {

			@Override public Object code() {

				final Shape pattern=shape.accept(mode(Spec.verify));
				final Shape selector=shape.accept(mode(Spec.filter)).accept(new Pruner()).accept(new Optimizer());

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
					focus.add(match);
				}

				template.forEach(statement -> {

					final Resource subject=statement.getSubject();
					final Value object=statement.getObject();

					final Value source=subject instanceof BNode ? bindings.getValue(((BNode)subject).getID()) : subject;
					final Value target=object instanceof BNode ? bindings.getValue(((BNode)object).getID()) : object;

					if ( source instanceof Resource && target != null ) {
						model.add((Resource)source, statement.getPredicate(), target);
					}

				});

			}

		});

		return _Cell.cell(model).insert(focus);
	}

	private _Cell stats(final Stats stats) {

		final Shape shape=stats.getShape();
		final List<Step> path=stats.getPath();

		final Model model=new LinkedHashModel();

		final Collection<Literal> counts=new ArrayList<>();
		final Collection<Value> mins=new ArrayList<>();
		final Collection<Value> maxs=new ArrayList<>();

		connection.prepareTupleQuery(compile(new SPARQL() {

			@Override public Object code() {

				final Shape selector=shape.accept(mode(Spec.filter)).accept(new Pruner()).accept(new Optimizer());

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

				if ( type != null ) { model.add(Spec.meta, Spec.stats, type); }
				if ( type != null && count != null ) { model.add(type, Spec.count, count); }
				if ( type != null && min != null ) { model.add(type, Spec.min, min); }
				if ( type != null && max != null ) { model.add(type, Spec.max, max); }

				counts.add(count);
				mins.add(min);
				maxs.add(max);

			}

		});

		model.add(Spec.meta, Spec.count, literal(BigInteger.valueOf(counts.stream()
				.filter(Objects::nonNull)
				.mapToLong(Literal::longValue)
				.sum())));

		mins.stream()
				.filter(Objects::nonNull)
				.reduce((x, y) -> compare(x, y, Compare.CompareOp.LT) ? x : y)
				.ifPresent(min -> model.add(Spec.meta, Spec.min, min));

		maxs.stream()
				.filter(Objects::nonNull)
				.reduce((x, y) -> compare(x, y, Compare.CompareOp.GT) ? x : y)
				.ifPresent(max -> model.add(Spec.meta, Spec.max, max));

		return _Cell.cell(model).insert(Spec.meta);
	}

	private _Cell items(final Items items) {

		final Shape shape=items.getShape();
		final List<Step> path=items.getPath();

		final Model model=new LinkedHashModel();

		connection.prepareTupleQuery(compile(new SPARQL() {

			@Override public Object code() {

				final Shape selector=shape.accept(mode(Spec.filter)).accept(new Pruner()).accept(new Optimizer());

				final Object source=var(id(selector));
				final Object target=path.isEmpty() ? source : var(id());

				return list(

						"# items query\f",

						prefixes(),

						"select (", target, " as ?value) (sample(?l) as ?label) (count(distinct ", source, ") as ?count) {\f",

						roots(selector), "\f",
						filters(selector), "\f",

						// !!! use filter(selector, emptySet(), 0, 0) to support sampling

						path.isEmpty() ? null : list(source, path(path), target), '\f',

						"optional { ", target, " rdfs:label ?l }\f", // !!! language

						"} group by ", target, " having ( ?count > 0 ) order by desc(?count) ?value\n"

						// !!! support ordering/slicing?

				);

			}

		})).evaluate(new AbstractTupleQueryResultHandler() {
			@Override public void handleSolution(final BindingSet bindings) throws TupleQueryResultHandlerException {

				final Value value=bindings.getValue("value");
				final Value count=bindings.getValue("count");
				final Value label=bindings.getValue("label");

				final BNode item=bnode();

				if ( item != null ) { model.add(Spec.meta, Spec.items, item); }
				if ( item != null && value != null ) { model.add(item, Spec.value, value); }
				if ( item != null && count != null ) { model.add(item, Spec.count, count); }

				// !!! manage multiple labels

				if ( label != null ) {
					model.add((Resource)value, RDFS.LABEL, label);
				}

			}
		});

		return _Cell.cell(model).insert(Spec.meta);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String compile(final SPARQL sparql) {

		final String query=sparql.compile();

		if ( logger.isLoggable(Level.FINE) ) {
			logger.fine("evaluating SPARQL query "+indent(query, true)+(query.endsWith("\n") ? "" : "\n"));
		}

		return query;
	}

}
