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
import com.metreeca.rest.formats.ReaderFormat;
import com.metreeca.rest.formats.WriterFormat;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.io.*;
import java.util.Map;
import java.util.regex.Pattern;

import static com.metreeca.form.things.Transputs.text;
import static com.metreeca.form.things.Values.iri;

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
 * {@linkplain Request#parameters() parameter values}, {@linkplain Request#headers() headers} and {@link ReaderFormat}
 * {@linkplain Request#body(Format) body} payloads;</li>
 *
 * <li>response {@linkplain Request#headers() headers} and {@link WriterFormat} {@linkplain Request#body(Format) body}
 * payloads;</li>
 *
 * </ul>
 *
 * <p>This wrapper is intended to ensure data portability between development and production environment and may cause
 * severe performance degradation for large payloads: setting the canonical {@linkplain #base(String) base} to the
 * expected public server base effectively disables rewriting in production.</p>
 */
public final class Rewriter implements Wrapper { // !!! review interactions with multi-part messages

	private static Pattern pattern(final String source) {
		return Pattern.compile("\\b"+Pattern.quote(source)+"\\b");
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
					.map(r -> rewrite(pattern(external), internal, r))
					.map(handler::handle)
					.map(r -> rewrite(pattern(internal), external, r));
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Request rewrite(final Pattern source, final String target, final Request request) {
		return request

				.user(rewrite(source, target, request.user()))
				.roles(request.roles().stream().map(value -> rewrite(source, target, value)).collect(toSet()))

				.base(rewrite(source, target, request.base()))

				.query(rewrite(source, target, request.query())) // !!! interactions with decoding?

				.parameters(request.parameters().entrySet().stream().collect(toMap(Map.Entry::getKey, entry ->
						entry.getValue().stream().map(value -> rewrite(source, target, value)).collect(toList())
				)))

				.headers(request.headers().entrySet().stream().collect(toMap(Map.Entry::getKey, entry ->
						entry.getValue().stream().map(value -> rewrite(source, target, value)).collect(toList())
				)))

				.filter(ReaderFormat.asReader, supplier -> () -> rewrite(source, target, supplier.get()));
	}

	private Response rewrite(final Pattern source, final String target, final Response response) {
		return response

				.headers(response.headers().entrySet().stream().collect(toMap(Map.Entry::getKey, entry ->
						entry.getValue().stream().map(value -> rewrite(source, target, value)).collect(toList())
				)))

				.filter(WriterFormat.asWriter, consumer -> writer -> consumer.accept(rewrite(source, target, writer)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Value rewrite(final Pattern source, final String target, final Value value) {
		return value instanceof IRI ? iri(rewrite(source, target, value.stringValue())) : value;
	}

	private IRI rewrite(final Pattern source, final String target, final IRI iri) {
		return iri == null ? null : iri(rewrite(source, target, iri.toString()));
	}

	private String rewrite(final Pattern source, final String target, final CharSequence string) {
		return string == null ? null : source.matcher(string).replaceAll(target);
	}

	private Reader rewrite(final Pattern source, final String target, final Reader reader) { // !!! streaming replacement
		return reader == null ? null : new FilterReader(new StringReader(rewrite(source, target, text(reader)))) {

			@Override public void close() throws IOException { reader.close(); }

		};
	}

	private Writer rewrite(final Pattern source, final String target, final Writer writer) { // !!! streaming replacement
		return writer == null ? null : new FilterWriter(new StringWriter()) {

			@Override public void close() throws IOException {
				writer.write(rewrite(source, target, out.toString()));
				writer.close();
			}

		};
	}

}
