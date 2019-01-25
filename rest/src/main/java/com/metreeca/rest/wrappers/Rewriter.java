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

package com.metreeca.rest.wrappers;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Values;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.In.in;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.MinExclusive.minExclusive;
import static com.metreeca.form.shapes.MinInclusive.minInclusive;
import static com.metreeca.form.shapes.Option.option;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Codecs.decode;
import static com.metreeca.form.things.Codecs.encode;
import static com.metreeca.form.things.Values.*;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.RDFFormat.rdf;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


/**
 * IRI rewriting wrapper.
 *
 * <p>Rewrites IRIs in requests and responses to/from an internal canonical IRI {@linkplain #base(String) base}.</p>
 *
 * <p>The following message components are inspected for rewritable IRIs:</p>
 *
 * <ul>
 *
 * <li>request {@linkplain Request#user() user}, {@linkplain Request#roles() roles}, {@linkplain Request#base() base},
 * {@linkplain Request#query() query}, {@linkplain Request#parameters() parameters}, {@linkplain Request#headers()
 * headers}, {@linkplain Message#shape() shape} and {@link RDFFormat} {@linkplain Request#body(Format) body};</li>
 *
 * <li>response {@linkplain Request#item() focus item}, {@linkplain Request#headers() headers}, {@linkplain
 * Message#shape() shape} and {@link RDFFormat} {@linkplain Request#body(Format) body};</li>
 *
 * </ul>
 *
 * <p><strong>Warning</strong> / This wrapper is intended to ensure data portability between development and production
 * environment and may cause severe performance degradation for large payloads: setting the canonical {@linkplain
 * #base(String) base} to the expected public server base effectively disables rewriting in production.</p>
 */
public final class Rewriter implements Wrapper {

	private String base;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the canonical IRI base.
	 *
	 * @param base the canonical internal IRI base mapped to/from this rewriter; empty for no rewriting
	 *
	 * @return this rewriter
	 *
	 * @throws NullPointerException     if {@code base} is null
	 * @throws IllegalArgumentException if {@code base} is not an absolute IRI or ends with a {@linkplain
	 *                                  Character#isUnicodeIdentifierPart(char) Unicode identifier character}
	 */
	public Rewriter base(final String base) {

		if ( base == null ) {
			throw new NullPointerException("null base");
		}

		if ( !Values.AbsoluteIRIPattern.matcher(base).matches() ) {
			throw new IllegalArgumentException("not an absolute base IRI");
		}

		if ( Character.isUnicodeIdentifierPart(base.charAt(base.length()-1)) ) {
			throw new IllegalArgumentException("base ending with Unicode identifier character");
		}

		this.base=base;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return request -> {

			final String external=request.base();
			final String internal=base;

			final boolean identity=external.isEmpty() || internal.isEmpty() || external.equals(internal);

			return identity ? handler.handle(request) : request
					.map(r -> rewrite(r, new Engine(external, internal)))
					.map(handler::handle)
					.map(r -> rewrite(r, new Engine(internal, external)));
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request rewrite(final Request request, final Engine engine) {
		return request

				.user(engine.rewrite(request.user()))
				.roles(engine.rewrite(request.roles(), engine::rewrite))

				.base(engine.rewrite(request.base()))

				.map(r -> { // re-encode rewritten query only if it was actually encoded

					final String encoded=r.query();
					final String decoded=decode(encoded);

					return r.query(encoded.equals(decoded) ? engine.rewrite(encoded) : encode(engine.rewrite(decoded)));

				})

				.parameters(engine.rewrite(request.parameters()))
				.headers(engine.rewrite(request.headers()))

				.shape(engine.rewrite(request.shape()))

				.pipe(rdf(), model -> Value(engine.rewrite(model, engine::rewrite)));
	}

	private Response rewrite(final Response response, final Engine engine) {
		return response

				// ;( force response focus rewriting even if location is not already set

				.header("Location", response.item().stringValue())

				.headers(engine.rewrite(response.headers()))

				.shape(engine.rewrite(response.shape()))

				.pipe(rdf(), model -> Value(engine.rewrite(model, engine::rewrite)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Engine {

		private final Pattern source;
		private final String target;

		private final ShapeEngine shapes;


		private Engine(final String source, final String target) {

			this.source=Pattern.compile("\\b"+Pattern.quote(source));
			this.target=target;

			this.shapes=new ShapeEngine();
		}


		private Shape rewrite(final Shape shape) {
			return shape.map(shapes);
		}


		private <T> Collection<T> rewrite(final Collection<T> collection, final Function<T, T> rewriter) {
			return collection.stream().map(rewriter).collect(toList());
		}

		private Map<String, Collection<String>> rewrite(final Map<String, Collection<String>> map) {
			return map.entrySet().stream().collect(toMap(
					Map.Entry::getKey, entry -> rewrite(entry.getValue(), this::rewrite)
			));
		}


		private Statement rewrite(final Statement statement) {
			return statement == null ? null : statement(
					rewrite(statement.getSubject()),
					rewrite(statement.getPredicate()),
					rewrite(statement.getObject()),
					rewrite(statement.getContext())
			);
		}

		private Value rewrite(final Value value) {
			return value instanceof IRI ? rewrite((IRI)value) : value;
		}

		private Resource rewrite(final Resource resource) {
			return resource instanceof IRI ? rewrite((IRI)resource) : resource;
		}

		private IRI rewrite(final IRI iri) {
			return iri == null ? null
					: direct(iri) ? iri(rewrite(iri.stringValue()))
					: inverse(iri(rewrite(iri.stringValue())));
		}

		private String rewrite(final CharSequence string) {
			return string == null ? null : source.matcher(string).replaceAll(target);
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private final class ShapeEngine implements Shape.Probe<Shape> {

			@Override public Meta probe(final Meta meta) {
				return meta(rewrite(meta.getIRI()), rewrite(meta.getValue()));
			}

			@Override public When probe(final When when) {
				return when(rewrite(when.getIRI()), rewrite(when.getValues(), Engine.this::rewrite));
			}


			@Override public Datatype probe(final Datatype datatype) {
				return datatype(rewrite(datatype.getIRI()));
			}

			@Override public Clazz probe(final Clazz clazz) {
				return clazz(rewrite(clazz.getIRI()));
			}

			@Override public MinExclusive probe(final MinExclusive minExclusive) {
				return minExclusive(rewrite(minExclusive.getValue()));
			}

			@Override public MaxExclusive probe(final MaxExclusive maxExclusive) {
				return maxExclusive(rewrite(maxExclusive.getValue()));
			}

			@Override public MinInclusive probe(final MinInclusive minInclusive) {
				return minInclusive(rewrite(minInclusive.getValue()));
			}

			@Override public MaxInclusive probe(final MaxInclusive maxInclusive) {
				return maxInclusive(rewrite(maxInclusive.getValue()));
			}

			@Override public Shape probe(final MinLength minLength) { return minLength; }

			@Override public Shape probe(final MaxLength maxLength) { return maxLength; }

			@Override public Shape probe(final com.metreeca.form.shapes.Pattern pattern) { return pattern; }

			@Override public Shape probe(final Like like) { return like; }


			@Override public Shape probe(final MinCount minCount) { return minCount; }

			@Override public Shape probe(final MaxCount maxCount) { return maxCount; }

			@Override public In probe(final In in) {
				return in(rewrite(in.getValues(), Engine.this::rewrite));
			}

			@Override public All probe(final All all) {
				return all(rewrite(all.getValues(), Engine.this::rewrite));
			}

			@Override public Any probe(final Any any) {
				return any(rewrite(any.getValues(), Engine.this::rewrite));
			}


			@Override public Field probe(final Field field) {
				return field(rewrite(field.getIRI()), rewrite(field.getShape()));
			}


			@Override public And probe(final And and) {
				return and(and.getShapes().stream().map(shape -> shape.map(this)).collect(toList()));
			}

			@Override public Or probe(final Or or) {
				return or(or.getShapes().stream().map(shape -> shape.map(this)).collect(toList()));
			}

			@Override public Option probe(final Option option) {
				return option(rewrite(option.getTest()), rewrite(option.getPass()), rewrite(option.getFail()));
			}

		}

	}

}
