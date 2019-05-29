/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.rest.HandlerTest.echo;
import static com.metreeca.rest.HandlerTest.exec;
import static com.metreeca.rest.ResponseAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;

import static java.lang.String.format;


final class ProtectorTest {

	@Nested final class TransportSecurity {

		@Test void testIsDisabledByDefault() {
			exec(() -> new Protector()

					.wrap(echo())

					.handle(new Request()
							.method(Request.GET)
							.base("http://example.com/")
					)

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK)
							.doesNotHaveHeader("Strict-Transport-Security")

					)
			);
		}

		@Test void testRedirectHTTPRequests() {
			exec(() -> new Protector()

					.secure(true)

					.wrap(echo())

					.handle(new Request()
							.method(Request.GET)
							.base("http://example.com/")
					)

					.accept(response -> assertThat(response)

							.hasStatus(Response.TemporaryRedirect)
							.hasHeader("Location", "https://example.com/")
							.doesNotHaveHeader("Strict-Transport-Security")

					)
			);
		}

		@Test void testSetStrictTransportSecurityHeader() {
			exec(() -> new Protector()

					.secure(true)

					.wrap(echo())

					.handle(new Request()
							.method(Request.GET)
							.base("https://example.com/")
					)

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK)
							.hasHeader("Strict-Transport-Security")

					)
			);
		}


	}

	@Nested final class XSSProtection {

		@Test void testIsDisabledByDefault() {
			exec(() -> new Protector()

					.wrap(echo())

					.handle(new Request()
							.method(Request.GET)
							.base("http://example.com/")
					)

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK)
							.doesNotHaveHeader("Content-Security-Policy")
							.doesNotHaveHeader("X-XSS-Protection")

					)
			);
		}

		@Test void testSetPolicyHeaders() {
			exec(() -> new Protector()

					.policy(true)

					.wrap(echo())

					.handle(new Request()
							.method(Request.GET)
					)

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK)
							.hasHeader("Content-Security-Policy")
							.hasHeader("X-XSS-Protection")

					)
			);
		}

	}

	@Nested final class XSRFProtection {

		private void play(
				final Supplier<Handler> factory,
				final Function<Request, Request> supplier,
				final Consumer<Response> consumer
		) {
			exec(() -> {

				final Handler handler=factory.get();

				handler

						.handle(new Request().method(Request.GET)) // generate XSRF token

						.accept(response -> {

									final String token=response
											.header("Set-Cookie")
											.map(value -> value.replaceAll("^[^=]+=([^\\s;]+).*$", "$1"))
											.orElse("");

									final Request request=new Request() // XSRF token >> copy to header
											.header(Protector.XSRFHeader, token)
											.header("Cookie", format("%s=%s", Protector.XSRFCookie, token));

									handler.handle(supplier.apply(request)).accept(consumer);

								}

						);

			});
		}


		@Test void testIsDisabledByDefault() {
			exec(() -> new Protector()

					.wrap(echo())

					.handle(new Request()
							.method(Request.GET)
							.base("http://example.com/")
					)

					.accept(response -> assertThat(response)

							.hasStatus(Response.OK)
							.doesNotHaveHeader("Set-Cookie")

					)
			);
		}


		@Nested final class TokenGeneration {

			@Test void testGenerateXSRFTokenOnSafeRequests() {
				exec(() -> new Protector()

						.token(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.GET)
						)

						.accept(response -> assertThat(response)

								.hasHeader("Set-Cookie", value -> assertThat(value)
										.matches(format("%s=[^\\s;]+; Path=%s; SameSite=Lax",
												Protector.XSRFCookie, response.request().base()
										))
								)

						)
				);
			}

			@Test void testGenerateSecureXSRFTokenOnSafeRequests() {
				exec(() -> new Protector()

						.token(true)

						.secure(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.GET)
						)

						.accept(response -> assertThat(response)

								.hasHeader("Set-Cookie", value -> assertThat(value)
										.matches(format("%s=[^\\s;]+; Path=%s; SameSite=Lax; Secure",
												Protector.XSRFCookie, response.request().base()
										))
								)

						)
				);
			}

			@Test void testDontGenerateXSRFTokenOnUnsafeRequests() {
				exec(() -> new Protector()

						.token(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.POST)
						)

						.accept(response -> assertThat(response)

								.doesNotHaveHeader("Set-Cookie")

						)
				);
			}

			@Test void testDontReplaceExistingXSRFToken() {
				play(() -> new Protector()

								.token(true)

								.wrap(echo()),

						request -> request.method(Request.POST),

						response -> assertThat(response)

								.doesNotHaveHeader("Set-Cookie")

				);
			}

			@Test void testRandomizeXSRFTokens() {
				exec(() -> {

					final Handler handler=new Protector()

							.token(true)

							.wrap(echo());

					handler

							.handle(new Request().method(Request.GET))

							.accept(response1 -> handler

									.handle(new Request().method(Request.GET))

									.accept(response2 -> assertThat(response2)

											.hasHeader("Set-Cookie", cookie2 -> assertThat(cookie2)

													.isNotEqualTo(response1.header("Set-Cookie").orElse(""))

											))
							);
				});
			}

		}

		@Nested final class TokenValidation {

			@Test void testIgnoreMissingTokenOnSafeRequest() {
				exec(() -> new Protector()

						.token(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.GET)
						)

						.accept(response -> assertThat(response)

								.hasStatus(Response.OK)

						)
				);
			}


			@Test void testReportMissingTokenOnUnsafeRequest() {
				exec(() -> new Protector()

						.token(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.POST)
						)

						.accept(response -> assertThat(response)

								.hasStatus(Response.Forbidden)

						)
				);
			}

			@Test void testReportUnpairedXSRFCookie() {
				exec(() -> new Protector()

						.token(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.POST)
								.header("Cookie", format("%s=%s", Protector.XSRFCookie, "token"))
						)

						.accept(response -> assertThat(response)

								.hasStatus(Response.Forbidden)

						)
				);
			}

			@Test void testReportUnpairedXSRFHeader() {
				exec(() -> new Protector()

						.token(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.POST)
								.header(Protector.XSRFHeader, "token")
						)

						.accept(response -> assertThat(response)

								.hasStatus(Response.Forbidden)

						)
				);
			}

			@Test void testReportMismatchedXSRFHeaderAndCookie() {
				exec(() -> new Protector()

						.token(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.POST)
								.header(Protector.XSRFHeader, "this")
								.header("Cookie", format("%s=%s", Protector.XSRFCookie, "that"))
						)

						.accept(response -> assertThat(response)

								.hasStatus(Response.Forbidden)

						)
				);
			}

			@Test void testReportForgedXSRFToken() {
				exec(() -> new Protector()

						.token(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.POST)
								.header(Protector.XSRFHeader, "made-up")
								.header("Cookie", format("%s=%s", Protector.XSRFCookie, "made-up"))
						)

						.accept(response -> assertThat(response)

								.hasStatus(Response.Forbidden)

						)
				);
			}

			@Test void testReportUnsignedXSRFToken() {

				final String token="eyJhbGciOiJub25lIiwiemlwIjoiR1pJUCJ9." // unsigned id+expiry token
						+ "H4sIAAAAAAAAAKtWyirJVLJSCsvNK44wzzOPCA8MS_QvstD2TCpwLXMrN8nJdCwK"
						+ "yYvUTkxzMs4O9LIMcrRV0lFKrShQsjI0NbU0MDY2sTCoBQA-JXtwRwAAAA.";

				exec(() -> new Protector()

						.token(true)

						.wrap(echo())

						.handle(new Request()
								.method(Request.POST)
								.header(Protector.XSRFHeader, token)
								.header("Cookie", format("%s=%s", Protector.XSRFCookie, token))
						)

						.accept(response -> assertThat(response)

								.hasStatus(Response.Forbidden)

						)
				);
			}

			@Test void testReportExpiredXSRFToken() {
				play(() -> new Protector()

								.token(10)

								.wrap(echo()),

						request -> {

							try { Thread.sleep(100);} catch ( final InterruptedException ignored ) {}

							return request.method(Request.POST);

						},

						response -> assertThat(response)

								.hasStatus(Response.Forbidden)

				);

			}


			@Test void testAcceptValidXSRFToken() {
				play(() -> new Protector()

								.token(true)

								.wrap(echo()),

						request -> request.method(Request.POST),

						response -> assertThat(response)

								.hasStatus(Response.OK)

				);

			}

		}

	}

}
