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
import com.metreeca.rest.Scribe;
import com.metreeca.rest.assets.Logger;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

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
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Scribe.*;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.assets.Logger.time;

import static org.eclipse.rdf4j.model.util.Values.triple;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

final class GraphFetcher extends Query.Probe<Collection<Statement>> {

	private static final int ItemsSampling=1_000; // the maximum number of resources to be returned from items queries
	private static final int StatsSampling=10_000; // the maximum number of resources to be evaluated by stats queries
	private static final int TermsSampling=10_000; // the maximum number of resources to be evaluated by terms queries

	private static final String Root="0";


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
		time(task).apply(t -> logger

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

		final int reserved=1+orders.size();

		final Shape filter=shape.filter(resource).tag(reserved);
		final Shape convey=shape.convey().tag(reserved);

		final Collection<Triple> template=convey.map(new TemplateProbe(Root)).collect(toList());
		final Collection<Statement> model=new LinkedHashSet<>();

		// construct results are serialized with no ordering guarantee >> transfer data as tuples to preserve order

		evaluate(() -> connection.prepareTupleQuery(compile(() -> Scribe.code(text(

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

				list(Stream.concat(

						Stream.of(Root), /// always project root

						Stream.concat( // project template variables

								template.stream().map(Triple::getSubject),
								template.stream().map(Triple::getObject)

						).distinct().map(BNode.class::cast).map(BNode::getID)

				).distinct().sorted().map(GraphFetcher::var)),

				filter(Root, filter, orders, offset, limit),
				convey(Root, convey),

				sorters(Root, orders), // !!! (€) don't extract if already present in pattern
				criteria(Root, orders)

		)))).evaluate(new AbstractTupleQueryResultHandler() {

			@Override public void handleSolution(final BindingSet bindings) {

				final Value match=bindings.getValue(Root);

				if ( match != null ) {

					if ( !match.equals(resource) ) {
						model.add(statement(resource, Shape.Contains, match));
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

	@Override public Collection<Statement> probe(final Terms terms) {

		final Shape shape=terms.shape();
		final List<IRI> path=terms.path();
		final int offset=terms.offset();
		final int limit=terms.limit();

		final String source=Root;
		final String target=path.isEmpty() ? source : "hook";

		final Shape filter=shape.filter(resource).tag(1);

		final Collection<Statement> model=new LinkedHashSet<>();

		evaluate(() -> connection.prepareTupleQuery(compile(() -> Scribe.code(text(

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

				var(target),
				var(source),

				roots(Root, filter),
				filters(Root, filter), // !!! use filter(selector, emptySet(), 0, 0) to support sampling

				path(source, path, target),

				offset(offset),
				limit(limit, TermsSampling)

		)))).evaluate(new AbstractTupleQueryResultHandler() {
			@Override public void handleSolution(final BindingSet bindings) throws TupleQueryResultHandlerException {

				// ;(virtuoso) counts are returned as xsd:int… cast to stay consistent

				final Value value=bindings.getValue("value");
				final Value count=literal(integer(bindings.getValue("count")).orElse(BigInteger.ZERO));
				final Value label=bindings.getValue("label");
				final Value notes=bindings.getValue("notes");

				final BNode term=bnode(md5(format(value)));

				model.add(statement(resource, GraphEngine.terms, term));

				model.add(statement(term, GraphEngine.value, value));
				model.add(statement(term, GraphEngine.count, count));

				if ( label != null ) { model.add(statement((Resource)value, RDFS.LABEL, label)); }
				if ( notes != null ) { model.add(statement((Resource)value, RDFS.COMMENT, notes)); }

			}
		}));

		return model;
	}

	@Override public Collection<Statement> probe(final Stats stats) {

		final Shape shape=stats.shape();
		final List<IRI> path=stats.path();
		final int offset=stats.offset();
		final int limit=stats.limit();

		final String source=Root;
		final String target=path.isEmpty() ? source : "hook";

		final Shape filter=shape.filter(resource).tag(1);

		final Collection<Statement> model=new LinkedHashSet<>();

		final Map<Value, BigInteger> counts=new HashMap<>();

		final Collection<Value> mins=new ArrayList<>();
		final Collection<Value> maxs=new ArrayList<>();

		evaluate(() -> connection.prepareTupleQuery(compile(() -> Scribe.code(text(

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

				text(GraphEngine.Base),
				var(target),

				roots(Root, filter),
				filters(Root, filter), // !!! use filter(selector, emptySet(), 0, 0) to support sampling

				path(source, path, target),

				offset(offset),
				limit(limit, StatsSampling)

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

				model.add(statement(resource, GraphEngine.stats, type));
				model.add(statement(type, GraphEngine.count, literal(count)));

				if ( type_label != null ) { model.add(statement(type, RDFS.LABEL, type_label)); }
				if ( type_notes != null ) { model.add(statement(type, RDFS.COMMENT, type_notes)); }

				if ( min != null ) { model.add(statement(type, GraphEngine.min, min)); }
				if ( max != null ) { model.add(statement(type, GraphEngine.max, max)); }

				if ( min_label != null ) { model.add(statement((Resource)min, RDFS.LABEL, min_label)); }
				if ( min_notes != null ) { model.add(statement((Resource)min, RDFS.COMMENT, min_notes)); }

				if ( max_label != null ) { model.add(statement((Resource)max, RDFS.LABEL, max_label)); }
				if ( max_notes != null ) { model.add(statement((Resource)max, RDFS.COMMENT, max_notes)); }

				counts.putIfAbsent(type, count);

				if ( min != null ) { mins.add(min); }
				if ( max != null ) { maxs.add(max); }

			}

		}));

		model.add(statement(resource, GraphEngine.count, literal(counts.values().stream()
				.reduce(BigInteger.ZERO, BigInteger::add)
		)));

		mins.stream()
				.reduce((x, y) -> compare(x, y) < 0 ? x : y)
				.ifPresent(min -> model.add(statement(resource, GraphEngine.min, min)));

		maxs.stream()
				.reduce((x, y) -> compare(x, y) > 0 ? x : y)
				.ifPresent(max -> model.add(statement(resource, GraphEngine.max, max)));

		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private /*static*/ UnaryOperator<Appendable> filter(final String anchor, final Shape shape,
			final List<Order> orders, final int offset
			, final int limit) {
		return shape.equals(and()) ? nothing() : shape.equals(or()) ? text("filter (false)") : text(

				"{ select distinct {root} {\n"
						+"\n"
						+"\t{roots}\n"
						+"\n"
						+"\t{filters}\n"
						+"\n"
						+"\t{sorters}\n"
						+"\n"
						+"} {orders} {offset} {limit} }",

				var(anchor),

				roots(Root, shape),
				filters(Root, shape),

				offset > 0 || limit > 0 ? sorters(Root, orders) : nothing(),
				offset > 0 || limit > 0 ? text(" order by ", criteria(Root, orders)) : nothing(),

				offset > 0 ? text(" offset %d", offset) : nothing(),
				text(" limit %d", limit > 0 ? min(limit, ItemsSampling) : ItemsSampling)

		);

	}

	private /*static*/ UnaryOperator<Appendable> roots(final String anchor, final Shape shape) { // root universal
		// constraints
		return all(shape)
				.map(this::values)
				.map(values -> values(anchor, values))
				.orElse(nothing());
	}

	private /*static*/ UnaryOperator<Appendable> filters(final String anchor, final Shape shape) {
		return shape.map(new SkeletonProbe(anchor, true));
	}

	private /*static*/ UnaryOperator<Appendable> convey(final String anchor, final Shape shape) {
		return shape.map(new SkeletonProbe(anchor, false));
	}

	private /*static*/ UnaryOperator<Appendable> sorters(final String anchor, final List<Order> orders) {
		return list(orders.stream()
				.filter(order -> !order.path().isEmpty()) // root already retrieved
				.map(order -> text("optional { {root} {path} {order} }\n",
						var(anchor), path(order.path()), var(valueOf(1+orders.indexOf(order)))
				))
		);
	}

	private static UnaryOperator<Appendable> criteria(final String anchor, final List<Order> orders) {
		return list(Stream.concat(

				orders.stream().map(order -> text(
						order.inverse() ? "desc({criterion})" : "asc({criterion})",
						order.path().isEmpty() ? var(anchor) : var(valueOf(1+orders.indexOf(order)))
				)),

				orders.stream()
						.map(Order::path)
						.filter(List::isEmpty)
						.findFirst()
						.map(empty -> Stream.<UnaryOperator<Appendable>>empty())
						.orElseGet(() -> Stream.of(var(anchor)))  // root as last resort, unless already used

		), " ");
	}

	private static UnaryOperator<Appendable> offset(final int offset) {
		return offset > 0 ? text("offset %d", offset) : nothing();
	}

	private static UnaryOperator<Appendable> limit(final int limit, final int sampling) {
		return text("limit %d", limit > 0 ? min(limit, sampling) : sampling);
	}


	private static UnaryOperator<Appendable> values(final String anchor, final Collection<Value> values) {
		return text("\fvalues {anchor} {\n{values}\n}\f",

				var(anchor), list(values.stream().map(Values::format).map(Scribe::text), "\n")

		);
	}


	private /*static*/ UnaryOperator<Appendable> path(final String source, final List<IRI> path, final String target) {
		return source == null || path == null || path.isEmpty() || target == null ? nothing()
				: list(var(source), text(" "), path(path), text(" "), var(target), text(" .\n"));
	}

	private /*static*/ UnaryOperator<Appendable> edge(final String source, final Field field, final String target) {
		return source == null || field == null || target == null ? nothing() : traverse(field.iri(),

				iri -> list(var(source), text(" "), same(true), text(iri), same(false), text(" "), var(target), text(" "
						+".\n")),
				iri -> list(var(target), text(" "), same(true), text(iri), same(false), text(" "), var(source), text(" "
						+".\n"))

		);
	}

	private /*static*/ UnaryOperator<Appendable> edge(final String source, final Field field, final Value target) {
		return source == null || field == null || target == null ? nothing() : traverse(field.iri(),

				iri -> list(var(source), text(" "), same(true), text(iri), same(false), text(" "), text(target), text(
						" .\n")),
				iri -> list(text(target), text(" "), same(true), text(iri), same(false), text(" "), var(source), text(
						" .\n"))

		);
	}


	private UnaryOperator<Appendable> path(final List<IRI> path) {
		return list(same(true), list(path.stream().map(step ->

				list(text(step), same(false))

		), "/"));
	}

	private static UnaryOperator<Appendable> var(final String id) {
		return text(" ?%s", id);
	}


	private UnaryOperator<Appendable> same() {
		return options.same() ? text("(owl:sameAs|^owl:sameAs)*") : nothing();
	}

	private UnaryOperator<Appendable> same(final boolean head) {
		return options.same()
				? list(head ? nothing() : text("/"), same(), head ? text("/") : nothing())
				: nothing();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TemplateProbe extends Shape.Probe<Stream<Triple>> {

		private final String anchor;


		private TemplateProbe(final String anchor) {
			this.anchor=anchor;
		}


		@Override public Stream<Triple> probe(final Field field) {

			final Shape shape=field.shape();
			final String alias=field.alias();

			final BNode source=bnode(anchor);
			final BNode target=bnode(alias);

			final Triple triple=traverse(field.iri(),
					iri -> triple(source, iri, target),
					iri -> triple(target, iri, source)
			);

			return Stream.concat(Stream.of(triple), shape.map(new TemplateProbe(alias)));
		}


		@Override public Stream<Triple> probe(final When when) {
			return Stream.concat(
					when.pass().map(this),
					when.fail().map(this)
			);
		}

		@Override public Stream<Triple> probe(final And and) {
			return and.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Triple> probe(final Or or) {
			return or.shapes().stream().flatMap(shape -> shape.map(this));
		}


		@Override public Stream<Triple> probe(final Shape shape) { return Stream.empty(); }

	}

	private final class SkeletonProbe extends Shape.Probe<UnaryOperator<Appendable>> {

		private final String anchor;
		private final boolean prune;


		private SkeletonProbe(final String anchor, final boolean prune) {
			this.anchor=anchor;
			this.prune=prune;
		}


		@Override public UnaryOperator<Appendable> probe(final Datatype datatype) {

			final IRI iri=datatype.iri();

			return iri.equals(ValueType) ? nothing() : text(

					iri.equals(ResourceType) ? "filter ( isBlank({value}) || isIRI({value}) )\n"
							: iri.equals(BNodeType) ? "filter isBlank({value})\n"
							: iri.equals(IRIType) ? "filter isIRI({value})\n"
							: iri.equals(LiteralType) ? "filter isLiteral({value})\n"
							: iri.equals(RDF.LANGSTRING) ? "filter (lang({value}) != '')\n"
							: "filter ( datatype({value}) = {datatype} )\n",

					var(anchor),
					text(iri)

			);

		}

		@Override public UnaryOperator<Appendable> probe(final Clazz clazz) {
			return text("{source} {path} {class}.\n",

					var(anchor),

					options.same()
							? text(" {same}/a/({same}/rdfs:subClassOf)* ", same())
							: text(" a/rdfs:subClassOf* "),

					text(clazz.iri())

			);
		}

		@Override public UnaryOperator<Appendable> probe(final Range range) {
			if ( prune ) {

				return probe((Shape)range); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return range.values().isEmpty() ? nothing() : text("filter ({source} in ({values}))\n",
						var(anchor), list(range.values().stream().map(Values::format).map(Scribe::text), ", ")
				);

			}
		}

		@Override public UnaryOperator<Appendable> probe(final Lang lang) {
			if ( prune ) {

				return probe((Shape)lang); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return lang.tags().isEmpty() ? nothing() : text("filter (lang({source}) in ({tags}))\n",
						var(anchor), list(lang.tags().stream().map(Values::quote).map(Scribe::text), ", ")
				);

			}
		}


		@Override public UnaryOperator<Appendable> probe(final MinExclusive minExclusive) {
			return text("filter ( {source} > {value} )\n", var(anchor), text(value(minExclusive.limit())));
		}

		@Override public UnaryOperator<Appendable> probe(final MaxExclusive maxExclusive) {
			return text("filter ( {source} < {value} )\n", var(anchor), text(value(maxExclusive.limit())));
		}

		@Override public UnaryOperator<Appendable> probe(final MinInclusive minInclusive) {
			return text("filter ( {source} >= {value} )\n", var(anchor), text(value(minInclusive.limit())));
		}

		@Override public UnaryOperator<Appendable> probe(final MaxInclusive maxInclusive) {
			return text("filter ( {source} <= {value} )\n", var(anchor), text(value(maxInclusive.limit())));
		}


		@Override public UnaryOperator<Appendable> probe(final MinLength minLength) {
			return text("filter (strlen(str({source})) >= {limit} )\n", var(anchor), text(minLength.limit()));
		}

		@Override public UnaryOperator<Appendable> probe(final MaxLength maxLength) {
			return text("filter (strlen(str({source})) <= {limit} )\n", var(anchor), text(maxLength.limit()));
		}


		@Override public UnaryOperator<Appendable> probe(final Pattern pattern) {
			return text("filter regex(str({source}), '{pattern}', '{flags}')\n",
					var(anchor), text(pattern.expression().replace("\\", "\\\\")), text(pattern.flags())
			);
		}

		@Override public UnaryOperator<Appendable> probe(final Like like) {
			return text("filter regex(str({source}), '{pattern}')\n",
					var(anchor), text(like.toExpression().replace("\\", "\\\\"))
			);
		}

		@Override public UnaryOperator<Appendable> probe(final Stem stem) {
			return text("filter strstarts(str({source}), '{stem}')\n",
					var(anchor), text(stem.prefix())
			);
		}


		public UnaryOperator<Appendable> probe(final MinCount minCount) { return prune ? probe((Shape)minCount) :
				nothing(); }

		public UnaryOperator<Appendable> probe(final MaxCount maxCount) { return prune ? probe((Shape)maxCount) :
				nothing(); }


		@Override public UnaryOperator<Appendable> probe(final All all) {
			return nothing(); // universal constraints handled by field probe
		}

		@Override public UnaryOperator<Appendable> probe(final Any any) {

			// values-based filtering (as opposed to in-based filtering) works also or root terms // !!! performance?

			return any.values().size() <= 1
					? nothing() // singleton universal constraints handled by field probe
					: values(anchor, values(any.values()));

		}


		public UnaryOperator<Appendable> probe(final Localized localized) { return prune ? probe((Shape)localized) :
				nothing(); }


		@Override public UnaryOperator<Appendable> probe(final Field field) {

			final Shape shape=field.shape();
			final String alias=field.alias();

			final Optional<Set<Value>> all=all(shape).map(GraphFetcher.this::values);
			final Optional<Set<Value>> any=any(shape).map(GraphFetcher.this::values);

			final Optional<Value> singleton=any
					.filter(values -> values.size() == 1)
					.map(values -> values.iterator().next());

			return text(

					// (€) optional unless universally constrained // !!! or filtered

					prune || all.isPresent() || singleton.isPresent()
							? "\f{pattern}\f"
							: "\foptional {\f{pattern}\f}\f",

					list(

							prune && (all.isPresent() || singleton.isPresent())
									? nothing() // (€) filtering hook already available on all/any edges
									: edge(anchor, field, alias), // filtering or projection hook

							list(all.map(values -> // insert universal constraints edges
									values.stream().map(value -> edge(anchor, field, value))
							).orElse(Stream.empty())),

							singleton.map(value -> // insert singleton existential constraint edge
									edge(anchor, field, value)
							).orElse(nothing()),

							text("\f"),

							shape.map(new SkeletonProbe(alias, prune))
					)
			);

		}


		@Override public UnaryOperator<Appendable> probe(final And and) {
			return list(and.shapes().stream().map(shape -> shape.map(this)));
		}

		@Override public UnaryOperator<Appendable> probe(final Or or) {
			return list(
					or.shapes().stream().map(s -> text("{\f{branch}\f}", s.map(this))),
					" union "
			);
		}


		@Override public UnaryOperator<Appendable> probe(final Shape shape) {
			throw new UnsupportedOperationException(shape.toString());
		}

	}

}
