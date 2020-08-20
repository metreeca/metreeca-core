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

package com.metreeca.gate.wrappers;

import com.metreeca.rest.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.rest.ResponseAssert.assertThat;


final class BearerTest {

	private void exec(final Runnable... tasks) {
		new Context().exec(tasks).clear();
	}


	private Bearer bearer() {
		return new Bearer((token, request) -> token.equals("token") ? Optional.of(request) : Optional.empty());
	}

	private Handler handler(final int status) {
		return request -> request.reply(response -> response.status(status));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testFallThroughToWrappedSchemes() {

		final Wrapper authenticator=handler -> request ->
				handler.handle(request).map(response -> response.header("WWW-Authenticate", "Custom"));

		exec(() -> bearer()

				.with(authenticator)
				.wrap(handler(Response.Unauthorized))

				.handle(new Request()
						.method(Request.GET)
						.header("Authorization", "Custom secret")
				)

				.accept(response -> assertThat(response)

						.as("access denied")
						.hasStatus(Response.Unauthorized)

						.as("fall-through challenge")
						.hasHeader("WWW-Authenticate", "Custom")

				)
		);
	}


	@Nested final class Anonymous {

		@Test void testGranted() {
			exec(() -> bearer()

					.wrap(handler(Response.OK))

					.handle(new Request()
							.method(Request.GET)
					)

					.accept(response -> assertThat(response)

							.as("access granted")
							.hasStatus(Response.OK)

							.as("challenge not included")
							.doesNotHaveHeader("WWW-Authenticate"))
			);
		}

		@Test void testForbidden() {
			exec(() -> bearer()

					.wrap(handler(Response.Forbidden))

					.handle(new Request()
							.method(Request.GET)
					)

					.accept(response -> assertThat(response)

							.as("access denied")
							.hasStatus(Response.Forbidden)

							.as("challenge not included")
							.doesNotHaveHeader("WWW-Authenticate"))
			);
		}

		@Test void testUnauthorized() {
			exec(() -> bearer()

					.wrap(handler(Response.Unauthorized))

					.handle(new Request()
							.method(Request.GET)
					)

					.accept(response -> assertThat(response)

							.as("access denied")
							.hasStatus(Response.Unauthorized)

							.as("challenge included without error")
							.matches(r -> r
									.header("WWW-Authenticate").orElse("")
									.matches("Bearer realm=\".*\"")
							)
					)
			);
		}

	}


	@Nested final class TokenBearing {

		@Test void testGranted() {
			exec(() -> bearer()

					.wrap(handler(Response.OK))

					.handle(new Request()
							.method(Request.GET)
							.header("Authorization", "Bearer token")
					)

					.accept(response -> assertThat(response)

							.as("access granted")
							.hasStatus(Response.OK)

							.as("challenge not included")
							.doesNotHaveHeader("WWW-Authenticate")
					)
			);
		}

		@Test void testBadCredentials() {
			exec(() -> bearer()

					.wrap(handler(Response.OK))

					.handle(new Request()
							.method(Request.GET)
							.header("Authorization", "Bearer qwertyuiop")
					)

					.accept(response -> assertThat(response)

							.as("access denied")
							.hasStatus(Response.Unauthorized)

							.as("challenge included with error")
							.matches(r -> r
									.header("WWW-Authenticate").orElse("")
									.matches("Bearer realm=\".*\", error=\"invalid_token\"")
							)
					)
			);
		}

		@Test void testForbidden() {
			exec(() -> bearer()

					.wrap(handler(Response.Forbidden))

					.handle(new Request()
							.method(Request.GET)
							.header("Authorization", "Bearer token")
					)

					.accept(response -> assertThat(response)

							.as("access denied")
							.hasStatus(Response.Forbidden)

							.as("challenge not included")
							.doesNotHaveHeader("WWW-Authenticate")
					)
			);
		}

		@Test void testUnauthorized() {
			exec(() -> bearer()

					.wrap(handler(Response.Unauthorized))

					.handle(new Request()
							.method(Request.GET)
							.header("Authorization", "Bearer token")
					)

					.accept(response -> assertThat(response)

							.as("access denied")
							.hasStatus(Response.Unauthorized)

							.as("challenge included without error")
							.matches(r -> r
									.header("WWW-Authenticate").orElse("")
									.matches("Bearer realm=\".*\"")
							)
					)
			);
		}

	}

}
