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

import com.metreeca.form.things.ModelSubject;
import com.metreeca.rest.formats.RDFFormat;

import com.google.common.truth.FailureMetadata;
import com.google.common.truth.Subject;
import org.junit.jupiter.api.Assertions;

import java.io.*;
import java.util.function.Function;

import static com.metreeca.form.things.Transputs.data;
import static com.metreeca.form.things.Transputs.text;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.WriterFormat.writer;

import static com.google.common.truth.Fact.fact;
import static com.google.common.truth.Fact.simpleFact;
import static com.google.common.truth.Truth.assertAbout;


public final class ResponseSubject extends Subject<ResponseSubject, Response> {

	public static ResponseSubject assertThat(final Response response) {
		return assertAbout(ResponseSubject::new).that(response);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ResponseSubject(final FailureMetadata metadata, final Response subject) {
		super(metadata, subject);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public void isSuccess() {
		if ( actual() == null ) {
			failWithoutActual(simpleFact("expected response"), fact("but was", null));
		} else if ( !actual().success() ) {
			failWithoutActual(simpleFact("expected to be success"), fact("but was", actual().status()));
		}
	}

	public void hasStatus(final int status) {
		if ( actual() == null ) {
			failWithoutActual(simpleFact("expected response"), fact("but was", null));
		} else if ( actual().status() != status ) {
			failWithoutActual(fact("expected status", status), fact("but was", actual().status()));
		}
	}

	public void hasEmptyBody() {
		if ( actual() == null ) {

			failWithoutActual(simpleFact("expected response"), fact("but was", null));

		} else {

			actual().body(output()).use(
					consumer -> {

						final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

						consumer.accept(() -> buffer);

						final int length=data(new ByteArrayInputStream(buffer.toByteArray())).length;

						if ( length > 0 ) {
							failWithoutActual(simpleFact("expected empty body"), fact("but had binary body of length", length));
						}

					},
					failure -> failWithoutActual(simpleFact("expected response"), fact("but was", null))
			);

			actual().body(writer()).use(
					consumer -> {

						final StringWriter buffer=new StringWriter();

						consumer.accept(() -> buffer);

						final int length=text(new StringReader(buffer.toString())).length();

						if ( length > 0 ) {
							failWithoutActual(simpleFact("expected empty body"), fact("but had textual body of length", length));
						}

					},
					failure -> failWithoutActual(simpleFact("expected response"), fact("but was", null))
			);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ModelSubject body(final RDFFormat format) {
		return body(format, ModelSubject::assertThat);
	}

	public <V, S extends Subject<S, ?extends V>> S body(final Format<V> format, final Function<V, S> subject) {

		final Response response=actual(); // !!! handle null


		return response.body(input()).set(() -> { // !!! handle textual body

			final ByteArrayOutputStream buffer=new ByteArrayOutputStream();

			response.body(output()).use(consumer -> consumer.accept(() -> buffer));

			return new ByteArrayInputStream(buffer.toByteArray());

		}).body(format).map(


				subject,
				failure -> Assertions.fail("unable to get body")
		);
	}

}
