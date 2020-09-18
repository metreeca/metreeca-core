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

package com.metreeca.rest.handlers;

import com.metreeca.rest.*;
import com.metreeca.rest.formats.OutputFormat;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.Condition;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;

import static com.metreeca.rest.ResponseAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


final class RouterTest {

	@Nested final class Paths {

		private Request request(final String path) {
			return new Request().path(path);
		}

		private Handler handler() {
			return request -> request.reply(response -> response
					.status(Response.OK)
					.header("path", request.path())
			);
		}


		@Test void testCheckPaths() {

			assertThatExceptionOfType(IllegalArgumentException.class)
					.as("empty path")
					.isThrownBy(() -> Router.router()
							.path("", handler())
					);

			assertThatExceptionOfType(IllegalArgumentException.class)
					.as("missing leading slash path")
					.isThrownBy(() -> Router.router()
							.path("path", handler())
					);

			assertThatExceptionOfType(IllegalArgumentException.class)
					.as("malformed placeholder step")
					.isThrownBy(() -> Router.router()
							.path("/pa{}th", handler())
					);

			assertThatExceptionOfType(IllegalArgumentException.class)
					.as("malformed prefix step")
					.isThrownBy(() -> Router.router()
							.path("/pa*th", handler())
					);

			assertThatExceptionOfType(IllegalArgumentException.class)
					.as("inline prefix step")
					.isThrownBy(() -> Router.router()
							.path("/*/path", handler())
					);

			assertThatExceptionOfType(IllegalStateException.class)
					.as("existing path")
					.isThrownBy(() -> Router.router()
							.path("/path", handler())
							.path("/path", handler())
					);

		}

		@Test void testIgnoreUnknownPath() {
			Router.router()

					.path("/path", handler())

					.handle(request("/unknown"))

					.accept(response -> assertThat(response)
							.as("request ignored")
							.doesNotHaveHeader("path")
					);
		}


		@Test void testMatchesLiteralPath() {

			final Router router=Router.router().path("/path", handler());

			router.handle(request("/path")).accept(response -> assertThat(response)
					.hasHeader("path", "/path")
			);

			router.handle(request("/path/")).accept(response -> assertThat(response)
					.doesNotHaveHeader("path")
			);

			router.handle(request("/path/unknown")).accept(response -> assertThat(response)
					.doesNotHaveHeader("path")
			);

		}

		@Test void testMatchesPlaceholderPath() {

			final Router router=Router.router().path("/head/{id}/tail", handler());

			router.handle(request("/head/path/tail")).accept(response -> assertThat(response)
					.hasHeader("path", "/head/path/tail")
			);

			router.handle(request("/head/tail")).accept(response -> assertThat(response)
					.doesNotHaveHeader("path")
			);

			router.handle(request("/head/path/path/tail")).accept(response -> assertThat(response)
					.doesNotHaveHeader("path")
			);

		}

		@Test void testSavePlaceholderValuesAsRequestParameters() {

			Router.router().path("/{head}/{tail}", handler())
					.handle(request("/one/two"))
					.accept(response -> RequestAssert.assertThat(response.request())
							.as("placeholder values saved as parameters")
							.hasParameter("head", "one")
							.hasParameter("tail", "two")
					);

			Router.router().path("/{}/{}", handler())
					.handle(request("/one/two"))
					.accept(response -> RequestAssert.assertThat(response.request())
							.has(new Condition<>(
									request -> request.parameters().isEmpty(),
									"empty placeholders ignored")
							)
					);

		}

		@Test void testMatchesPrefixPath() {

			final Router router=Router.router().path("/head/*", handler());

			router.handle(request("/head/path")).accept(response -> assertThat(response)
					.hasHeader("path", "/head/path")
			);

			router.handle(request("/head/path/path")).accept(response -> assertThat(response)
					.hasHeader("path", "/head/path/path")
			);

			router.handle(request("/head")).accept(response -> assertThat(response)
					.doesNotHaveHeader("path")
			);

		}


		@Test void testPreferFirstMatch() {

			final Router router=Router.router()

					.path("/path", request -> request.reply(response -> response.status(100)))
					.path("/*", request -> request.reply(response -> response.status(200)));

			router.handle(request("/path")).accept(response -> assertThat(response).hasStatus(100));
			router.handle(request("/path/path")).accept(response -> assertThat(response).hasStatus(200));

		}

	}

	@Nested final class Methods {

		private Future<Response> handler(final Request request) {
			return request.reply(response -> response

					.status(Response.OK)

					.body(OutputFormat.output(), output -> {
						try {
							output.write("body".getBytes());
						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}
					})

			);
		}


		@Test void testHandleOPTIONSByDefault() {
			Router.router()

					.get(this::handler)

					.handle(new Request().method(Request.OPTIONS))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasHeaders("Allow", Request.OPTIONS, Request.HEAD, Request.GET)
					);
		}

		@Test void testIncludeAllowHeaderOnUnsupportedMethods() {
			Router.router()

					.get(this::handler)

					.handle(new Request().method(Request.POST))

					.accept(response -> assertThat(response)
							.hasStatus(Response.MethodNotAllowed)
							.hasHeaders("Allow", Request.OPTIONS, Request.HEAD, Request.GET)
					);
		}

		@Test void testHandleHEADByDefault() {
			Router.router()

					.get(this::handler)

					.handle(new Request().method(Request.HEAD))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(OutputFormat.output(), target -> {

								final ByteArrayOutputStream output=new ByteArrayOutputStream();

								target.accept(output);

								Assertions.assertThat(output.toByteArray()).isEmpty();

							})
					);
		}

		@Test void testRejectHEADIfGetIsNotSupported() {
			Router.router()

					.handle(new Request().method(Request.HEAD))

					.accept(response -> assertThat(response)
							.hasStatus(Response.MethodNotAllowed)
					);
		}

	}

}
