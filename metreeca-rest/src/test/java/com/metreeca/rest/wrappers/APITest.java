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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.*;

import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.TextFormat.text;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;


final class APITest {

	@Nested final class QueryParsing {

		private Map.Entry<String, ? extends List<String>> parameter(final String name,
				final List<String> values) {
			return new AbstractMap.SimpleImmutableEntry<>(name, values);
		}


		@Test void testPreprocessQueryParameters() {
			new Context().get(API::new)

					.wrap((Handler)request -> {

						assertThat(request.parameters()).containsExactly(
								parameter("one", singletonList("1")),
								parameter("two", asList("2", "2"))
						);

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.GET)
							.query("one=1&two=2&two=2"))

					.accept(response -> assertThat(response.status()).isEqualTo(OK));
		}

		@Test void testPreprocessBodyParameters() {
			new Context().get(API::new)

					.wrap((Handler)request -> {

						assertThat(request.parameters()).containsExactly(
								parameter("one", singletonList("1")),
								parameter("two", asList("2", "2"))
						);

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.POST)
							.header("Content-Type", "application/x-www-form-urlencoded")
							.body(text(), "one=1&two=2&two=2"))

					.accept(response -> assertThat(response.status()).isEqualTo(OK));
		}

		@Test void testPreprocessDontOverwriteExistingParameters() {
			new Context().get(API::new)

					.wrap((Handler)request -> {

						assertThat(request.parameters()).containsExactly(
								parameter("existing", singletonList("true"))
						);

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.GET)
							.query("one=1&two=2&two=2")
							.parameter("existing", "true"))

					.accept(response -> assertThat(response.status()).isEqualTo(OK));
		}

		@Test void testPreprocessQueryOnlyOnGET() {
			new Context().get(API::new)

					.wrap((Handler)request -> {

						assertThat(request.parameters()).isEmpty();

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.PUT)
							.query("one=1&two=2&two=2"))

					.accept(response -> assertThat(response.status()).isEqualTo(OK));
		}

		@Test void testPreprocessBodyOnlyOnPOST() {
			new Context().get(API::new)

					.wrap((Handler)request -> {

						assertThat(request.parameters()).isEmpty();

						return request.reply(response -> response.status(OK));

					})

					.handle(new Request()
							.method(Request.PUT)
							.header("Content-Type", "application/x-www-form-urlencoded")
							.body(text(), "one=1&two=2&two=2"))

					.accept(response -> assertThat(response.status()).isEqualTo(OK));
		}

	}

	@Nested final class ErrorHandling {

		@Test void testTrapStrayExceptions() {
			new Context().exec(() -> new API()

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
