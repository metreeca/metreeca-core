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
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.assets.Logger;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.math.BigInteger;
import java.util.*;
import java.util.function.*;
import java.util.stream.Stream;

import static com.metreeca.json.Focus.focus;
import static com.metreeca.json.Frame.inverse;
import static com.metreeca.json.Frame.traverse;
import static com.metreeca.json.Values.BNodeType;
import static com.metreeca.json.Values.IRIType;
import static com.metreeca.json.Values.LiteralType;
import static com.metreeca.json.Values.ResourceType;
import static com.metreeca.json.Values.ValueType;
import static com.metreeca.json.Values.bnode;
import static com.metreeca.json.Values.compare;
import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.integer;
import static com.metreeca.json.Values.literal;
import static com.metreeca.json.Values.md5;
import static com.metreeca.json.Values.statement;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.rdf4j.assets.Snippets.*;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.assets.Logger.time;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

final class GraphFetcher extends Query.Probe<Collection<Statement>> { // !!! refactor

	static Iterable<Statement> outline(final IRI resource, final Shape shape) {
		return filter(resource, shape).outline(resource).collect(toList());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Shape filter(final Value focus, final Shape shape) {
		return anchor(focus, shape.map(new TrimmerProbe(Mode, Filter)));
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

	private final Options options;


	private final Logger logger=asset(logger());


	GraphFetcher(final RepositoryConnection connection, final IRI resource, final Options options) {
		this.connection=connection;
		this.resource=resource;
		this.options=options;
	}


	private String compile(final Supplier<String> generator) {
		return time(generator).apply((t, v) -> logger

				.debug(this, () -> format("executing %s", v.endsWith("\n") ? v : v+"\n"))
				.debug(this, () -> format("generated in <%,d> ms", t))

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

		final Shape pattern=shape.redact(retain(Mode, Convey, Expose));
		final Shape selector=filter(resource, shape);

		evaluate(() -> connection.prepareTupleQuery(compile(() -> source(

				nothing(id(root, pattern, selector)), // link root to pattern and selector shapes

				snippet(

						"# items query\n"
								+"\n"
								+"prefix owl: <http://www.w3.org/2002/07/owl#>\n"
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

					template.forEach(statement -> {

						final Resource subject=statement.getSubject();
						final Value object=statement.getObject();

						final Value source=subject instanceof BNode
								? bindings.getValue(((BNode)subject).getID())
								: subject;

						final Value target=object instanceof BNode
								? bindings.getValue(((BNode)object).getID())
								: object;

						if ( source instanceof Resource && target != null ) {
							model.add(statement((Resource)source, statement.getPredicate(), target));
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

		final Map<Value, BigInteger> counts=new HashMap<>();

		final Collection<Value> mins=new ArrayList<>();
		final Collection<Value> maxs=new ArrayList<>();

		final Shape selector=filter(resource, shape);

		final Object source=var(selector);
		final Object target=path.isEmpty() ? source : var();

		evaluate(() -> connection.prepareTupleQuery(compile(() -> source(snippet(

				"# stats query\n"
						+"\n"
						+"prefix : <{base}>\n"
						+"prefix owl: <http://www.w3.org/2002/07/owl#>\n"
						+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
						+"\n"
						+"select \n"
						+"\n"
						+"\t?type ?type_label ?type_notes\n"
						+"\n"
						+"\t?min ?min_label ?min_notes\n"
						+"\t?max ?max_label ?max_notes\n"
						+"\n"
						+"\t?count\n"
						+"\n"
						+"where {\n"
						+"\n"
						+"\t{\n"
						+"\n"
						+"\t\tselect ?type\n"
						+"\n"
						+"\t\t\t(min({target}) as ?min)\n"
						+"\t\t\t(max({target}) as ?max) \n"
						+"\n"
						+"\t\t\t(count(distinct {target}) as ?count)\n"
						+"\n"
						+"\t\twhere {\n"
						+"\n"
						+"\t\t\t{roots}\n"
						+"\n"
						+"\t\t\t{filters}\n"
						+"\n"
						+"\t\t\t{path}\n"
						+"\n"
						+"\t\t\tbind (if(isBlank({target}), :bnode, if(isIRI({target}), :iri, datatype({target}))) as "
						+"?type)\n"
						+"\n"
						+"\t\t}\n"
						+"\n"
						+"\t\tgroup by ?type\n"
						+"\t\thaving ( count(distinct {target}) > 0 )\n"
						+"\t\torder by desc(?count) ?type\n"
						+"\t\t{offset}\n"
						+"\t\t{limit}\n"
						+"\n"
						+"\t}\n"
						+"\n"
						+"\toptional { ?type rdfs:label ?type_label }\n"
						+"\toptional { ?type rdfs:comment ?type_notes }\n"
						+"\n"
						+"\toptional { ?min rdfs:label ?min_label }\n"
						+"\toptional { ?min rdfs:comment ?min_notes }\n"
						+"\n"
						+"\toptional { ?max rdfs:label ?max_label }\n"
						+"\toptional { ?max rdfs:comment ?max_notes }\n"
						+"\n"
						+"}",

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

				final Value type_label=bindings.getValue("type_label");
				final Value type_notes=bindings.getValue("type_notes");

				final Value min=bindings.getValue("min");
				final Value max=bindings.getValue("max");

				final Value min_label=bindings.getValue("min_label");
				final Value min_notes=bindings.getValue("min_notes");

				final Value max_label=bindings.getValue("max_label");
				final Value max_notes=bindings.getValue("max_notes");

				// ;(virtuoso) counts are returned as xsd:int… cast to stay consistent

				final BigInteger count=integer(bindings.getValue("count")).orElse(BigInteger.ZERO);

				model.add(resource, GraphEngine.stats, type);
				model.add(type, GraphEngine.count, literal(count));

				if ( type_label != null ) { model.add(type, RDFS.LABEL, type_label); }
				if ( type_notes != null ) { model.add(type, RDFS.COMMENT, type_notes); }

				if ( min != null ) { model.add(type, GraphEngine.min, min); }
				if ( max != null ) { model.add(type, GraphEngine.max, max); }

				if ( min_label != null ) { model.add((Resource)min, RDFS.LABEL, min_label); }
				if ( min_notes != null ) { model.add((Resource)min, RDFS.COMMENT, min_notes); }

				if ( max_label != null ) { model.add((Resource)max, RDFS.LABEL, max_label); }
				if ( max_notes != null ) { model.add((Resource)max, RDFS.COMMENT, max_notes); }

				counts.putIfAbsent(type, count);

				if ( min != null ) { mins.add(min); }
				if ( max != null ) { maxs.add(max); }

			}

		}));

		model.add(resource, GraphEngine.count, literal(counts.values().stream()
				.reduce(BigInteger.ZERO, BigInteger::add)
		));

		mins.stream()
				.reduce((x, y) -> compare(x, y) < 0 ? x : y)
				.ifPresent(min -> model.add(resource, GraphEngine.min, min));

		maxs.stream()
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

		final Shape selector=filter(resource, shape);

		final Object source=var(selector);
		final Object target=path.isEmpty() ? source : var();

		evaluate(() -> connection.prepareTupleQuery(compile(() -> source(snippet(

				"# terms query\n"
						+"\n"
						+"prefix owl: <http://www.w3.org/2002/07/owl#>\n"
						+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
						+"\n"
						+"select ?value ?count ?label ?notes where {\n"
						+"\n"
						+"\t{\n"
						+"\n"
						+"\t\tselect ({target} as ?value) (count(distinct {source}) as ?count)\n"
						+"\n"
						+"\t\twhere {\n"
						+"\n"
						+"\t\t\t{roots}\n"
						+"\n"
						+"\t\t\t{filters}\n"
						+"\n"
						+"\t\t\t{path}\n"
						+"\n"
						+"\t\t}\n"
						+"\n"
						+"\t\tgroup by {target} \n"
						+"\t\thaving ( count(distinct {source}) > 0 ) \n"
						+"\t\torder by desc(?count) ?value\n"
						+"\t\t{offset}\n"
						+"\t\t{limit}\n"
						+"\n"
						+"\t}\n"
						+"\n"
						+"\toptional { ?value rdfs:label ?label }\n"
						+"\toptional { ?value rdfs:comment ?notes }\n"
						+"\n"
						+"}",

				target, source,

				roots(selector),
				filters(selector), // !!! use filter(selector, emptySet(), 0, 0) to support sampling

				path(source, path, target),

				offset > 0 ? snippet("offset {offset}", offset) : null,
				limit > 0 ? snippet("limit {limit}", limit) : null

		)))).evaluate(new AbstractTupleQueryResultHandler() {
			@Override public void handleSolution(final BindingSet bindings) throws TupleQueryResultHandlerException {

				// ;(virtuoso) counts are returned as xsd:int… cast to stay consistent

				final Value value=bindings.getValue("value");
				final Value count=literal(integer(bindings.getValue("count")).orElse(BigInteger.ZERO));
				final Value label=bindings.getValue("label");
				final Value notes=bindings.getValue("notes");

				final BNode term=bnode(md5(format(value)));

				model.add(resource, GraphEngine.terms, term);

				model.add(term, GraphEngine.value, value);
				model.add(term, GraphEngine.count, count);

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
		return shape.map(new SkeletonProbe(shape, true));
	}

	private Snippet pattern(final Shape shape) {
		return shape.map(new SkeletonProbe(shape, false));
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


	private Snippet path(final Object source, final Collection<IRI> path, final Object target) {
		return source == null || path == null || path.isEmpty() || target == null ? nothing()
				: snippet(source, " ", path(path), " ", target, " .\n");
	}

	private Snippet edge(final Object source, final Field field, final Object target) {
		return source == null || field == null || target == null ? nothing() : traverse(field.iri(),

				iri -> snippet(source, " ", same(true), format(iri), same(false), " ", target, " .\n"),
				iri -> snippet(target, " ", same(true), format(iri), same(false), " ", source, " .\n")

		);
	}


	private Snippet path(final Collection<IRI> path) {
		return snippet(same(true), list(path.stream().map(step -> snippet(

				snippet(format(step)),
				same(false))

		), '/'));
	}


	private Snippet same(final boolean head) {
		return options.same()
				? snippet(head ? nothing() : "/", "(owl:sameAs|^owl:sameAs)*", head ? "/" : nothing())
				: nothing();
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


		@Override public Stream<Integer> probe(final Field field) {

			final Shape shape=field.shape();

			final Integer source=identifier.apply(focus);
			final Integer target=identifier.apply(shape);

			final Resource snode=bnode(source.toString());
			final Resource tnode=bnode(target.toString());

			template.accept(traverse(field.iri(),
					iri -> statement(snode, iri, tnode),
					iri -> statement(tnode, iri, snode)
			));

			return Stream.concat(
					Stream.of(source, target),
					shape.map(new TemplateProbe(shape, identifier, template))
			);

		}


		@Override public Stream<Integer> probe(final When when) {
			return Stream.concat(
					when.pass().map(this),
					when.fail().map(this)
			);
		}

		@Override public Stream<Integer> probe(final And and) {
			return and.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Integer> probe(final Or or) {
			return or.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Integer> probe(final Shape shape) { return Stream.empty(); }

	}

	private final class SkeletonProbe extends Shape.Probe<Snippet> {

		private final Shape source;
		private final boolean prune;


		private SkeletonProbe(final Shape source, final boolean prune) {
			this.source=source;
			this.prune=prune;
		}


		@Override public Snippet probe(final Datatype datatype) {

			final IRI iri=datatype.iri();

			return iri.equals(ValueType) ? nothing() : snippet(

					iri.equals(ResourceType) ? "filter ( isBlank({value}) || isIRI({value}) )"
							: iri.equals(BNodeType) ? "filter isBlank({value})"
							: iri.equals(IRIType) ? "filter isIRI({value})"
							: iri.equals(LiteralType) ? "filter isLiteral({value})"
							: iri.equals(RDF.LANGSTRING) ? "filter (lang({value}) != '')"
							: "filter ( datatype({value}) = <{datatype}> )",

					var(source),
					iri

			);

		}

		@Override public Snippet probe(final Clazz clazz) {
			return snippet(var(source), " ",
					same(true), "a/(", same(true), "rdfs:subClassOf)*", same(false), " ",
					format(clazz.iri()), " ."
			);
		}

		@Override public Snippet probe(final Range range) {
			if ( prune ) {

				return probe((Shape)range); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return range.values().isEmpty() ? nothing() : snippet("filter ({source} in ({values}))",
						var(source), list(range.values().stream().map(Values::format), ", ")
				);

			}
		}

		@Override public Snippet probe(final Lang lang) {
			if ( prune ) {

				return probe((Shape)lang); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return lang.tags().isEmpty() ? nothing() : snippet("filter (lang({source}) in ({tags}))",
						var(source), list(lang.tags().stream().map(Values::quote), ", ")
				);

			}
		}


		@Override public Snippet probe(final MinExclusive minExclusive) {
			return snippet("filter ( {source} > {value} )", var(source), format(value(minExclusive.limit())));
		}

		@Override public Snippet probe(final MaxExclusive maxExclusive) {
			return snippet("filter ( {source} < {value} )", var(source), format(value(maxExclusive.limit())));
		}

		@Override public Snippet probe(final MinInclusive minInclusive) {
			return snippet("filter ( {source} >= {value} )", var(source), format(value(minInclusive.limit())));
		}

		@Override public Snippet probe(final MaxInclusive maxInclusive) {
			return snippet("filter ( {source} <= {value} )", var(source), format(value(maxInclusive.limit())));
		}


		@Override public Snippet probe(final MinLength minLength) {
			return snippet("filter (strlen(str({source})) >= {limit} )", var(source), minLength.limit());
		}

		@Override public Snippet probe(final MaxLength maxLength) {
			return snippet("filter (strlen(str({source})) <= {limit} )", var(source), maxLength.limit());
		}


		@Override public Snippet probe(final Pattern pattern) {
			return snippet("filter regex(str({source}), '{pattern}', '{flags}')",
					var(source), pattern.expression().replace("\\", "\\\\"), pattern.flags()
			);
		}

		@Override public Snippet probe(final Like like) {
			return snippet("filter regex(str({source}), '{pattern}')",
					var(source), like.toExpression().replace("\\", "\\\\")
			);
		}

		@Override public Snippet probe(final Stem stem) {
			return snippet("filter strstarts(str({source}), '{stem}')",
					var(source), stem.prefix()
			);
		}


		public Snippet probe(final MinCount minCount) { return prune ? probe((Shape)minCount) : nothing(); }

		public Snippet probe(final MaxCount maxCount) { return prune ? probe((Shape)maxCount) : nothing(); }


		@Override public Snippet probe(final All all) {
			return nothing(); // universal constraints handled by field probe
		}

		@Override public Snippet probe(final Any any) {

			// values-based filtering (as opposed to in-based filtering) works also or root terms
			// / !!! performance?

			return any.values().size() <= 1
					? nothing() // singleton universal constraints handled by field probe
					: values(source, values(any.values()));

		}


		public Snippet probe(final Localized localized) { return prune ? probe((Shape)localized) : nothing(); }


		@Override public Snippet probe(final Field field) {


			final Shape shape=field.shape();

			final Optional<Set<Value>> all=all(shape).map(GraphFetcher.this::values);
			final Optional<Set<Value>> any=any(shape).map(GraphFetcher.this::values);

			final Optional<Value> singleton=any
					.filter(values -> values.size() == 1)
					.map(values -> values.iterator().next());

			return snippet(

					// (€) optional unless universally constrained // !!! or filtered

					prune || (all.isPresent() || singleton.isPresent())
							? "\n\n{pattern}\n\n"
							: "\n\noptional {\n\n{pattern}\n\n}\n\n",

					snippet(

							prune && (all.isPresent() || singleton.isPresent())
									? nothing() // (€) filtering hook already available on all/any edges
									: edge(var(source), field, var(shape)), // filtering or projection hook

							all.map(values -> // insert universal constraints edges
									values.stream().map(value -> edge(var(source), field, format(value)))
							).orElse(Stream.empty()),

							singleton.map(value -> // insert singleton existential constraint edge
									edge(var(source), field, format(value))
							).orElse(nothing()),

							"\n\n",

							shape.map(new SkeletonProbe(shape, prune))
					)
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


		@Override public Snippet probe(final Shape shape) {
			throw new UnsupportedOperationException(shape.toString());
		}

	}

}
