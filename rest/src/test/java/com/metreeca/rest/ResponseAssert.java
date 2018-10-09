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

package com.metreeca.rest;

import com.metreeca.form.things.Codecs;
import com.metreeca.rest.formats.TextFormat;

import java.io.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;


public final class ResponseAssert extends MessageAssert<ResponseAssert, Response> {

	public static ResponseAssert assertThat(final Response response) {

		if ( response != null ) {

			final Cache cache=new Cache(response);

			if ( !response.body(input()).get().isPresent() ) {
				response.body(input()).set(cache::input); // cache binary body
			}

			if ( !response.body(reader()).get().isPresent() ) {
				response.body(reader()).set(cache::reader); // cache textual body
			}

			final StringBuilder builder=new StringBuilder(2500);

			builder.append(response.status()).append('\n');

			response.headers().forEach((name, values) -> values.forEach(value ->
					builder.append(name).append(": ").append(value).append('\n')
			));

			builder.append('\n');

			response.body(TextFormat.text()).use(text -> {
				if ( !text.isEmpty() ) {

					final int limit=builder.capacity();

					builder.append(text.length() <= limit ? text : text.substring(0, limit)+"\n⋮").append("\n\n");
				}
			});

			Logger.getLogger(response.getClass().getName()).log(
					response.success() ? Level.INFO : Level.WARNING,
					builder.toString(),
					response.cause().orElse(null)
			);

		}

		return new ResponseAssert(response);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ResponseAssert(final Response actual) {
		super(actual, ResponseAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ResponseAssert isSuccess() {

		isNotNull();

		if ( !actual.success() ) {
			failWithMessage("expected response to be success but was <%d>", actual.status());
		}

		return this;
	}

	public ResponseAssert hasStatus(final int expected) {

		isNotNull();

		if ( actual.status() != expected ) {
			failWithMessage("expected response status to be <%d> was <%d>", expected, actual.status());
		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Cache {

		private final Response response;

		private byte[] data;
		private String text;


		private Cache(final Response response) {
			this.response=response;
		}


		private ByteArrayInputStream input() {

			final byte[] data=data();
			final String text=text();

			return new ByteArrayInputStream(data.length == 0 ? text.getBytes(Codecs.UTF8) : data);
		}

		private StringReader reader() {

			final String text=text();
			final byte[] data=data();

			return new StringReader(text.isEmpty() ? new String(data, Codecs.UTF8) : text);
		}


		private String text() {

			if ( text == null ) {
				try (final StringWriter buffer=new StringWriter()) {

					response.body(writer()).use(consumer -> consumer.accept(() -> buffer));

					text=buffer.toString();

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			}

			return text;
		}

		private byte[] data() {

			if ( data == null ) {
				try (final ByteArrayOutputStream buffer=new ByteArrayOutputStream()) {

					response.body(output()).use(consumer -> consumer.accept(() -> buffer));

					data=buffer.toByteArray();

				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			}

			return data;
		}

	}

}
