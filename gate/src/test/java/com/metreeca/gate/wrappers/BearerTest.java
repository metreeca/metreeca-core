/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.gate.wrappers;

import com.metreeca.rest.*;
import com.metreeca.tray.Tray;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.rest.ResponseAssert.assertThat;


final class BearerTest {

	private void exec(final Runnable... tasks) {
		new Tray().exec(tasks).clear();
	}


	private Bearer bearer() {
		return new Bearer((now, token) -> token.equals("token")? Optional.of(handler -> handler) : Optional.empty());
	}

	private Handler handler(final int status) {
		return request -> request.reply(response -> response.status(status));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testFallThroughToOtherSchemes() {

		final Wrapper authenticator=handler -> request ->
				handler.handle(request).map(response -> response.header("WWW-Authenticate", "Custom"));

		exec(() -> bearer()

				.wrap(authenticator)
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
