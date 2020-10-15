/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
