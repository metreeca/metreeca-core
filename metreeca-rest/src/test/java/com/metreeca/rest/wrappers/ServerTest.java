/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.Request;
import com.metreeca.rest.Toolbox;
import com.metreeca.rest.formats.TextFormat;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.wrappers.Server.server;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;


final class ServerTest {

	@Nested final class QueryParsing {

		private Map.Entry<String, ? extends List<String>> parameter(final String name,
				final List<String> values) {
			return new AbstractMap.SimpleImmutableEntry<>(name, values);
		}


		@Test void testPreprocessQueryParameters() {
			new Toolbox().get(Server::server)

					.wrap(request -> {

						Assertions.assertThat(request.parameters()).containsExactly(
								parameter("one", singletonList("1")),
								parameter("two", asList("2", "2"))
						);

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.GET)
							.query("one=1&two=2&two=2"))

					.accept(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
		}

		@Test void testPreprocessBodyParameters() {
			new Toolbox().get(Server::server)

					.wrap(request -> {

						Assertions.assertThat(request.parameters()).containsExactly(
								parameter("one", singletonList("1")),
								parameter("two", asList("2", "2"))
						);

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.POST)
							.header("Content-Type", "application/x-www-form-urlencoded")
							.body(TextFormat.text(), "one=1&two=2&two=2"))

					.accept(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
		}

		@Test void testPreprocessDontOverwriteExistingParameters() {
			new Toolbox().get(Server::server)

					.wrap(request -> {

						Assertions.assertThat(request.parameters()).containsExactly(
								parameter("existing", singletonList("true"))
						);

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.GET)
							.query("one=1&two=2&two=2")
							.parameter("existing", "true"))

					.accept(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
		}

		@Test void testPreprocessQueryOnlyOnGET() {
			new Toolbox().get(Server::server)

					.wrap(request -> {

						Assertions.assertThat(request.parameters()).isEmpty();

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.PUT)
							.query("one=1&two=2&two=2"))

					.accept(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
		}

		@Test void testPreprocessBodyOnlyOnPOST() {
			new Toolbox().get(Server::server)

					.wrap(request -> {

						Assertions.assertThat(request.parameters()).isEmpty();

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.PUT)
							.header("Content-Type", "application/x-www-form-urlencoded")
							.body(TextFormat.text(), "one=1&two=2&two=2"))

					.accept(response -> Assertions.assertThat(response.status()).isEqualTo(OK));
		}

	}

	@Nested final class ErrorHandling {

		@Test void testTrapStrayExceptions() {
			new Toolbox().exec(() -> server()

					.wrap((Request request) -> { throw new UnsupportedOperationException("stray"); })

					.handle(new Request())

					.accept(response -> assertThat(response)
							.hasStatus(InternalServerError)
							.hasCause(UnsupportedOperationException.class)
					)

			);
		}

	}

}
