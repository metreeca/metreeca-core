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

import com.metreeca.rest.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;


final class BearerTest {

	private void exec(final Runnable... tasks) {
		new Toolbox().exec(tasks).clear();
	}


	private Bearer bearer() {
		return Bearer.bearer((token, request) -> token.equals("token") ? Optional.of(request) : Optional.empty());
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

				.accept(response -> ResponseAssert.assertThat(response)

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

					.accept(response -> ResponseAssert.assertThat(response)

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

					.accept(response -> ResponseAssert.assertThat(response)

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

					.accept(response -> ResponseAssert.assertThat(response)

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

					.accept(response -> ResponseAssert.assertThat(response)

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

					.accept(response -> ResponseAssert.assertThat(response)

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

					.accept(response -> ResponseAssert.assertThat(response)

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

					.accept(response -> ResponseAssert.assertThat(response)

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
