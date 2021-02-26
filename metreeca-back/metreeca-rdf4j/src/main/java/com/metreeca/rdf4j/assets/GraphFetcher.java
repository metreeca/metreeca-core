/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rdf4j.assets;

import com.metreeca.json.Query;
import com.metreeca.json.*;
import com.metreeca.json.queries.*;
import com.metreeca.json.shapes.*;
import com.metreeca.rest.assets.Logger;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.math.BigInteger;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static com.metreeca.json.Focus.focus;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.rdf4j.assets.Snippets.*;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.assets.Logger.time;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

final class GraphFetcher extends Query.Probe<Collection<Statement>> { // !!! refactor

	static Shape convey(final Shape shape) { // !!! caching
		return shape.redact(retain(Mode, Convey));
	}

	static Shape filter(final Shape shape) { // !!! caching
		return shape.redact(retain(Mode, Filter));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static Iterable<Statement> outline(final IRI resource, final Shape shape) {
		return anchor(resource, shape).outline(resource).collect(toList());
	}

	private static Shape anchor(final Value resource, final Shape shape) {

		return resource.stringValue().endsWith("/")

				// container: connect to the focus using ldp:contains, unless otherwise specified in the filtering shape

				? shape.empty() ? field(inverse(LDP.CONTAINS), focus()) : shape

				// resource: constraint to the focus

				: and(all(focus()), shape);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final RepositoryConnection connection;

	private final IRI resource;

	private final Logger logger=asset(logger());


	GraphFetcher(final RepositoryConnection connection, final IRI resource) {
		this.connection=connection;
		this.resource=resource;
	}


	private String compile(final Supplier<String> generator) {
		return time(generator).apply((t, v) -> logger

				.debug(this, () -> format("executing %s", v.endsWith("\n") ? v : v+"\n"))
				.debug(this, () -> format("generated in <,%d> ms", t))

		);
	}

	private void evaluate(final Runnable task) {
		time(task).apply((t) -> logger

				.debug(this, () -> format("evaluated in <%,d> ms", t))

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Value value(final Value value) {
		return value instanceof Focus
				? ((Focus)value).resolve(resource)
				: value;
	}

	private Set<Value> values(final Collection<Value> values) {
		return values.stream()
				.map(this::value)
				.collect(toSet());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Collection<Statement> probe(final Items items) {

		final Shape shape=items.shape();
		final List<Order> orders=items.orders();
		final int offset=items.offset();
		final int limit=items.limit();

		final Object root=new Object(); // root object

		final Collection<Statement> model=new LinkedHashSet<>();
		final Collection<Statement> template=new ArrayList<>();

		// construct results are serialized with no ordering guarantee >> transfer data as tuples to preserve order

		final Shape pattern=convey(shape);
		final Shape selector=anchor(resource, filter(shape));

		evaluate(() -> connection.prepareTupleQuery(compile(() -> source(

				nothing(id(root, pattern, selector)), // link root to pattern and selector shapes

				snippet(

						"# items query\n"
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
										pattern.map(new TemplateProbe(pattern, s -> identifiers.apply(s, s),
												template::add))
								)
								.distinct()
								.sorted()
								.forEachOrdered(id -> source.accept(" ?"+id)),

						selector(selector, orders, offset, limit),
						pattern(pattern),

						sorters(root, orders), // !!! (€) don't extract if already present in pattern
						criteria(root, orders)

				)

		))).evaluate(new AbstractTupleQueryResultHandler() {

			@Override public void handleSolution(final BindingSet bindings) {

				final Value match=bindings.getValue("0"); // root id

				if ( match != null ) {

					if ( !match.equals(resource) ) {
						model.add(statement(resource, LDP.CONTAINS, match));
					}

					if ( template.isEmpty() ) { // wildcard shape => symmetric+labelled concise bounded description

						description((Resource)match);

					} else {

						template(bindings);

					}

				}

			}

			private void template(final BindingSet bindings) {
				template.forEach(statement -> {

					final Resource subject=statement.getSubject();
					final Value object=statement.getObject();

					final Value source=subject instanceof BNode ? bindings.getValue(((BNode)subject).getID()) :
							subject;
					final Value target=object instanceof BNode ? bindings.getValue(((BNode)object).getID()) :
							object;

					if ( source instanceof Resource && target != null ) {
						model.add(statement((Resource)source, statement.getPredicate(), target));
					}

				});
			}

			private void description(final Resource resource) {

				final Collection<Resource> visited=new HashSet<>();
				final Collection<Resource> pending=new HashSet<>(singleton(resource));

				while ( !pending.isEmpty() ) {

					final GraphQuery query=connection.prepareGraphQuery(source(

							"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
									+"\n"
									+"construct {\n"
									+"\n"
									+"\t?r ?p ?o. ?o a ?t; rdfs:label ?l; rdfs:comment ?c.\n"
									+"\t?s ?p ?r. ?s a ?t; rdfs:label ?l; rdfs:comment ?c.\n"
									+"\t\n"
									+"} where {\n"
									+"\n"
									+"\tvalues ?r {\n"
									+"\t\t{resources}\n"
									+"\t}\n"
									+"\n"
									+"\t{\n"
									+"\t\n"
									+"\t\t?r ?p ?o\n"
									+"\n"
									+"\t\toptional { ?o a ?t }\n"
									+"\t\toptional { ?o rdfs:label ?l }\n"
									+"\t\toptional { ?o rdfs:comment ?c }\n"
									+"\n"
									+"\t} union {\n"
									+"\n"
									+"\t\t?s ?p ?r\n"
									+"\n"
									+"\t\toptional { ?s a ?t }\n"
									+"\t\toptional { ?s rdfs:label ?l }\n"
									+"\t\toptional { ?s rdfs:comment ?c }\n"
									+"\n"
									+"\t}\n"
									+"\n"
									+"}",

							list(pending.stream().map(Values::format), "\n")

					));

					pending.clear();

					query.evaluate(new AbstractRDFHandler() {
						@Override public void handleStatement(final Statement statement) {

							model.add(statement);

							final Resource subject=statement.getSubject();
							final Value object=statement.getObject();

							if ( subject instanceof BNode && visited.add(subject) ) {
								pending.add(subject);
							}

							if ( object instanceof BNode && visited.add((Resource)object) ) {
								pending.add((Resource)object);
							}

						}
					});

				}
			}

		}));

		return model;
	}

	@Override public Collection<Statement> probe(final Stats stats) {

		final Shape shape=stats.shape();
		final List<IRI> path=stats.path();
		final int offset=stats.offset();
		final int limit=stats.limit();

		final Model model=new LinkedHashModel();

		final Collection<BigInteger> counts=new ArrayList<>();
		final Collection<Value> mins=new ArrayList<>();
		final Collection<Value> maxs=new ArrayList<>();

		final Shape selector=anchor(resource, filter(shape));

		final Object source=var(selector);
		final Object target=path.isEmpty() ? source : var();

		evaluate(() -> connection.prepareTupleQuery(compile(() -> source(snippet(

				"# stats query\n"
						+"\n"
						+"prefix : <{base}>\n"
						+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
						+"\n"
						+"select ?type\t\n"
						+"\n"
						+"\t(count(distinct {target}) as ?count)\n"
						+"\t(min({target}) as ?min)\n"
						+"\t(max({target}) as ?max) \n"
						+"\n"
						+
						"where {\n"
						+"\n"
						+"\t{roots}\n"
						+"\n"
						+"\t{filters}\n"
						+"\n"
						+"\t{path}\n"
						+"\n"
						+"\tbind (if(isBlank({target}), :bnode, if(isIRI({target}), :iri, datatype({target}))) as "
						+"?type)\n"
						+"\n"
						+"}\n"
						+"\n"
						+"group by ?type\n"
						+"having ( count({target}) > 0 )\n"
						+"order by desc(?count) ?type\n"
						+"{offset}\n"
						+"{limit}",

				// !!! support slicing?

				GraphEngine.Base,
				target,

				roots(selector),
				filters(selector), // !!! use filter(selector, emptySet(), 0, 0) to support sampling

				path(source, path, target),

				offset > 0 ? snippet("offset {offset}", offset) : null,
				limit > 0 ? snippet("limit {limit}", limit) : null

		)))).evaluate(new AbstractTupleQueryResultHandler() {

			@Override public void handleSolution(final BindingSet bindings) {

				final Resource type=(Resource)bindings.getValue("type");

				final BigInteger count=integer(bindings.getValue("count")).orElse(BigInteger.ZERO);

				final Value min=bindings.getValue("min");
				final Value max=bindings.getValue("max");

				// ;(virtuoso) counts are returned as xsd:int… cast to stay consistent

				if ( type != null ) { model.add(resource, GraphEngine.stats, type); }
				if ( type != null ) { model.add(type, GraphEngine.count, literal(count)); }

				if ( type != null && min != null ) { model.add(type, GraphEngine.min, min); }
				if ( type != null && max != null ) { model.add(type, GraphEngine.max, max); }

				counts.add(count);
				mins.add(min);
				maxs.add(max);

			}

		}));

		model.add(resource, GraphEngine.count, literal(counts.stream()
				.filter(Objects::nonNull)
				.reduce(BigInteger.ZERO, BigInteger::add)
		));

		mins.stream()
				.filter(Objects::nonNull)
				.reduce((x, y) -> compare(x, y) < 0 ? x : y)
				.ifPresent(min -> model.add(resource, GraphEngine.min, min));

		maxs.stream()
				.filter(Objects::nonNull)
				.reduce((x, y) -> compare(x, y) > 0 ? x : y)
				.ifPresent(max -> model.add(resource, GraphEngine.max, max));

		return model;
	}

	@Override public Collection<Statement> probe(final Terms terms) {

		final Shape shape=terms.shape();
		final List<IRI> path=terms.path();
		final int offset=terms.offset();
		final int limit=terms.limit();

		final Model model=new LinkedHashModel();

		final Shape selector=anchor(resource, filter(shape));

		final Object source=var(selector);
		final Object target=path.isEmpty() ? source : var();

		evaluate(() -> connection.prepareTupleQuery(compile(() -> source(snippet(

				"# terms query\n"
						+"\n"
						+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
						+"\n"
						+"select ({target} as ?value)\t\n"
						+"\n"
						+"\t(sample(?l) as ?label)\n"
						+"\t(sample(?n) as ?notes)\n"
						+"\t(count(distinct {source}) as ?count)\n"
						+
						"\n"
						+"where {\n"
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
						+"}\n"
						+"\n"
						+"group by {target} \n"
						+"having ( count({source}) > 0 ) \n"
						+"order by desc(?count) ?value\n"
						+"{offset}\n"
						+"{limit}",

				target, source,

				roots(selector),
				filters(selector), // !!! use filter(selector, emptySet(), 0, 0) to support sampling

				path(source, path, target),

				offset > 0 ? snippet("offset {offset}", offset) : null,
				limit > 0 ? snippet("limit {limit}", limit) : null

		)))).evaluate(new AbstractTupleQueryResultHandler() {
			@Override public void handleSolution(final BindingSet bindings) throws TupleQueryResultHandlerException {

				final Value value=bindings.getValue("value");
				final Value count=bindings.getValue("count");
				final Value label=bindings.getValue("label");
				final Value notes=bindings.getValue("notes");

				// ;(virtuoso) counts are returned as xsd:int… cast to stay consistent

				final BNode term=bnode();

				if ( term != null ) { model.add(resource, GraphEngine.terms, term); }
				if ( term != null && value != null ) { model.add(term, GraphEngine.value, value); }
				if ( term != null && count != null ) {
					model.add(term, GraphEngine.count, literal(integer(count).orElse(BigInteger.ZERO)));
				}

				// !!! manage multiple languages

				if ( label != null ) { model.add((Resource)value, RDFS.LABEL, label); }
				if ( notes != null ) { model.add((Resource)value, RDFS.COMMENT, notes); }

			}
		}));

		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Snippet selector(final Shape shape, final Collection<Order> orders, final int offset, final int limit) {
		return shape.equals(and()) ? nothing() : shape.equals(or()) ? snippet("filter (false)") : snippet(

				"{ select distinct {root} {\n"
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

	private Snippet roots(final Shape shape) { // root universal constraints
		return all(shape)
				.map(this::values)
				.map(values -> values(shape, values))
				.orElse(null);
	}

	private Snippet filters(final Shape shape) {
		return shape.map(new FilterProbe(shape));
	}

	private Snippet pattern(final Shape shape) {
		return shape.map(new PatternProbe(shape));
	}

	private Snippet sorters(final Object root, final Collection<Order> orders) {
		return snippet(orders.stream()
				.filter(order -> !order.path().isEmpty()) // root already retrieved
				.map(order -> snippet(
						"optional { {root} {path} {order} }\n", var(root),
						path(order.path().stream().collect(toList())), var(order))
				)
		);
	}

	private Snippet criteria(final Object root, final Collection<Order> orders) {
		return list(Stream.concat(

				orders.stream().map(order -> snippet(
						order.inverse() ? "desc({criterion})" : "asc({criterion})",
						var(order.path().isEmpty() ? root : order)
				)),

				orders.stream()
						.map(Order::path)
						.filter(List::isEmpty)
						.findFirst()
						.map(empty -> Stream.empty())
						.orElseGet(() -> Stream.of(var(root)))  // root as last resort, unless already used

		), " ");
	}

	private Snippet values(final Shape source, final Collection<Value> values) {
		return snippet("\n\nvalues {source} {\n{values}\n}\n\n",

				var(source), list(values.stream().map(Values::format), "\n")

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class TemplateProbe extends Shape.Probe<Stream<Integer>> {

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

			final IRI iri=field.name();
			final Shape shape=field.shape();

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
			return and.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Integer> probe(final Or or) {
			return or.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Integer> probe(final When when) {
			return Stream.concat(
					when.pass().map(this),
					when.fail().map(this)
			);
		}

	}

	private final class FilterProbe extends Shape.Probe<Snippet> {

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

			final IRI iri=datatype.iri();

			return iri.equals(ValueType) ? nothing() : snippet(

					iri.equals(ResourceType) ? "filter ( isBlank({value}) || isIRI({value}) )"
							: iri.equals(BNodeType) ? "filter isBlank({value})"
							: iri.equals(IRIType) ? "filter isIRI({value})"
							: iri.equals(LiteralType) ? "filter isLiteral({value})"
							: "filter ( datatype({value}) = <{datatype}> )",

					var(source),
					iri

			);

		}

		@Override public Snippet probe(final Clazz clazz) {
			return snippet(var(source), " a/rdfs:subClassOf* ", Values.format(clazz.iri()), " .");
		}

		@Override public Snippet probe(final Range range) {
			throw new UnsupportedOperationException("focus range constraint");
		}

		@Override public Snippet probe(final Lang lang) {
			throw new UnsupportedOperationException("focus lang constraint");
		}


		@Override public Snippet probe(final MinExclusive minExclusive) {
			return snippet("filter ( {source} > {value} )", var(source), Values.format(value(minExclusive.limit())));
		}

		@Override public Snippet probe(final MaxExclusive maxExclusive) {
			return snippet("filter ( {source} < {value} )", var(source), Values.format(value(maxExclusive.limit())));
		}

		@Override public Snippet probe(final MinInclusive minInclusive) {
			return snippet("filter ( {source} >= {value} )", var(source), Values.format(value(minInclusive.limit())));
		}

		@Override public Snippet probe(final MaxInclusive maxInclusive) {
			return snippet("filter ( {source} <= {value} )", var(source), Values.format(value(maxInclusive.limit())));
		}

		@Override public Snippet probe(final MinLength minLength) {
			return snippet("filter (strlen(str({source})) >= {limit} )", var(source), minLength.limit());
		}

		@Override public Snippet probe(final MaxLength maxLength) {
			return snippet("filter (strlen(str({source})) <= {limit} )", var(source), maxLength.limit());
		}


		@Override public Snippet probe(final MinCount minCount) {
			throw new UnsupportedOperationException("minimum focus size constraint");
		}

		@Override public Snippet probe(final MaxCount maxCount) {
			throw new UnsupportedOperationException("maximum focus size constraint");
		}

		@Override public Snippet probe(final Pattern pattern) {
			return snippet("filter regex({source}, '{pattern}', '{flags}')",
					var(source), pattern.expression().replace("\\", "\\\\"), pattern.flags()
			);
		}

		@Override public Snippet probe(final Like like) {
			return snippet("filter regex({source}, '{pattern}')",
					var(source), like.toExpression().replace("\\", "\\\\")
			);
		}

		@Override public Snippet probe(final Stem stem) {
			return snippet("filter strstarts({source}, '{stem}')",
					var(source), stem.prefix()
			);
		}


		@Override public Snippet probe(final All all) {
			return nothing(); // universal constraints handled by field probe
		}

		@Override public Snippet probe(final Any any) { // singleton universal constraints handled by field probe

			// values-based filtering (as opposed to in-based filtering) works also or root terms // !!!
			// performance?

			return any.values().size() > 1 ? values(source, values(any.values())) : nothing();

		}

		@Override public Snippet probe(final Localized localized) {
			throw new UnsupportedOperationException("focus localized constraint");
		}


		@Override public Snippet probe(final Field field) {

			final IRI iri=field.name();
			final Shape shape=field.shape();

			final Optional<Set<Value>> all=all(shape).map(GraphFetcher.this::values);
			final Optional<Set<Value>> any=Any.any(shape).map(GraphFetcher.this::values);

			final Optional<Value> singleton=any
					.filter(values -> values.size() == 1)
					.map(values -> values.iterator().next());

			return snippet(

					(shape instanceof All || singleton.isPresent()) // filtering hook
							? null // ($) only if actually referenced by filters
							: edge(var(source), iri, var(shape)),

					all // target universal constraints
							.map(values -> values.stream().map(value -> edge(var(source), iri, Values.format(value))))
							.orElse(null),

					singleton // target singleton existential constraints
							.map(value -> edge(var(source), iri, Values.format(value)))
							.orElse(null),

					"\n\n",

					filters(shape)
			);
		}


		@Override public Snippet probe(final And and) {
			return snippet(and.shapes().stream().map(shape -> shape.map(this)));
		}

		@Override public Snippet probe(final Or or) {
			return list(
					or.shapes().stream().map(s -> snippet("{\f{branch}\f}", s.map(this))),
					" union "
			);
		}

		@Override public Snippet probe(final When when) {
			throw new UnsupportedOperationException("conditional pattern"); // !!! tbi
		}

	}

	private final class PatternProbe extends Shape.Probe<Snippet> {

		// !!! (€) remove optionals if term is required or if exists a filter on the same path

		private final Shape shape;


		private PatternProbe(final Shape shape) {
			this.shape=shape;
		}


		@Override public Snippet probe(final Guard guard) {
			throw new UnsupportedOperationException("partially redacted shape");
		}


		@Override public Snippet probe(final Field field) {

			final IRI iri=field.name();
			final Shape shape=field.shape();

			return snippet( // (€) optional unless universal constraints are present

					all(shape).isPresent() ? "\n\n{pattern}\n\n" : "\n\noptional {\n\n{pattern}\n\n}\n\n",

					snippet(
							edge(var(this.shape), iri, var(shape)), "\n",
							pattern(shape)
					)

			);
		}


		@Override public Snippet probe(final And and) {
			return snippet(and.shapes().stream().map(s -> s.map(this)));
		}

		@Override public Snippet probe(final Or or) {
			return snippet(or.shapes().stream().map(s -> s.map(this)));
		}

		@Override public Snippet probe(final When when) {
			throw new UnsupportedOperationException("conditional shape");
		}

	}

}
