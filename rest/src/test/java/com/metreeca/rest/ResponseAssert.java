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

import com.metreeca.form.things.ModelAssert;
import com.metreeca.rest.formats.DataFormat;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.formats.TextFormat;

import org.assertj.core.api.*;

import java.io.*;
import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.logging.Logger;

import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;
import static com.metreeca.tray.sys.Trace.clip;

import static org.assertj.core.api.Assertions.fail;

import static java.lang.String.format;


public final class ResponseAssert extends AbstractAssert<ResponseAssert, Response> {

	public static ResponseAssert assertThat(final Response response) {
		return new ResponseAssert(response).cache();
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

		final byte[] data=data();
		final String text=text();

		if ( data.length > 0 ) {
			failWithMessage("expected empty body but had binary body of length <%d>", data.length);
		}

		if ( !text.isEmpty() ) {
			failWithMessage(
					"expected empty body but had textual body of length <%d> (%s)",
					text.length(), clip(text)
			);
		}


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
				"expected response to have <%s> body but was unable to retrieve one <%s>", error
		));
	}


	public <V> ObjectAssert<V> hasBodyThat(final Format<V> format) {
		return hasBodyThat(format, Assertions::assertThat);
	}

	public ModelAssert hasBodyThat(final RDFFormat format) {
		return hasBodyThat(format, ModelAssert::assertThat);
	}

	public <V, A extends Assert<A, ? extends V>> A hasBodyThat(final Format<V> format, final Function<V, A> mapper) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		isNotNull();

		return actual.body(format).map(mapper, error -> fail(
				"expected response to have <%s> body but was unable to retrieve one <%s>", error
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ResponseAssert cache() {

		if ( actual != null ) {

			actual

					.body(input())
					.set(new Supplier<InputStream>() { // cache binary body

						private byte[] data;

						@Override public InputStream get() {
							return new ByteArrayInputStream(data != null ? data : (data=data()));
						}

					})

					.body(reader())
					.set(new Supplier<Reader>() { // cache textual body

						private String text;

						@Override public Reader get() {
							return new StringReader(text != null ? text : (text=text()));
						}

					});

			final StringBuilder builder=new StringBuilder(1000);

			builder.append("response status").append(actual.status()).append('\n');

			actual.headers().forEach((name, values) -> values.forEach(value ->
					builder.append(name).append(": ").append(value).append('\n')
			));

			builder.append('\n');

			actual.body(DataFormat.data()).use(data -> {
				if ( data().length > 0 ) { builder.append(format("<binary body of length %d>", data.length)); }
			});

			actual.body(TextFormat.text()).use(text -> {
				if ( !text.isEmpty() ) { builder.append(text); }
			});

			Logger.getGlobal().info(indent(builder.toString()));

		}

		return this;
	}


	private byte[] data() {
		try (final ByteArrayOutputStream buffer=new ByteArrayOutputStream()) {

			actual.body(output()).use(consumer -> consumer.accept(() -> buffer));

			return buffer.toByteArray();

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

	private String text() {
		try (final StringWriter buffer=new StringWriter()) {

			actual.body(writer()).use(consumer -> consumer.accept(() -> buffer));

			return buffer.toString();

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

}
