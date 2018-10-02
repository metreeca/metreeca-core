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
import com.metreeca.rest.formats.RDFFormat;

import org.assertj.core.api.AbstractAssert;
import org.assertj.core.api.Assert;
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.util.function.Function;

import static com.metreeca.form.things.Transputs.data;
import static com.metreeca.form.things.Transputs.text;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.WriterFormat.writer;


public final class ResponseAssert extends AbstractAssert<ResponseAssert, Response> {

	public static ResponseAssert assertThat(final Response response) {
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

	public ResponseAssert hasEmptyBody() {

		isNotNull();

		actual.body(output()).use(
				consumer -> {

					final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

					consumer.accept(() -> buffer);

					final int length=data(new ByteArrayInputStream(buffer.toByteArray())).length;

					if ( length > 0 ) {
						failWithMessage("expected empty body but had binary body of length <%d>", length);
					}

				},
				failure -> failWithMessage("expected response")
		);

		actual.body(writer()).use(
				consumer -> {

					final StringWriter buffer=new StringWriter();

					consumer.accept(() -> buffer);

					final int length=text(new StringReader(buffer.toString())).length();

					if ( length > 0 ) {
						failWithMessage("expected empty body but had textual body of length <%d>", length);
					}

				},
				failure -> failWithMessage("expected response")
		);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ModelAssert body(final RDFFormat format) {
		return body(format, ModelAssert::assertThat);
	}

	public <V, A extends Assert<A, ? extends V>> A body(final Format<V> format, final Function<V, A> subject) {

		// !!! handle null actual()
		// !!! handle textual body

		return actual

				.body(input())
				.set(() -> {

					final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

					actual.body(output()).use(consumer -> consumer.accept(() -> buffer));

					return new ByteArrayInputStream(buffer.toByteArray());

				})

				.body(format)
				.map(subject, failure -> Assertions.fail("unable to get body"));
	}

}
