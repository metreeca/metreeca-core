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
import com.metreeca.gate.RosterAssert;
import com.metreeca.gate.RosterAssert.MockRoster;
import com.metreeca.rest.*;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

import static com.metreeca.form.things.JsonValues.field;
import static com.metreeca.form.things.JsonValues.object;
import static com.metreeca.form.truths.JsonAssert.assertThat;
import static com.metreeca.gate.Roster.roster;
import static com.metreeca.gate.RosterAssert.user;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.bodies.JSONBody.json;
import static com.metreeca.tray.Tray.tool;


final class ManagerTest {

	private static final int SoftExpiry=100;
	private static final int HardExpiry=200;


	private void exec(final Runnable... tasks) {
		new Tray()

				.set(roster(), () -> new MockRoster("faussone"))

				.exec(tasks)

				.clear();
	}


	private Wrapper manager() {
		return new Manager("/~", SoftExpiry, HardExpiry);
	}

	private Handler handler(final Function<IRI, Integer> status) {
		return request -> request.reply(response -> response.status(status.apply(request.user())));
	}


	@Nested final class Management {

		@Test void testOpenSession() {
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
							.hasStatus(Response.Created)
							.hasHeader("Authorization")
							.hasBody(json(), json -> assertThat(json)
									.hasField("user", "faussone")
							)
					)
			);
		}

		@Test void testOpenSessionAndUpdateSecret() {
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
										.hasStatus(Response.Created)
										.hasHeader("Authorization")
										.hasBody(json(), json -> assertThat(json)
												.hasField("user", "faussone")
										);

								RosterAssert.assertThat(tool(roster()))
										.hasUser(user("faussone"), "new-secret")
										.doesNotHaveUser(user("faussone"), "faussone");

							}
					)
			);
		}


		@Test void testRejectedCredentials() {
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
							.body(json(), object())
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.BadRequest)
							.hasBody(json(), json -> assertThat(json)
									.hasField("error")
							)
					)
			);
		}

	}

	@Nested final class Authentication {

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

					.accept(response -> consumer.accept(

							handler, response.header("Authorization").orElse("")

					));
		}


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

				).accept((handler, authorization) -> handler

						.handle(new Request()
								.method(Request.GET)
								.header("Authorization", authorization)
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


			@Test void testBadCredentials() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, authorization) -> handler

						.handle(new Request()
								.method(Request.GET)
								.header("Authorization", "Bearer qwertyuiop")
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("error reported")
								.hasStatus(Response.Unauthorized)

								.as("challenge included with error")
								.matches(r -> r
										.header("WWW-Authenticate").orElse("")
										.matches("Bearer realm=\".*\", error=\"invalid_token\"")
								)

						)

				));
			}

			@Test void testForbidden() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> Response.Forbidden))

				).accept((handler, authorization) -> handler

						.handle(new Request()
								.method(Request.GET)
								.header("Authorization", authorization)
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

				).accept((handler, authorization) -> handler

						.handle(new Request()
								.method(Request.GET)
								.header("Authorization", authorization)
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("error reported")
								.hasStatus(Response.Unauthorized)


								.as("challenge included without error")
								.matches(r -> r
										.header("WWW-Authenticate").orElse("")
										.matches("Bearer realm=\".*\"")
								)

						)

				));
			}

		}

		@Nested final class Expiry {

			@Test void testNoExpiry() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, authorization) -> handler

						.handle(new Request() // submit request before soft expiry

								.method(Request.GET)
								.header("Authorization", authorization)
								.path("/resource")
						)

						.accept(response -> assertThat(response)

								.as("access granted")
								.hasStatus(Response.OK)

								.as("authorization replayed")
								.hasHeader("Authorization", authorization)

						)
				));
			}

			@Test void testSoftExpiry() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, authorization) -> {

							try { Thread.sleep(SoftExpiry+10); } catch ( final InterruptedException ignored ) {}

							handler // submit request between soft and hard expiry

									.handle(new Request()
											.method(Request.GET)
											.header("Authorization", authorization)
											.path("/resource")
									)

									.accept(response -> assertThat(response)

											.as("access granted")
											.hasStatus(Response.OK)

											.as("authorization regenerated")
											.hasHeader("Authorization")
											.matches(r -> !r
													.header("Authorization")
													.orElse("")
													.equals(authorization)
											)

									);
						}
				));
			}

			@Test void testHashChange() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, authorization) -> {

							handler // update credentials > hash changes

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
											.hasStatus(Response.Created)
									);

							try { Thread.sleep(SoftExpiry+10); } catch ( final InterruptedException ignored ) {}

							handler // submit request after hash change

									.handle(new Request()
											.method(Request.GET)
											.header("Authorization", authorization)
											.path("/resource")
									)

									.accept(response -> assertThat(response)

											.as("access denied")
											.hasStatus(Response.Forbidden)

											.as("no authorization included")
											.doesNotHaveHeader("Authorization")

									);
						}
				));
			}

			@Test void testHardExpiry() {
				exec(() -> authenticate(manager()

						.wrap(handler(user -> user.equals(Form.none) ? Response.Unauthorized : Response.OK))

				).accept((handler, authorization) -> {

							try { Thread.sleep(HardExpiry+10); } catch ( final InterruptedException ignored ) {}

							handler // submit request after hard expiry

									.handle(new Request()
											.method(Request.GET)
											.header("Authorization", authorization)
											.path("/resource")
									)

									.accept(response -> assertThat(response)

											.as("access denied")
											.hasStatus(Response.Forbidden)

											.as("no authorization included")
											.doesNotHaveHeader("Authorization")

									);
						}
				));
			}

		}

	}

}
