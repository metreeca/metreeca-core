/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

import com.metreeca.form.things.Values;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import static com.metreeca.form.things.Transputs.encode;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.formats.RDFFormat.asRDF;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;


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
 * headers} and {@link RDFFormat} {@linkplain Request#body(Format) body} payloads;</li>
 *
 * <li>response {@linkplain Request#headers() headers} and {@link RDFFormat} {@linkplain Request#body(Format) body}
 * payloads;</li>
 *
 * </ul>
 *
 * <p><strong>Warning</strong> / This wrapper is intended to ensure data portability between development and production
 * environment and may cause severe performance degradation for large payloads: setting the canonical {@linkplain
 * #base(String) base} to the expected public server base effectively disables rewriting in production.</p>
 */
public final class Rewriter implements Wrapper {

	private static Pattern pattern(final String source) {
		return Pattern.compile("(^|\\b)"+Pattern.quote(source)+"(\\b|$)");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
	 * @throws IllegalArgumentException if {@code base} is not an absolute IRI or ends with a legal {@linkplain
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
					.map(r -> rewrite(external, internal, r))
					.map(handler::handle)
					.map(r -> rewrite(internal, external, r));
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request rewrite(final String source, final String target, final Request request) {

		final Pattern decoded=pattern(source);
		final Pattern encoded=pattern(encode(source));

		return request

				.user(rewrite(decoded, target, request.user()))
				.roles(request.roles().stream().map(value -> rewrite(decoded, target, value)).collect(toSet()))

				.base(rewrite(decoded, target, request.base()))

				.query(rewrite(encoded, encode(target), // encoded variant
						rewrite(decoded, target, request.query()) // decoded variant
				))

				.parameters(request.parameters().entrySet().stream().collect(toMap(Map.Entry::getKey, entry ->
						entry.getValue().stream().map(value -> rewrite(decoded, target, value)).collect(toList())
				)))

				.headers(request.headers().entrySet().stream().collect(toMap(Map.Entry::getKey, entry ->
						entry.getValue().stream().map(value -> rewrite(decoded, target, value)).collect(toList())
				)))

				.filter(asRDF, model -> rewrite(decoded, target, model));
	}

	private Response rewrite(final String source, final String target, final Response response) {

		final Pattern decoded=pattern(source);

		return response

				.headers(response.headers().entrySet().stream().collect(toMap(Map.Entry::getKey, entry ->
						entry.getValue().stream().map(value -> rewrite(decoded, target, value)).collect(toList())
				)))

				.filter(asRDF, model -> rewrite(decoded, target, model));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private List<Statement> rewrite(final Pattern source, final String target, final Collection<Statement> model) {
		return model.stream().map(s -> rewrite(source, target, s)).collect(toList());
	}

	private Statement rewrite(final Pattern source, final String target, final Statement statement) {
		return statement == null ? null : statement(
				rewrite(source, target, statement.getSubject()),
				rewrite(source, target, statement.getPredicate()),
				rewrite(source, target, statement.getObject()),
				rewrite(source, target, statement.getContext())
		);
	}

	private Value rewrite(final Pattern source, final String target, final Value value) {
		return value instanceof IRI ? rewrite(source, target, (IRI)value) : value;
	}

	private Resource rewrite(final Pattern source, final String target, final Resource resource) {
		return resource instanceof IRI ? rewrite(source, target, (IRI)resource) : resource;
	}

	private IRI rewrite(final Pattern source, final String target, final IRI iri) {
		return iri == null ? null : iri(rewrite(source, target, iri.toString()));
	}

	private String rewrite(final Pattern source, final String target, final CharSequence string) {
		return string == null ? null : source.matcher(string).replaceAll(target);
	}

}
