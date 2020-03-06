/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.sparql.wrappers;

import com.metreeca.sparql.Values;
import com.metreeca.sparql.formats.RDFFormat;
import com.metreeca.rest.*;
import com.metreeca.tree.Shape;
import com.metreeca.tree.shapes.*;

import org.eclipse.rdf4j.model.IRI;

import java.util.Collection;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.sparql.Values.direct;
import static com.metreeca.sparql.Values.inverse;
import static com.metreeca.sparql.Values.iri;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Guard.guard;
import static com.metreeca.tree.shapes.In.in;
import static com.metreeca.tree.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.tree.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.tree.shapes.Meta.meta;
import static com.metreeca.tree.shapes.MinExclusive.minExclusive;
import static com.metreeca.tree.shapes.MinInclusive.minInclusive;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


/**
 * IRI rewriter.
 *
 * <p>Rewrites IRIs in requests and responses to/from an internal canonical IRI {@linkplain #Rewriter(String)
 * base}.</p>
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
 * #Rewriter(String) base} to the expected public server base effectively disables rewriting in production.</p>
 */
public final class Rewriter implements Wrapper {

	private static final Pattern QueryWordPattern=Pattern.compile("[^=&]+");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String base;


	/**
	 * Creates a IRI rewriter.
	 *
	 * @param base the canonical internal IRI base mapped to/from this rewriter; empty for no rewriting
	 *
	 * @throws NullPointerException     if {@code base} is null
	 * @throws IllegalArgumentException if {@code base} is not an absolute IRI or ends with a {@linkplain
	 *                                  Character#isUnicodeIdentifierPart(char) Unicode identifier character}
	 */
	public Rewriter(final String base) {

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
					.map(_request -> rewrite(_request, new Engine(external, internal)))
					.map(handler::handle)
					.map(_response -> rewrite(request, _response, new Engine(internal, external)));
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request rewrite(final Request request, final Engine engine) {
		return new Request().lift(request) // preserve the external request it for linking to the external response

				.user(request.user().map(RDFFormat::iri).map(engine::rewrite).orElse(null))
				.roles(engine.rewrite(request.roles(), engine::rewrite))

				.method(request.method())
				.base(engine.rewrite(request.base()))
				.path(request.path())
				.query(rewrite(request.query(), engine))

				.parameters(engine.rewrite(request.parameters()))

				.headers(engine.rewrite(request.headers()))

				.shape(engine.rewrite(request.shape()))

				.header(RDFFormat.ExternalBase, request.base()); // make the external base available to rewriting in RDFBody

	}

	private String rewrite(final CharSequence query, final Engine engine) {

		final StringBuffer buffer=new StringBuffer(query.length());
		final Matcher matcher=QueryWordPattern.matcher(query);

		while ( matcher.find() ) {

			final String encoded=matcher.group();
			final String decoded=Codecs.decode(encoded);

			final String rewritten=engine.rewrite(decoded);

			matcher.appendReplacement(buffer,
					encoded.equals(decoded)? rewritten : Codecs.encode(rewritten) // re-encode only if actually encoded
			);
		}

		matcher.appendTail(buffer);

		return buffer.toString();
	}


	private Response rewrite(final Request request, final Response response, final Engine engine) {

		// rewrite the internal response to drive on-demand rewriting in RDFBody

		response

				.headers(engine.rewrite(response.headers()))
				.shape(engine.rewrite(response.shape()));

		// clone the internal response linking it to the original external request

		final Response external=new Response(request).lift(response)

				.status(response.status())
				.cause(response.cause().orElse(null))

				.shape(response.shape());

		// make external base/location available to rewriting in RDFBody (after cloning to keep service headers hidden)

		response

				.header(RDFFormat.ExternalBase, request.base())
				.header("Location", engine.rewrite(response.item()));

		return external;

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


		private Object rewrite(final Object value) {
			return value instanceof IRI ? rewrite((IRI)value) : value;
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
				return meta(rewrite(meta.getLabel()), rewrite(meta.getValue()));
			}

			@Override public Guard probe(final Guard guard) {
				return guard(rewrite(guard.getAxis()), rewrite(guard.getValues(), Engine.this::rewrite));
			}


			@Override public Datatype probe(final Datatype datatype) {
				return datatype(rewrite(datatype.getName()));
			}

			@Override public Clazz probe(final Clazz clazz) {
				return clazz(rewrite(clazz.getName()));
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

			@Override public Shape probe(final com.metreeca.tree.shapes.Pattern pattern) { return pattern; }

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
				return field(rewrite(field.getName()), rewrite(field.getShape()));
			}


			@Override public And probe(final And and) {
				return and(and.getShapes().stream().map(shape -> shape.map(this)).collect(toList()));
			}

			@Override public Or probe(final Or or) {
				return or(or.getShapes().stream().map(shape -> shape.map(this)).collect(toList()));
			}

			@Override public When probe(final When when) {
				return when(rewrite(when.getTest()), rewrite(when.getPass()), rewrite(when.getFail()));
			}

		}

	}

}
