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

	static final String root="0";


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

	Scribe roots(final Shape shape) { // root universal constraints
		return all(shape)
				.map(values -> values(root, values))
				.orElse(nothing());
	}

	Scribe filters(final Shape shape) {
		return shape.map(new SkeletonProbe(root, true, options.same()));
	}

	Scribe pattern(final Shape shape) {
		return shape.map(new SkeletonProbe(root, false, options.same()));
	}

	Scribe anchor(final List<IRI> path, final String target) {
		return path.isEmpty() ? nothing()
				: list(var(root), text(" "), path(path), text(" "), var(target), text(" .\n"));
	}


	Scribe path(final List<IRI> path) {
		return options.same()
				? list(same(), list(path.stream().map(step -> list(text(step), same())), "/"))
				: list(path.stream().map(Scribe::text), "/");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static Scribe values(final String anchor, final Collection<Value> values) {
		return text("\fvalues %s {\n%s\n}\f",
				var(anchor), list(values.stream().map(Values::format).map(Scribe::text), "\n")
		);
	}

	static Scribe same() {
		return text("(owl:sameAs|^owl:sameAs)*");
	}

	static Scribe var(final String id) {
		return text(" ?%s", id);
	}

	static Scribe offset(final int offset) {
		return offset > 0 ? text("offset %d", offset) : nothing();
	}

	static Scribe limit(final int limit, final int sampling) {
		return limit == 0 && sampling == 0 ? nothing()
				: text("limit %d", limit > 0 ? min(limit, sampling) : sampling);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class SkeletonProbe extends Shape.Probe<Scribe> {

		private final String anchor;

		private final boolean prune;
		private final boolean same;


		private SkeletonProbe(final String anchor, final boolean prune, final boolean same) {

			this.anchor=anchor;

			this.prune=prune;
			this.same=same;
		}


		private Scribe edge(final String source, final IRI predicate, final String target) {
			return traverse(predicate,

					iri -> same
							? text("%s %s/%s/%s %s .\n", var(source), same(), text(iri), same(), var(target))
							: text("%s %s %s .\n", var(source), text(iri), var(target)),

					iri -> same
							? text("%s %s/%s/%s %s .\n", var(target), same(), text(iri), same(), var(source))
							: text("%s %s %s .\n", var(target), text(iri), var(source))

			);
		}

		private Scribe edge(final String source, final IRI predicate, final Value target) {
			return traverse(predicate,

					iri -> same
							? text("%s %s/%s/%s %s .\n", var(source), same(), text(iri), same(), text(target))
							: text("%s %s %s .\n", var(source), text(iri), text(target)),

					iri -> same
							? text("%s %s/%s/%s %s .\n", text(target), same(), text(iri), same(), var(source))
							: text("%s %s %s .\n", text(target), text(iri), var(source))

			);
		}


		@Override public Scribe probe(final Datatype datatype) {

			final IRI iri=datatype.iri();

			return iri.equals(ValueType) ? nothing()

					: iri.equals(ResourceType) ? text("filter ( isBlank(%1$s) || isIRI(%1$s) )\n", var(anchor))
					: iri.equals(BNodeType) ? text("filter isBlank(%s)\n", var(anchor))
					: iri.equals(IRIType) ? text("filter isIRI(%s)\n", var(anchor))
					: iri.equals(LiteralType) ? text("filter isLiteral(%s)\n", var(anchor))
					: iri.equals(RDF.LANGSTRING) ? text("filter (lang(%s) != '')\n", var(anchor))

					: text("filter ( datatype(%s) = %s )\n", var(anchor), text(iri));

		}

		@Override public Scribe probe(final Clazz clazz) {
			return same
					? text("%s (owl:sameAs|^owl:sameAs)*/a/((owl:sameAs|^owl:sameAs)*/rdfs:subClassOf)* %s.\n",
					var(anchor), text(clazz.iri()))
					: text("%s a/rdfs:subClassOf* %s.\n", var(anchor), text(clazz.iri()));
		}

		@Override public Scribe probe(final Range range) {
			if ( prune ) {

				return probe((Shape)range); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return range.values().isEmpty() ? nothing() : text("filter (%s in (%s))\n",
						var(anchor), list(range.values().stream().map(Values::format).map(Scribe::text), ", ")
				);

			}
		}

		@Override public Scribe probe(final Lang lang) {
			if ( prune ) {

				return probe((Shape)lang); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return lang.tags().isEmpty() ? nothing() : text("filter (lang(%s) in (%s))\n",
						var(anchor), list(lang.tags().stream().map(Values::quote).map(Scribe::text), ", ")
				);

			}
		}


		@Override public Scribe probe(final MinExclusive minExclusive) {
			return text("filter ( %s > %s )\n", var(anchor), text(minExclusive.limit()));
		}

		@Override public Scribe probe(final MaxExclusive maxExclusive) {
			return text("filter ( %s < %s )\n", var(anchor), text(maxExclusive.limit()));
		}

		@Override public Scribe probe(final MinInclusive minInclusive) {
			return text("filter ( %s >= %s )\n", var(anchor), text(minInclusive.limit()));
		}

		@Override public Scribe probe(final MaxInclusive maxInclusive) {
			return text("filter ( %s <= %s )\n", var(anchor), text(maxInclusive.limit()));
		}


		@Override public Scribe probe(final MinLength minLength) {
			return text("filter (strlen(str(%s)) >= %s )\n", var(anchor), text(minLength.limit()));
		}

		@Override public Scribe probe(final MaxLength maxLength) {
			return text("filter (strlen(str(%s)) <= %s )\n", var(anchor), text(maxLength.limit()));
		}


		@Override public Scribe probe(final Pattern pattern) {
			return text("filter regex(str(%s), %s, %s)\n",
					var(anchor), text(quote(pattern.expression())), text(quote(pattern.flags()))
			);
		}

		@Override public Scribe probe(final Like like) {
			return text("filter regex(str(%s), %s)\n",
					var(anchor), text(quote(like.toExpression()))
			);
		}

		@Override public Scribe probe(final Stem stem) {
			return text("filter strstarts(str(%s), %s)\n",
					var(anchor), text(quote(stem.prefix()))
			);
		}


		@Override public Scribe probe(final MinCount minCount) {
			return prune ? probe((Shape)minCount) : nothing();
		}

		@Override public Scribe probe(final MaxCount maxCount) {
			return prune ? probe((Shape)maxCount) : nothing();
		}


		@Override public Scribe probe(final All all) {
			return nothing(); // universal constraints handled by field probe
		}

		@Override public Scribe probe(final Any any) {

			// values-based filtering (as opposed to in-based filtering) works also or root terms // !!! performance?

			return any.values().size() <= 1
					? nothing() // singleton universal constraints handled by field probe
					: values(anchor, any.values());

		}


		@Override public Scribe probe(final Localized localized) {
			return prune ? probe((Shape)localized) :
					nothing();
		}


		@Override public Scribe probe(final Field field) {

			final Shape shape=field.shape();
			final String alias=field.alias();

			final Optional<Set<Value>> all=all(shape);
			final Optional<Set<Value>> any=any(shape);

			final Optional<Value> singleton=any
					.filter(values -> values.size() == 1)
					.map(values -> values.iterator().next());

			return text( // (€) optional unless universally constrained // !!! or filtered

					prune || all.isPresent() || singleton.isPresent() ? "\f%s\f" : "\foptional {\f%s\f}\f",

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


		@Override public Scribe probe(final And and) {
			return list(and.shapes().stream().map(shape -> shape.map(this)));
		}

		@Override public Scribe probe(final Or or) {
			return list(or.shapes().stream().map(s -> text("{\f%s\f}", s.map(this))), " union ");
		}


		@Override public Scribe probe(final Shape shape) {
			throw new UnsupportedOperationException(shape.toString());
		}

	}

}
