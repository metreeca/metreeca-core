/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.gate.wrappers;

import com.metreeca.form.things.Codecs;
import com.metreeca.gate.Roster;
import com.metreeca.gate.RosterTest;
import com.metreeca.rest.*;
import com.metreeca.tray.Tray;

import org.junit.jupiter.api.Test;

import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.tray.Tray.tool;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;


final class BearerTest {

	private Tray tray() {
		return new Tray().set(Roster.Factory, RosterTest.MockRoster::new);
	}

	private Handler bearer(final Handler handler) {
		return new Bearer().wrap(handler);
	}

	private Handler handler(final int status) {
		return request -> request.reply(response -> response.status(status));
	}


	//// Anonymous /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testAnonymousGranted() {
		tray()

				.get(() -> bearer(handler(Response.OK)))

				.handle(new Request()
						.method(Request.GET)
						.body(reader()).set(Codecs::reader))

				.accept(response -> {

					assertEquals(Response.OK, response.status());
					assertFalse(response.body(text()).get().isPresent());

					assertFalse(response
							.header("WWW-Authenticate")
							.isPresent(), "challenge not included");

				});
	}

	@Test void testAnonymousForbidden() {
		tray()

				.get(() -> bearer(handler(Response.Forbidden)))

				.handle(new Request()
						.method(Request.GET))

				.accept(response -> {

					assertEquals(Response.Forbidden, response.status(), "error reported");

					assertFalse(response
							.header("WWW-Authenticate")
							.isPresent(), "challenge not included");

				});
	}

	@Test void testAnonymousUnauthorized() {
		tray()

				.get(() -> bearer(handler(Response.Unauthorized)))

				.handle(new Request()
						.method(Request.GET))

				.accept(response -> {

					assertEquals(Response.Unauthorized, response.status(), "error reported");

					assertTrue(response
									.header("WWW-Authenticate").orElse("")
									.matches("Bearer realm=\".*\""),
							"challenge included without error");

				});
	}


	//// BearerToken ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testAuthorizationGranted() {
		tray()

				.exec(() -> tool(Roster.Factory).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.get(() -> bearer(handler(Response.OK)))

				.handle(new Request()
						.method(Request.GET)
						.header("Authorization", "Bearer this"))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.describedAs("challenge not included").doesNotHaveHeader("WWW-Authenticate")
						.doesNotHaveBody());
	}

	@Test void testAuthorizationBadCredentials() {
		tray()

				.exec(() -> tool(Roster.Factory).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.get(() -> bearer(handler(Response.OK)))

				.handle(new Request()
						.method(Request.GET)
						.header("Authorization", "Bearer qwertyuiop"))

				.accept(reader -> {

					assertEquals(Response.Unauthorized, reader.status(), "error reported");

					assertTrue(reader
									.header("WWW-Authenticate").orElse("")
									.matches("Bearer realm=\".*\", error=\"invalid_token\""),
							"challenge included with error");

				});
	}

	@Test void testAuthorizationForbidden() {
		tray()

				.exec(() -> tool(Roster.Factory).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.get(() -> bearer(handler(Response.Forbidden)))

				.handle(new Request()
						.method(Request.GET)
						.header("Authorization", "Bearer this"))

				.accept(response -> {

					assertEquals(Response.Forbidden, response.status(), "error reported");

					assertFalse(response
									.header("WWW-Authenticate")
									.isPresent(),
							"challenge not included");

				});
	}

	@Test void testAuthorizationUnauthorized() {
		tray()

				.exec(() -> tool(Roster.Factory).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.get(() -> bearer(handler(Response.Unauthorized)))

				.handle(new Request()
						.method(Request.GET)
						.header("Authorization", "Bearer this"))

				.accept(reader -> {

					assertEquals(Response.Unauthorized, reader.status(), "error reported");

					assertTrue(reader
									.header("WWW-Authenticate").orElse("")
									.matches("Bearer realm=\".*\""),
							"challenge included without error");

				});
	}


	//// Other Authorization Scheme ////////////////////////////////////////////////////////////////////////////////////

	@Test void testFallThroughToOtherSchemes() {

		final Wrapper authenticator=handler -> request ->
				handler.handle(request).map(response -> response.header("WWW-Authenticate", "Custom"));

		tray()

				.exec(() -> tool(Roster.Factory).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.get(() -> bearer(authenticator.wrap(handler(Response.Unauthorized))))

				.handle(new Request()
						.method(Request.GET)
						.header("Authorization", "Custom secret"))

				.accept(reader -> {

					assertEquals(Response.Unauthorized, reader.status(), "error reported");

					assertEquals("Custom", reader.header("WWW-Authenticate").orElse(""),
							"fall-through challenge");

				});
	}

}
