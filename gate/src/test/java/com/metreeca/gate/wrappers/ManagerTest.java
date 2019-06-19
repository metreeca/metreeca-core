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

import com.metreeca.form.Form;
import com.metreeca.gate.Roster;
import com.metreeca.gate.RosterAssert;
import com.metreeca.gate.RosterAssert.MockRoster;
import com.metreeca.rest.*;
import com.metreeca.tray.Tray;
import com.metreeca.tray.sys.ClockTest.MockClock;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.metreeca.form.things.JsonValues.field;
import static com.metreeca.form.things.JsonValues.object;
import static com.metreeca.form.things.JsonValues.value;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.truths.JsonAssert.assertThat;
import static com.metreeca.gate.Roster.roster;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.bodies.JSONBody.json;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Clock.clock;

import static java.lang.Math.max;


final class ManagerTest {

	private static final long SoftTimeout=Duration.ofHours(1).toMillis();
	private static final long HardTimeout=Duration.ofDays(1).toMillis();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final MockClock clock=new MockClock();
	private final Roster roster=new MockRoster(entry("faussone", "faussone"));

	private void exec(final Runnable... tasks) {
		new Tray()

				.set(clock(), () -> clock.time(0))
				.set(roster(), () -> roster)

				.exec(tasks)

				.clear();
	}


	private Wrapper manager() {
		return new Manager("/~", SoftTimeout, HardTimeout);
	}

	private Handler handler(final Function<IRI, Integer> status) {
		return request -> request.reply(response -> response.status(status.apply(request.user())));
	}


	private Consumer<BiConsumer<Handler, String>> authenticate(final Handler handler) {
		return consumer -> handler

				// open session

				.handle(new Request()
						.method(Request.POST)
						.path("/~")
						.body(json(), object(
								field("handle", "faussone"),
								field("secret", "faussone")
						))
				)

				// handle requests

				.accept(response -> consumer.accept(handler, response

						.header("Set-Cookie")
						.map(cookie -> cookie.substring(0, max(0, cookie.indexOf(';'))))
						.orElse("")

				));
	}


	@Nested final class Management {

		@Test void testQuerySessionAnonymous() {
			exec(() -> manager()

					.wrap(handler(user -> Response.OK))

					.handle(new Request()
							.method(Request.GET)
							.path("/~")
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasHeader("Cache-Control", cache -> Assertions.assertThat(cache)
									.contains("timeout=0")
							)
							.hasBody(json(), json -> assertThat(json)
									.isEqualTo(object())
							)
					)
			);
		}

		@Test void testQuerySessionIdentified() {
			exec(() -> authenticate(manager()

					.wrap(handler(user -> Response.OK))

			).accept((handler, cookie) -> handler

					.handle(new Request()
							.method(Request.GET)
							.path("/~")
							.header("Cookie", cookie)
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasHeader("Cache-Control", cache -> Assertions.assertThat(cache)
									.contains("timeout=")
							)
							.hasBody(json(), json -> assertThat(json)
									.hasField("user", value("faussone"))
							)
					)
			));
		}


		@Test void testCreateSession() {
			exec(() -> manager()

					.wrap(handler(user -> Response.OK))

					.handle(new Request()
							.method(Request.POST)
							.path("/~")
							.body(json(), object(
									field("handle", "faussone"),
									field("secret", "faussone")
							))
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasHeader("Set-Cookie")
							.hasHeader("Cache-Control", cache -> Assertions.assertThat(cache)
									.contains("timeout=")
							)
							.hasBody(json(), json -> assertThat(json)
									.hasField("user", value("faussone"))
							)
					)
			);
		}

		@Test void testCreateSessionAndUpdateSecret() {
			exec(() -> manager()

					.wrap(handler(user -> Response.OK))

					.handle(new Request()
							.method(Request.POST)
							.path("/~")
							.body(json(), object(
									field("handle", "faussone"),
									field("secret", "faussone"),
									field("update", "new-secret")
							))
					)

					.accept(response -> {

						assertThat(response)
								.hasStatus(Response.OK)
								.hasHeader("Set-Cookie")
								.hasHeader("Cache-Control", cache -> Assertions.assertThat(cache)
										.contains("timeout=")
								)
								.hasBody(json(), json -> assertThat(json)
										.hasField("user", value("faussone"))
								);

						RosterAssert.assertThat(tool(roster()))
								.hasUser("faussone", "new-secret")
								.doesNotHaveUser("faussone", "faussone");

					})
			);
		}

		@Test void testDeleteSession() {
			exec(() -> manager()

					.wrap(handler(user -> Response.OK))

					.handle(new Request()
							.method(Request.POST)
							.path("/~")
							.body(json(), object())
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasHeader("Set-Cookie", cookie -> Assertions.assertThat(cookie)
									.startsWith(Manager.SessionCookie+"=;")
							)
							.hasHeader("Cache-Control", cache -> Assertions.assertThat(cache)
									.contains("timeout=0")
							)
							.hasBody(json(), json -> assertThat(json)
									.isEqualTo(object())
							)
					)
			);
		}


		@Test void testInvalidCredentials() {
			exec(() -> manager()

					.wrap(handler(user -> Response.OK))

					.handle(new Request()
							.method(Request.POST)
							.path("/~")
							.body(json(), object(
									field("handle", "faussone"),
									field("secret", "invalid")
							))
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.Forbidden)
							.doesNotHaveHeader("Set-Cookie")
							.hasBody(json(), json -> assertThat(json)
									.hasField("error")
							)
					)
			);
		}

		@Test void testMalformedTicket() {
			exec(() -> manager()

					.wrap(handler(user -> Response.OK))

					.handle(new Request()
							.method(Request.POST)
							.path("/~")
							.body(json(), object(
									field("handle", "")
							))
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.BadRequest)
							.doesNotHaveHeader("Set-Cookie")
							.hasBody(json(), json -> assertThat(json)
									.hasField("error")
							)
					)
			);
		}

	}

	@Nested final class Authentication {

		@Nested final class Anonymous {

			@Test void testGranted() {
				exec(() -> manager()

						.wrap(handler(user -> Response.OK))

						.handle(new Request()
								.method(Request.GET)
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("access granted")
								.hasStatus(Response.OK)

								.as("challenge not included")
								.doesNotHaveHeader("WWW-Authenticate")

						)
				);
			}

			@Test void testForbidden() {
				exec(() -> manager()

						.wrap(handler(user -> Response.Forbidden))

						.handle(new Request()
								.method(Request.GET)
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("error reported")
								.hasStatus(Response.Forbidden)

								.as("challenge not included")
								.doesNotHaveHeader("WWW-Authenticate")

						)
				);
			}

			@Test void testUnauthorized() {
				exec(() -> manager()

						.wrap(handler(user -> Response.Unauthorized))

						.handle(new Request()
								.method(Request.GET)
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("error reported")
								.hasStatus(Response.Unauthorized)

								.as("challenge included")
								.hasHeader("WWW-Authenticate")

						)
				);
			}

		}

		@Nested final class Identified {

			@Test void testGranted() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, cookie) -> handler

						.handle(new Request()
								.method(Request.GET)
								.header("Cookie", cookie)
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("access granted")
								.hasStatus(Response.OK)

								.as("challenge not included")
								.doesNotHaveHeader("WWW-Authenticate")

						)
				));
			}


			@Test void testInvalidCookie() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, authorization) -> handler

						.handle(new Request()
								.method(Request.GET)
								.header("Cookie", Manager.SessionCookie+"=qwertyuiop")
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("error reported")
								.hasStatus(Response.Unauthorized)

								.as("challenge included")
								.hasHeader("WWW-Authenticate")

						)

				));
			}

			@Test void testForbidden() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> Response.Forbidden))

				).accept((handler, cookie) -> handler

						.handle(new Request()
								.method(Request.GET)
								.header("Cookie", cookie)
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("error reported")
								.hasStatus(Response.Forbidden)

								.as("challenge not included")
								.doesNotHaveHeader("WWW-Authenticate")

						)

				));
			}

			@Test void testUnauthorized() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> Response.Unauthorized))

				).accept((handler, cookie) -> handler

						.handle(new Request()
								.method(Request.GET)
								.header("Cookie", cookie)
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("error reported")
								.hasStatus(Response.Unauthorized)

								.as("challenge included")
								.hasHeader("WWW-Authenticate")

						)

				));
			}

		}

		@Nested final class Expiry {

			@Test void testDeletion() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, cookie) -> {

					handler // delete session

							.handle(new Request()
									.method(Request.POST)
									.path("/~")
									.header("Cookie", cookie)
									.body(json(), object())
							)

							.accept(response -> assertThat(response)
									.hasStatus(Response.OK)
									.hasBody(json(), json -> assertThat(json)
											.isEqualTo(object())
									)
							);

					handler // submit request after session deletion

							.handle(new Request()
									.method(Request.GET)
									.header("Cookie", cookie)
									.path("/resource")
							)

							.accept(response -> assertThat(response)

									.as("access denied")
									.hasStatus(Response.Unauthorized)

							);
				}));
			}


			@Test void testHandleChange() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, cookie) -> {

					handler // update credentials > handle changes

							.handle(new Request()
									.method(Request.POST)
									.path("/~")
									.body(json(), object(
											field("handle", "faussone"),
											field("secret", "faussone"),
											field("update", "new-secret")
									))
							)

							.accept(response -> assertThat(response)
									.hasStatus(Response.OK)
							);


					handler // submit request after handle change

							.handle(new Request()
									.method(Request.GET)
									.header("Cookie", cookie)
									.path("/resource")
							)

							.accept(response -> assertThat(response)

									.as("access denied")
									.hasStatus(Response.Unauthorized)

							);
				}));
			}

			@Test void testExtension() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, cookie) -> {

					clock.time(SoftTimeout*50/100);

					handler // submit request before soft expiry

							.handle(new Request()
									.method(Request.GET)
									.header("Cookie", cookie)
									.path("/resource")
							)

							.accept(response -> assertThat(response)

									.as("access granted")
									.hasStatus(Response.OK)

									.as("token extended")
									.hasHeader("Set-Cookie", extended -> Assertions.assertThat(extended)
											.doesNotStartWith(cookie)
									)

							);
				}));
			}

			@Test void testSoftExpiry() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, cookie) -> {

					clock.time(SoftTimeout*150/100);

					handler // submit request after soft expiry

							.handle(new Request()
									.method(Request.GET)
									.header("Cookie", cookie)
									.path("/resource")
							)

							.accept(response -> assertThat(response)

									.as("access denied")
									.hasStatus(Response.Unauthorized)

							);
				}));
			}

		}

	}

}
