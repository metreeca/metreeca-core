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

import com.metreeca.form.truths.JSONAssert;
import com.metreeca.form.truths.ModelAssert;
import com.metreeca.form.things.Transputs;
import com.metreeca.rest.formats.*;

import org.assertj.core.api.*;

import java.io.*;
import java.util.Collection;
import java.util.function.Function;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;
import static com.metreeca.tray.sys.Trace.clip;

import static org.assertj.core.api.Assertions.fail;


public final class ResponseAssert extends AbstractAssert<ResponseAssert, Response> {

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
					builder.append(text.length() > builder.capacity() ? text.substring(0, builder.capacity())+"\n⋮" : text);
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


	public ResponseAssert hasHeader(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		isNotNull();

		final Collection<String> values=actual.headers(name);

		if ( values.isEmpty() ) {
			failWithMessage("expected response to have <%s> headers but has none", name);
		}

		return this;
	}

	public ResponseAssert hasHeader(final String name, final String value) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		isNotNull();

		final String found=actual.header(name).orElse(null);

		if ( !value.equals(found)) {
			failWithMessage(
					"expected response to have <%s> header with value <%s> but found <%s>",
					name, value, found
			);
		}

		return this;
	}

	public ResponseAssert doesNotHaveHeader(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		isNotNull();

		final Collection<String> values=actual.headers(name);

		if ( !values.isEmpty() ) {
			failWithMessage("expected response to have no <%s> headers but has <%s>", name, values);
		}

		return this;
	}


	public ResponseAssert doesNotHaveBody() {

		isNotNull();

		actual.body(DataFormat.data()).get().ifPresent(data -> {
			if ( data.length > 0 ) {
				failWithMessage("expected empty body but had binary body of length <%d>", data.length);
			}
		});

		actual.body(TextFormat.text()).get().ifPresent(text -> {
			if ( !text.isEmpty() ) {
				failWithMessage(
						"expected empty body but had textual body of length <%d> (%s)",
						text.length(), clip(text)
				);
			}
		});

		return this;
	}

	public ResponseAssert doesNotHaveBody(final Format<?> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		isNotNull();

		return actual.body(format).map(
				value -> fail("expected response to have no <%s> body but has one"),
				error -> this
		);
	}


	public ResponseAssert hasBody(final Format<?> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		isNotNull();

		return actual.body(format).map(value -> this, error -> fail(
				"expected response to have a <%s> body but was unable to retrieve one (%s)",
				format.getClass().getSimpleName(), error
		));
	}


	public <V> ObjectAssert<V> hasBodyThat(final Format<V> format) {
		return hasBodyThat(format, Assertions::assertThat);
	}

	public JSONAssert hasBodyThat(final JSONFormat format) {
		return hasBodyThat(format, JSONAssert::assertThat);
	}

	public ModelAssert hasBodyThat(final RDFFormat format) {
		return hasBodyThat(format, ModelAssert::assertThat);
	}

	private <V, T, A extends Assert<A, T>> A hasBodyThat(final Format<V> format, final Function<V, A> mapper) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		isNotNull();

		return actual.body(format).map(mapper, error -> fail(
				"expected response to have a <%s> body but was unable to retrieve one (%s)",
				format.getClass().getSimpleName(), error
		));
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

			return new ByteArrayInputStream(data.length == 0 ? text.getBytes(Transputs.UTF8) : data);
		}

		private StringReader reader() {

			final String text=text();
			final byte[] data=data();

			return new StringReader(text.isEmpty() ? new String(data, Transputs.UTF8) : text);
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
