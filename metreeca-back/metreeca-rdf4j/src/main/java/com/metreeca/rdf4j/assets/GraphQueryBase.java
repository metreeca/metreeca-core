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

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.rdf4j.SPARQLScribe.lang;
import static com.metreeca.rdf4j.SPARQLScribe.string;
import static com.metreeca.rdf4j.SPARQLScribe.*;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Scribe.text;
import static com.metreeca.rest.Scribe.*;
import static com.metreeca.rest.assets.Logger.logger;
import static com.metreeca.rest.assets.Logger.time;

import static java.lang.String.format;
import static java.lang.String.valueOf;

abstract class GraphQueryBase {

	static final String root="0";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Options options;

	private int label=1; // the next label available for tagging (0 reserved for the root node)

	private final Logger logger=asset(logger());


	GraphQueryBase(final Options options) {
		this.options=options;
	}


	Options options() {
		return options;
	}


	String label() {
		return valueOf(label++);
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

				space(all(shape).map(values -> values(root, values)).orElse(nothing())),  // root universal constraints

				space(shape.map(new SkeletonProbe(root, true)))

		);
	}


	Scribe pattern(final Shape shape) {
		return space(shape.map(new SkeletonProbe(root, false)));
	}

	Scribe anchor(final Collection<IRI> path, final String target) {
		return path.isEmpty() ? nothing() : space(edge(var(root), path(path), var(target)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class SkeletonProbe extends Shape.Probe<Scribe> {

		private final String anchor;
		private final boolean prune;


		private SkeletonProbe(final String anchor, final boolean prune) {
			this.anchor=anchor;
			this.prune=prune;
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
			return edge(var(anchor), text("a/rdfs:subClassOf*"), text(clazz.iri()));
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

				return lang.tags().isEmpty() ? nothing() : filter(
						in(lang(var(anchor)), lang.tags().stream().map(Values::quote).map(Scribe::text))
				);

			}
		}


		@Override public Scribe probe(final MinExclusive minExclusive) {
			return filter(gt(var(anchor), text(minExclusive.limit())));
		}

		@Override public Scribe probe(final MaxExclusive maxExclusive) {
			return filter(lt(var(anchor), text(maxExclusive.limit())));
		}

		@Override public Scribe probe(final MinInclusive minInclusive) {
			return filter(gte(var(anchor), text(minInclusive.limit())));
		}

		@Override public Scribe probe(final MaxInclusive maxInclusive) {
			return filter(lte(var(anchor), text(maxInclusive.limit())));
		}


		@Override public Scribe probe(final MinLength minLength) {
			return filter(gte(strlen(str(var(anchor))), text(minLength.limit())));
		}

		@Override public Scribe probe(final MaxLength maxLength) {
			return filter(lte(strlen(str(var(anchor))), text(maxLength.limit())));
		}


		@Override public Scribe probe(final Pattern pattern) {
			return filter(regex(
					str(var(anchor)), text(quote(pattern.expression())), text(quote(pattern.flags()))
			));
		}

		@Override public Scribe probe(final Like like) {
			return filter(regex(
					str(var(anchor)), text(quote(like.toExpression()))
			));
		}

		@Override public Scribe probe(final Stem stem) {
			return filter(strstarts(
					str(var(anchor)), text(quote(stem.prefix()))
			));
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
			return anchor.equals(root)
					? values(anchor, any.values())
					: filter(in(var(anchor), any.values().stream().map(Scribe::text)));
		}


		@Override public Scribe probe(final Localized localized) {
			return prune ? probe((Shape)localized) : nothing();
		}


		@Override public Scribe probe(final Field field) {

			final Shape shape=field.shape();
			final String alias=field.alias();

			final Optional<Set<Value>> all=all(shape);

			final Scribe constraints=list(

					prune && all.isPresent() ? nothing() // (€) filtering hook already available on universal edges
							: line(edge(var(anchor), field.iri(), var(alias))), // filtering or projection hook

					list(all.map(values -> values.stream().map(value ->  // universal constraints edges
							line(edge(var(anchor), field.iri(), text(value)))
					)).orElse(Stream.empty())),

					space(shape.map(new SkeletonProbe(alias, prune)))
			);

			return space(prune ? constraints : optional(space(constraints)));

		}


		@Override public Scribe probe(final And and) {
			return list(and.shapes().stream().map(shape -> shape.map(this)));
		}

		@Override public Scribe probe(final Or or) {
			return union(or.shapes().stream().map(s -> block(space(s.map(this)))).toArray(Scribe[]::new));
		}


		@Override public Scribe probe(final Shape shape) {
			throw new UnsupportedOperationException(shape.toString());
		}

	}

}
