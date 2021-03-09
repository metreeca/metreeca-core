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

import com.metreeca.json.Shape;
import com.metreeca.json.Values;
import com.metreeca.json.queries.Stats;
import com.metreeca.json.queries.Terms;
import com.metreeca.json.shapes.*;
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.Scribe;
import com.metreeca.rest.assets.Logger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.metreeca.json.Frame.traverse;
import static com.metreeca.json.Shape.*;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Field.fields;
import static com.metreeca.json.shapes.Guard.Convey;
import static com.metreeca.json.shapes.Guard.Mode;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Scribe.text;
import static com.metreeca.rest.Scribe.*;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.assets.Logger.time;

import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;

abstract class GraphQueryBase {

	static final String Root="0";

	static final String Base="app:/terms#";

	static final IRI terms=iri(Base, "terms");
	static final IRI stats=iri(Base, "stats");

	static final IRI value=iri(Base, "value");
	static final IRI count=iri(Base, "count");

	static final IRI min=iri(Base, "min");
	static final IRI max=iri(Base, "max");


	private static final Set<IRI> Annotations=unmodifiableSet(new HashSet<>(asList(RDFS.LABEL, RDFS.COMMENT)));


	static Shape StatsShape(final Stats query) {

		final Shape term=annotations(query.shape(), query.path());

		return and(

				field(count, required(), datatype(XSD.INTEGER)),
				field(min, optional(), term),
				field(max, optional(), term),

				field(stats, multiple(),
						field(count, required(), datatype(XSD.INTEGER)),
						field(min, required(), term),
						field(max, required(), term)
				)

		);
	}

	static Shape TermsShape(final Terms query) {

		final Shape term=annotations(query.shape(), query.path());

		return and(
				field(terms, multiple(),
						field(value, required(), term),
						field(count, required(), datatype(XSD.INTEGER))
				)
		);
	}


	private static Shape annotations(final Shape shape, final Iterable<IRI> path) {

		Shape nested=shape.redact(Mode, Convey);

		for (final IRI step : path) {
			nested=field(nested, step)

					.orElseThrow(() -> new IllegalArgumentException(format("unknown path step <%s>", step)))

					.shape();
		}

		return and(fields(nested).filter(field -> Annotations.contains(field.iri())));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Options options;

	private final Logger logger=asset(logger());


	GraphQueryBase(final Options options) {
		this.options=options;
	}


	protected Options options() {
		return options;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	String compile(final Supplier<String> generator) {
		return time(generator).apply((t, v) -> logger

				.debug(this, () -> format("executing %s", v.endsWith("\n") ? v : v+"\n"))
				.debug(this, () -> format("generated in <%,d> ms", t))

		);
	}

	void evaluate(final Runnable task) {
		time(task).apply(t -> logger

				.debug(this, () -> format("evaluated in <%,d> ms", t))

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	UnaryOperator<Appendable> roots(final Shape shape) { // root universal constraints
		return all(shape)
				.map(values -> values(Root, values))
				.orElse(nothing());
	}

	UnaryOperator<Appendable> filters(final Shape shape) {
		return shape.map(new SkeletonProbe(Root, true));
	}

	UnaryOperator<Appendable> path(final List<IRI> path, final String target) {
		return path.isEmpty() ? nothing()
				: list(var(Root), text(" "), path(path), text(" "), var(target), text(" .\n"));
	}


	UnaryOperator<Appendable> offset(final int offset) {
		return offset > 0 ? text("offset %d", offset) : nothing();
	}

	UnaryOperator<Appendable> limit(final int limit, final int sampling) {
		return limit == 0 && sampling == 0 ? nothing()
				: text("limit %d", limit > 0 ? min(limit, sampling) : sampling);
	}


	private static UnaryOperator<Appendable> values(final String anchor, final Collection<Value> values) {
		return text("\fvalues {anchor} {\n{values}\n}\f",

				var(anchor), list(values.stream().map(Values::format).map(Scribe::text), "\n")

		);
	}


	private UnaryOperator<Appendable> edge(final String source, final Field field, final String target) {
		return source == null || field == null || target == null ? nothing() : traverse(field.iri(),

				iri -> list(var(source), text(" "), same(true), text(iri), same(false), text(" "), var(target), text(" "
						+".\n")),
				iri -> list(var(target), text(" "), same(true), text(iri), same(false), text(" "), var(source), text(" "
						+".\n"))

		);
	}

	private UnaryOperator<Appendable> edge(final String source, final Field field, final Value target) {
		return source == null || field == null || target == null ? nothing() : traverse(field.iri(),

				iri -> list(var(source), text(" "), same(true), text(iri), same(false), text(" "), text(target), text(
						" .\n")),
				iri -> list(text(target), text(" "), same(true), text(iri), same(false), text(" "), var(source), text(
						" .\n"))

		);
	}


	UnaryOperator<Appendable> path(final List<IRI> path) {
		return list(same(true), list(path.stream().map(step ->

				list(text(step), same(false))

		), "/"));
	}

	static UnaryOperator<Appendable> var(final String id) {
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

	UnaryOperator<Appendable> pattern(final Shape shape) {
		return shape.map(new SkeletonProbe(Root, false));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
			return text("filter ( {source} > {value} )\n", var(anchor), text(minExclusive.limit()));
		}

		@Override public UnaryOperator<Appendable> probe(final MaxExclusive maxExclusive) {
			return text("filter ( {source} < {value} )\n", var(anchor), text(maxExclusive.limit()));
		}

		@Override public UnaryOperator<Appendable> probe(final MinInclusive minInclusive) {
			return text("filter ( {source} >= {value} )\n", var(anchor), text(minInclusive.limit()));
		}

		@Override public UnaryOperator<Appendable> probe(final MaxInclusive maxInclusive) {
			return text("filter ( {source} <= {value} )\n", var(anchor), text(maxInclusive.limit()));
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


		public UnaryOperator<Appendable> probe(final MinCount minCount) {
			return prune ? probe((Shape)minCount) :
					nothing();
		}

		public UnaryOperator<Appendable> probe(final MaxCount maxCount) {
			return prune ? probe((Shape)maxCount) :
					nothing();
		}


		@Override public UnaryOperator<Appendable> probe(final All all) {
			return nothing(); // universal constraints handled by field probe
		}

		@Override public UnaryOperator<Appendable> probe(final Any any) {

			// values-based filtering (as opposed to in-based filtering) works also or root terms // !!! performance?

			return any.values().size() <= 1
					? nothing() // singleton universal constraints handled by field probe
					: values(anchor, any.values());

		}


		public UnaryOperator<Appendable> probe(final Localized localized) {
			return prune ? probe((Shape)localized) :
					nothing();
		}


		@Override public UnaryOperator<Appendable> probe(final Field field) {

			final Shape shape=field.shape();
			final String alias=field.alias();

			final Optional<Set<Value>> all=all(shape);
			final Optional<Set<Value>> any=any(shape);

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
