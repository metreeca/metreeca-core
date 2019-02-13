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
import com.metreeca.form.probes.*;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Values;
import com.metreeca.tray.rdf.Graph;

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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.things.Snippets.*;
import static com.metreeca.form.things.Values.*;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.query.algebra.evaluation.util.QueryEvaluationUtil.compare;


final class GraphRetriever extends GraphProcessor {

	private final Graph graph=tool(Graph.Factory);


	Collection<Statement> retrieve(final Resource focus, final Query query) {
		return graph.query(connection -> {
			return query.map(new Query.Probe<Collection<Statement>>() {

				@Override public Collection<Statement> probe(final Edges edges) { return edges(connection, focus, edges); }

				@Override public Collection<Statement> probe(final Stats stats) { return stats(connection, focus, stats); }

				@Override public Collection<Statement> probe(final Items items) { return items(connection, focus, items); }

			});
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> edges(final RepositoryConnection connection, final Resource focus, final Edges edges) {

		final Shape shape=edges.getShape();
		final List<Order> orders=edges.getOrders();
		final int offset=edges.getOffset();
		final int limit=edges.getLimit();

		final Object root=new Object(); // root object

		final Collection<Statement> model=new LinkedHashSet<>();
		final Collection<Statement> template=new ArrayList<>();

		// construct results are serialized with no ordering guarantee >> transfer data as tuples to preserve ordering

		evaluate(() -> connection.prepareTupleQuery(compile(() -> {

			final Shape pattern=shape.map(new Redactor(Form.mode, Form.convey)).map(new Optimizer());
			final Shape selector=shape.map(new Redactor(Form.mode, Form.filter)).map(new Pruner()).map(new Optimizer());

			return source(nothing(id(root, pattern, selector)), snippet(

					"# edges query\n"
							+"\n"
							+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
							+"\n"
							+"select {variables} where {\n"
							+"\n"
							+"\t{filter}\n"
							+"\n"
							+"\t{pattern}\n"
							+"\n"
							+"\t{sorters}\n"
							+"\n"
							+"} order by {criteria}",

					(Snippet)(source, identifiers) -> Stream
							.concat(
									Stream.of(identifiers.apply(root, root)), /// always project root
									pattern.map(new TemplateProbe(pattern, s -> identifiers.apply(s, s), template::add))
							)
							.distinct()
							.sorted()
							.forEachOrdered(id -> source.accept(" ?"+id)),

					filter(selector, orders, offset, limit),
					pattern(pattern),

					sorters(root, orders), // !!! (€) don't extract if already present in pattern
					criteria(root, orders)

			));

		})).evaluate(new AbstractTupleQueryResultHandler() {

			@Override public void handleSolution(final BindingSet bindings) {

				final Value match=bindings.getValue("0"); // root id

				if ( match != null ) {

					if ( !match.equals(focus) ) {
						model.add(statement(focus, LDP.CONTAINS, match));
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

		}));

		return model;
	}

	private Collection<Statement> stats(final RepositoryConnection connection, final Resource focus, final Stats stats) {

		final Shape shape=stats.getShape();
		final List<IRI> path=stats.getPath();

		final Model model=new LinkedHashModel();

		final Collection<Literal> counts=new ArrayList<>();
		final Collection<Value> mins=new ArrayList<>();
		final Collection<Value> maxs=new ArrayList<>();

		evaluate(() -> connection.prepareTupleQuery(compile(() -> {

			final Shape selector=shape
					.map(new Redactor(Form.mode, Form.filter))
					.map(new Pruner())
					.map(new Optimizer());

			final Object source=var(selector);
			final Object target=path.isEmpty() ? source : var();

			return source(snippet(

					"# stats query\n"
							+"\n"
							+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
							+"\n"
							+"select ?type\t\n"
							+"\n"
							+"\t(count(distinct {target}) as ?count)\n"
							+"\t(min({target}) as ?min)\n"
							+"\t(max({target}) as ?max) \n"
							+"\n"
							+"\bwhere {\n"
							+"\n"
							+"\t{roots}\n"
							+"\n"
							+"\t{filters}\n"
							+"\n"
							+"\t{path}\n"
							+"\n"
							+"\tbind(if(isBlank({target}) || isIRI({target}), rdfs:Resource, datatype({target})) as ?type)\n"
							+"\n"
							+"} group by ?type order by desc(?count) ?type",


					target,

					roots(selector),
					filters(selector), // !!! use filter(selector, emptySet(), 0, 0) to support sampling

					edge(source, path, target)

					// !!! support ordering/slicing?

			));

		})).evaluate(new AbstractTupleQueryResultHandler() {

			@Override public void handleSolution(final BindingSet bindings) {

				final Resource type=(Resource)bindings.getValue("type");
				final Literal count=(Literal)bindings.getValue("count");
				final Value min=bindings.getValue("min");
				final Value max=bindings.getValue("max");

				if ( type != null ) { model.add(focus, Form.stats, type); }
				if ( type != null && count != null ) { model.add(type, Form.count, count); }
				if ( type != null && min != null ) { model.add(type, Form.min, min); }
				if ( type != null && max != null ) { model.add(type, Form.max, max); }

				counts.add(count);
				mins.add(min);
				maxs.add(max);

			}

		}));

		model.add(focus, Form.count, literal(BigInteger.valueOf(counts.stream()
				.filter(Objects::nonNull)
				.mapToLong(Literal::longValue)
				.sum())));

		mins.stream()
				.filter(Objects::nonNull)
				.reduce((x, y) -> compare(x, y, Compare.CompareOp.LT) ? x : y)
				.ifPresent(min -> model.add(focus, Form.min, min));

		maxs.stream()
				.filter(Objects::nonNull)
				.reduce((x, y) -> compare(x, y, Compare.CompareOp.GT) ? x : y)
				.ifPresent(max -> model.add(focus, Form.max, max));

		return model;
	}

	private Collection<Statement> items(final RepositoryConnection connection, final Resource focus, final Items items) {

		final Shape shape=items.getShape();
		final List<IRI> path=items.getPath();

		final Model model=new LinkedHashModel();

		evaluate(() -> connection.prepareTupleQuery(compile(() -> {

			final Shape selector=shape
					.map(new Redactor(Form.mode, Form.filter))
					.map(new Pruner())
					.map(new Optimizer());

			final Object source=var(selector);
			final Object target=path.isEmpty() ? source : var();

			return source(snippet(

					"# items query\n"
							+"\n"
							+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
							+"\n"
							+"select ({target} as ?value)\t\n"
							+"\n"
							+"\t(sample(?l) as ?label)\n"
							+"\t(sample(?n) as ?notes)\n"
							+"\t(count(distinct {source}) as ?count)\n"
							+"\n"
							+"\bwhere {\n"
							+"\n"
							+"\t{roots}\n"
							+"\t\n"
							+"\t{filters}\n"
							+"\t\n"
							+"\t{path}\n"
							+"\t\n"
							+"\toptional { {target} rdfs:label ?l }\n"
							+"\toptional { {target} rdfs:comment ?n }\n"
							+"\t\t\n"
							+"} group by {target} having ( ?count > 0 ) order by desc(?count) ?value",

					target, source,


					roots(selector),
					filters(selector), // !!! use filter(selector, emptySet(), 0, 0) to support sampling

					edge(source, path, target)

					// !!! handle label/comment language
					// !!! support ordering/slicing?

			));

		})).evaluate(new AbstractTupleQueryResultHandler() {
			@Override public void handleSolution(final BindingSet bindings) throws TupleQueryResultHandlerException {

				final Value value=bindings.getValue("value");
				final Value count=bindings.getValue("count");
				final Value label=bindings.getValue("label");
				final Value notes=bindings.getValue("notes");

				final BNode item=bnode();

				if ( item != null ) { model.add(focus, Form.items, item); }
				if ( item != null && value != null ) { model.add(item, Form.value, value); }
				if ( item != null && count != null ) { model.add(item, Form.count, count); }

				// !!! manage multiple languages

				if ( label != null ) { model.add((Resource)value, RDFS.LABEL, label); }
				if ( notes != null ) { model.add((Resource)value, RDFS.COMMENT, notes); }

			}
		}));

		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Snippet filter(final Shape shape, final Collection<Order> orders, final int offset, final int limit) {
		return shape.equals(and()) ? nothing() : shape.equals(or()) ? snippet("filter (false)") : snippet(

				"{ select {root} {\n"
						+"\n"
						+"\t{roots}\n"
						+"\n"
						+"\t{filters}\n"
						+"\n"
						+"\t{sorters}\n"
						+"\n"
						+"} {orders} {offset} {limit} }",

				var(shape),

				roots(shape),
				filters(shape),

				offset > 0 || limit > 0 ? sorters(shape, orders) : null,
				offset > 0 || limit > 0 ? snippet(" order by ", criteria(shape, orders)) : null,

				offset > 0 ? snippet(" offset ", offset) : null,
				limit > 0 ? snippet(" limit ", limit) : null

		);

	}


	private static Snippet pattern(final Shape shape) {
		return shape.map(new PatternProbe(shape));
	}

	private static Snippet roots(final Shape shape) { // root universal constraints
		return all(shape)
				.map(values -> values(shape, values))
				.orElse(null);
	}

	private static Snippet filters(final Shape shape) {
		return shape.map(new FilterProbe(shape));
	}

	private static Snippet sorters(final Object root, final Collection<Order> orders) {
		return snippet(orders.stream()
				.filter(order -> !order.getPath().isEmpty()) // root already retrieved
				.map(order -> snippet(
						"optional { {root} {path} {order} }\n", var(root), path(order.getPath()), var(order))
				)
		);
	}

	private static Snippet criteria(final Object root, final Collection<Order> orders) {
		return list(Stream.concat(

				orders.stream().map(order -> snippet(
						order.isInverse() ? "desc({criterion})" : "asc({criterion})",
						var(order.getPath().isEmpty() ? root : order)
				)),

				orders.stream()
						.map(Order::getPath)
						.filter(List::isEmpty)
						.findFirst()
						.map(empty -> Stream.empty())
						.orElseGet(() -> Stream.of(var(root)))  // root as last resort, unless already used

		), " ");
	}

	private static Snippet values(final Shape source, final Collection<Value> values) {
		return snippet("\n\nvalues {source} {\n{values}\n}\n\n",

				var(source), list(values.stream().map(Values::format), "\n")

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TemplateProbe extends Traverser<Stream<Integer>> {

		private final Shape focus;

		private final Function<Shape, Integer> identifier;
		private final Consumer<Statement> template;


		private TemplateProbe(
				final Shape focus, final Function<Shape, Integer> identifier, final Consumer<Statement> template
		) {

			this.focus=focus;

			this.identifier=identifier;
			this.template=template;
		}


		@Override public Stream<Integer> probe(final Shape shape) { return Stream.empty(); }


		@Override public Stream<Integer> probe(final Field field) {

			final IRI iri=field.getIRI();
			final Shape shape=field.getShape();

			final Integer source=identifier.apply(focus);
			final Integer target=identifier.apply(shape);

			final Resource snode=bnode(source.toString());
			final Resource tnode=bnode(target.toString());

			template.accept(direct(iri) ? statement(snode, iri, tnode) : statement(tnode, inverse(iri), snode));

			return Stream.concat(
					Stream.of(source, target),
					shape.map(new TemplateProbe(shape, identifier, template))
			);

		}


		@Override public Stream<Integer> probe(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Integer> probe(final Or or) {
			return or.getShapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Integer> probe(final When when) {
			return Stream.concat(
					when.getPass().map(this),
					when.getFail().map(this)
			);
		}

	}

	private static final class PatternProbe extends Traverser<Snippet> {

		// !!! (€) remove optionals if term is required or if exists a filter on the same path

		private final Shape shape;


		private PatternProbe(final Shape shape) {
			this.shape=shape;
		}


		@Override public Snippet probe(final Guard guard) {
			throw new UnsupportedOperationException("partially redacted shape");
		}


		@Override public Snippet probe(final Field field) {

			final IRI iri=field.getIRI();
			final Shape shape=field.getShape();

			return snippet( // (€) optional unless universal constraints are present

					all(shape).isPresent() ? "\n\n{pattern}\n\n" : "\n\noptional {\n\n{pattern}\n\n}\n\n",

					snippet(
							edge(var(this.shape), iri, var(shape)), "\n",
							pattern(shape)
					)

			);
		}


		@Override public Snippet probe(final And and) {
			return snippet(and.getShapes().stream().map(s -> s.map(this)));
		}

		@Override public Snippet probe(final Or or) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Snippet probe(final When when) {
			throw new UnsupportedOperationException("conditional shape");
		}

	}

	private static final class FilterProbe implements Shape.Probe<Snippet> {

		private final Shape source;


		private FilterProbe(final Shape source) {
			this.source=source;
		}


		@Override public Snippet probe(final Meta meta) {
			return nothing();
		}

		@Override public Snippet probe(final Guard guard) {
			throw new UnsupportedOperationException("partially redacted shape");
		}


		@Override public Snippet probe(final Datatype datatype) {
			throw new UnsupportedOperationException("datatype constraint");
		}

		@Override public Snippet probe(final Clazz clazz) {
			return snippet(var(source), " a/rdfs:subClassOf* ", format(clazz.getIRI()), " .");
		}

		@Override public Snippet probe(final MinExclusive minExclusive) {
			return snippet("filter ( {source} > {value} )", var(source), format(minExclusive.getValue()));
		}

		@Override public Snippet probe(final MaxExclusive maxExclusive) {
			return snippet("filter ( {source} < {value} )", var(source), format(maxExclusive.getValue()));
		}

		@Override public Snippet probe(final MinInclusive minInclusive) {
			return snippet("filter ( {source} >= {value} )", var(source), format(minInclusive.getValue()));
		}

		@Override public Snippet probe(final MaxInclusive maxInclusive) {
			return snippet("filter ( {source} <= {value} )", var(source), format(maxInclusive.getValue()));
		}

		@Override public Snippet probe(final Pattern pattern) {
			return snippet("filter regex({source}, '{pattern}', '{flags}')",
					var(source), pattern.getText().replace("\\", "\\\\"), pattern.getFlags()
			);
		}

		@Override public Snippet probe(final Like like) {
			return snippet("filter regex({source}, '{pattern}')",
					var(source), like.toExpression().replace("\\", "\\\\")
			);
		}

		@Override public Snippet probe(final MinLength minLength) {
			return snippet("filter (strlen(str({source})) >= {limit} )", var(source), minLength.getLimit());
		}

		@Override public Snippet probe(final MaxLength maxLength) {
			return snippet("filter (strlen(str({source})) <= {limit} )", var(source), maxLength.getLimit());
		}

		@Override public Snippet probe(final MinCount minCount) {
			throw new UnsupportedOperationException("minimum focus size constraint");
		}

		@Override public Snippet probe(final MaxCount maxCount) {
			throw new UnsupportedOperationException("maximum focus size constraint");
		}


		@Override public Snippet probe(final In in) {
			throw new UnsupportedOperationException("focus range constraint");
		}

		@Override public Snippet probe(final All all) {
			return nothing(); // universal constraints handled by field probe
		}

		@Override public Snippet probe(final Any any) {

			// values-based filtering (as opposed to in-based filtering) works also or root terms // !!! performance?

			return values(source, any.getValues());
		}


		@Override public Snippet probe(final Field field) {

			final IRI iri=field.getIRI();
			final Shape shape=field.getShape();

			return snippet(

					shape instanceof All // filtering hook
							? null // ($) only if actually referenced by filters
							: edge(var(source), iri, var(shape)),

					all(shape) // target universal constraints
							.map(values -> values.stream().map(value -> edge(var(source), iri, format(value))))
							.orElse(null),

					"\n\n",

					filters(shape)
			);
		}


		@Override public Snippet probe(final And and) {
			return snippet(and.getShapes().stream().map(shape -> shape.map(this)));
		}

		@Override public Snippet probe(final Or or) {
			throw new UnsupportedOperationException("disjunction pattern"); // !!! tbi
		}

		@Override public Snippet probe(final When when) {
			throw new UnsupportedOperationException("conditional pattern"); // !!! tbi
		}

	}

}
