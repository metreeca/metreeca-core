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
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.json.Frame.traverse;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.rdf4j.SPARQLScribe.lang;
import static com.metreeca.rdf4j.SPARQLScribe.string;
import static com.metreeca.rdf4j.SPARQLScribe.*;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Scribe.text;
import static com.metreeca.rest.Scribe.*;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.assets.Logger.time;

import static java.lang.String.format;
import static java.util.Collections.singleton;

abstract class GraphQueryBase {

	static final String root="0";

	static Scribe same() {
		return text("(owl:sameAs|^owl:sameAs)*");
	}

	static Scribe same(final Collection<IRI> path) {
		return list(Stream.concat(
				Stream.of(same()),
				path.stream().flatMap(step -> Stream.of(text(step), same()))
		), "/");
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

	Scribe filters(final Shape shape) {
		return space(
				all(shape).map(values -> values(root, values)).orElse(nothing()), // root universal constraints
				shape.map(new SkeletonProbe(root, true, options.same()))
		);
	}

	Scribe pattern(final Shape shape) {
		return space(shape.map(new SkeletonProbe(root, false, options.same())));
	}

	Scribe anchor(final Collection<IRI> path, final String target) {
		return path.isEmpty() ? nothing() : space(edge(
				var(root),
				options.same() ? same(path) : path(path),
				var(target)
		));
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


		@Override public Scribe probe(final Datatype datatype) {

			final IRI iri=datatype.iri();

			return iri.equals(ValueType) ? nothing() : line(filter(

					iri.equals(ResourceType) ? or(isBlank(var(anchor)), isIRI(var(anchor)))
							: iri.equals(BNodeType) ? isBlank(var(anchor))
							: iri.equals(IRIType) ? isIRI(var(anchor))
							: iri.equals(LiteralType) ? isLiteral(var(anchor))
							: iri.equals(RDF.LANGSTRING) ? neq(lang(var(anchor)), string(""))

							: eq(datatype(var(anchor)), text(iri))

			));
		}

		@Override public Scribe probe(final Clazz clazz) {
			return line(edge(
					var(anchor),
					same ? text("%1$s/a/(%1$s/rdfs:subClassOf)*/%1$s", same()) : text("a/rdfs:subClassOf*"),
					text(clazz.iri())
			));
		}

		@Override public Scribe probe(final Range range) {
			if ( prune ) {

				return probe((Shape)range); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return range.values().isEmpty() ? nothing() : line(filter(
						in(var(anchor), range.values().stream().map(Scribe::text))
				));

			}
		}

		@Override public Scribe probe(final Lang lang) {
			if ( prune ) {

				return probe((Shape)lang); // !!! tbi (filter not exists w/ special root treatment)

			} else {

				return lang.tags().isEmpty() ? nothing() : line(filter(
						in(lang(var(anchor)), lang.tags().stream().map(Values::quote).map(Scribe::text))
				));

			}
		}


		@Override public Scribe probe(final MinExclusive minExclusive) {
			return line(filter(gt(var(anchor), text(minExclusive.limit()))));
		}

		@Override public Scribe probe(final MaxExclusive maxExclusive) {
			return line(filter(lt(var(anchor), text(maxExclusive.limit()))));
		}

		@Override public Scribe probe(final MinInclusive minInclusive) {
			return line(filter(gte(var(anchor), text(minInclusive.limit()))));
		}

		@Override public Scribe probe(final MaxInclusive maxInclusive) {
			return line(filter(lte(var(anchor), text(maxInclusive.limit()))));
		}


		@Override public Scribe probe(final MinLength minLength) {
			return line(filter(gte(strlen(str(var(anchor))), text(minLength.limit()))));
		}

		@Override public Scribe probe(final MaxLength maxLength) {
			return line(filter(lte(strlen(str(var(anchor))), text(maxLength.limit()))));
		}


		@Override public Scribe probe(final Pattern pattern) {
			return line(filter(regex(
					str(var(anchor)), text(quote(pattern.expression())), text(quote(pattern.flags()))
			)));
		}

		@Override public Scribe probe(final Like like) {
			return line(filter(regex(
					str(var(anchor)), text(quote(like.toExpression()))
			)));
		}

		@Override public Scribe probe(final Stem stem) {
			return line(filter(strstarts(
					str(var(anchor)), text(quote(stem.prefix()))
			)));
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
			return prune ? probe((Shape)localized) : nothing();
		}


		@Override public Scribe probe(final Field field) {

			final Shape shape=field.shape();
			final String alias=field.alias();

			final Optional<Set<Value>> all=all(shape);
			final Optional<Set<Value>> any=any(shape);

			final Optional<Value> singleton=any
					.filter(values -> values.size() == 1)
					.map(values -> values.iterator().next());

			final Function<Scribe, Scribe> edge=target -> (traverse(field.iri(),

					iri -> line(edge(var(anchor), same ? same(singleton(iri)) : text(iri), target)),
					iri -> line(edge(target, same ? same(singleton(iri)) : text(iri), var(anchor)))

			));

			final Scribe constraints=list(

					prune && (all.isPresent() || singleton.isPresent())
							? nothing() // (€) filtering hook already available on all/any edges
							: edge.apply(var(alias)), // filtering or projection hook

					list(all.map(values -> // insert universal constraints edges
							values.stream().map(value -> edge.apply(text(value)))
					).orElse(Stream.empty())),

					singleton.map(value -> // insert singleton existential constraint edge
							edge.apply(text(value))
					).orElse(nothing()),

					space(shape.map(new SkeletonProbe(alias, prune, same)))
			);

			return space( // (€) optional unless universally constrained // !!! or filtered
					prune || all.isPresent() || singleton.isPresent() ? constraints : optional(space(constraints))
			);

		}


		@Override public Scribe probe(final And and) {
			return list(and.shapes().stream().map(shape -> shape.map(this)));
		}

		@Override public Scribe probe(final Or or) {
			return union(or.shapes().stream().map(s -> block(space(s.map(this)))));
		}


		@Override public Scribe probe(final Shape shape) {
			throw new UnsupportedOperationException(shape.toString());
		}

	}

}
