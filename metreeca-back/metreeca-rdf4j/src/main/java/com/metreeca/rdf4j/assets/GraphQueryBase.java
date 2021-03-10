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
import com.metreeca.json.shapes.*;
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.Scribe;
import com.metreeca.rest.assets.Logger;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;
import java.util.stream.Stream;

import static com.metreeca.json.Frame.traverse;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Scribe.text;
import static com.metreeca.rest.Scribe.*;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.assets.Logger.time;

import static java.lang.Math.min;
import static java.lang.String.format;

abstract class GraphQueryBase {

	static final String Root="0";


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
		return shape.map(new SkeletonProbe(Root, true, options.same()));
	}

	UnaryOperator<Appendable> pattern(final Shape shape) {
		return shape.map(new SkeletonProbe(Root, false, options.same()));
	}


	UnaryOperator<Appendable> path(final List<IRI> path, final String target) {
		return path.isEmpty() ? nothing()
				: list(var(Root), text(" "), path(path), text(" "), var(target), text(" .\n"));
	}

	UnaryOperator<Appendable> path(final List<IRI> path) {
		return options.same()
				? list(same(), list(path.stream().map(step -> list(text(step), same())), "/"))
				: list(path.stream().map(Scribe::text), "/");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static UnaryOperator<Appendable> values(final String anchor, final Collection<Value> values) {
		return text("\fvalues {anchor} {\n{values}\n}\f",

				var(anchor), list(values.stream().map(Values::format).map(Scribe::text), "\n")

		);
	}

	static UnaryOperator<Appendable> same() {
		return text("(owl:sameAs|^owl:sameAs)*");
	}

	static UnaryOperator<Appendable> var(final String id) {
		return text(" ?%s", id);
	}

	static UnaryOperator<Appendable> offset(final int offset) {
		return offset > 0 ? text("offset %d", offset) : nothing();
	}

	static UnaryOperator<Appendable> limit(final int limit, final int sampling) {
		return limit == 0 && sampling == 0 ? nothing()
				: text("limit %d", limit > 0 ? min(limit, sampling) : sampling);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class SkeletonProbe extends Shape.Probe<UnaryOperator<Appendable>> {

		private final String anchor;

		private final boolean prune;
		private final boolean same;


		private SkeletonProbe(final String anchor, final boolean prune, final boolean same) {

			this.anchor=anchor;

			this.prune=prune;
			this.same=same;
		}


		private UnaryOperator<Appendable> edge(final String source, final IRI predicate, final String target) {

			final UnaryOperator<Appendable> vs=var(source);
			final UnaryOperator<Appendable> vt=var(target);
			final UnaryOperator<Appendable> sp=same();

			return traverse(predicate,

					iri -> same
							? text("{source} {same}/{predicate}/{same} {target} .\n", vs, sp, text(iri), vt)
							: text("{source} {predicate} {target} .\n", vs, text(iri), vt),

					iri -> same
							? text("{target} {same}/{predicate}/{same} {source} .\n", vt, sp, text(iri), vs)
							: text("{target} {predicate} {source} .\n", vt, text(iri), vs)

			);
		}

		private UnaryOperator<Appendable> edge(final String source, final IRI predicate, final Value target) {

			final UnaryOperator<Appendable> vs=var(source);
			final UnaryOperator<Appendable> vt=text(target);
			final UnaryOperator<Appendable> sp=same();

			return traverse(predicate,

					iri -> same
							? text("{source} {same}/{predicate}/{same} {target} .\n", vs, sp, text(iri), vt)
							: text("{source} {predicate} {target} .\n", vs, text(iri), vt),

					iri -> same
							? text("{target} {same}/{predicate}/{same} {source} .\n", vt, sp, text(iri), vs)
							: text("{target} {predicate} {source} .\n", vt, text(iri), vs)

			);
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

					same
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


		@Override public UnaryOperator<Appendable> probe(final MinCount minCount) {
			return prune ? probe((Shape)minCount) :
					nothing();
		}

		@Override public UnaryOperator<Appendable> probe(final MaxCount maxCount) {
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


		@Override public UnaryOperator<Appendable> probe(final Localized localized) {
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
									: edge(anchor, field.iri(), alias), // filtering or projection hook

							list(all.map(values -> // insert universal constraints edges
									values.stream().map(value -> edge(anchor, field.iri(), value))
							).orElse(Stream.empty())),

							singleton.map(value -> // insert singleton existential constraint edge
									edge(anchor, field.iri(), value)
							).orElse(nothing()),

							text("\f"),

							shape.map(new SkeletonProbe(alias, prune, same))
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
