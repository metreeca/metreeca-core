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

package com.metreeca.next.wrappers;

import com.metreeca.next.Handler;
import com.metreeca.link.LinkTest.Testbed;
import com.metreeca.next.Request;
import com.metreeca.next.Response;
import com.metreeca.tray.iam.Roster;
import com.metreeca.tray.iam.RosterTest;
import com.metreeca.tray.iam.RosterTest.MockRoster;

import org.junit.Test;

import static com.metreeca.link.LinkTest.testbed;
import static com.metreeca.next.wrappers.Bearer.bearer;
import static com.metreeca.tray.Tray.tool;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public final class BearerTest {

	private Testbed harness(final Handler handler) {
		return testbed()

				.toolkit(tray -> tray.set(Roster.Tool, tools -> new MockRoster()))
				.handler(() -> bearer().wrap(handler));
	}

	private Handler handler(final int status) {
		return (request, response) -> response.status(status).done();
	}


	//// Anonymous /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testAnonymousGranted() {
		harness(handler(Response.OK))

				.request(writer -> writer
						.method(Request.GET)
						.done())

				.response(reader -> {

					assertEquals("success reported", Response.OK, reader.status());
					assertTrue("empty body", reader.text().isEmpty());

					assertFalse("challenge not included", reader
							.header("WWW-Authenticate")
							.isPresent());

				});
	}

	@Test public void testAnonymousForbidden() {
		harness(handler(Response.Forbidden))

				.request(writer -> writer
						.method(Request.GET)
						.done())

				.response(reader -> {

					assertEquals("error reported", Response.Forbidden, reader.status());

					assertFalse("challenge not included", reader
							.header("WWW-Authenticate")
							.isPresent());

				});
	}

	@Test public void testAnonymousUnauthorized() {
		harness(handler(Response.Unauthorized))

				.request(writer -> writer.method(Request.GET).done())

				.response(reader -> {

					assertEquals("error reported", Response.Unauthorized, reader.status());

					assertTrue("challenge included without error", reader
							.header("WWW-Authenticate").orElse("")
							.matches("Bearer realm=\".*\""));

				});
	}


	//// BearerToken ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testAuthorizationGranted() {
		harness(handler(Response.OK))

				.exec(() -> tool(Roster.Tool).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.request(writer -> writer
						.method(Request.GET)
						.header("Authorization", "Bearer this")
						.done())

				.response(reader -> {

					assertEquals("success reported", Response.OK, reader.status());
					assertTrue("empty body", reader.text().isEmpty());

					assertFalse("challenge not included", reader
							.header("WWW-Authenticate")
							.isPresent());

				});
	}

	@Test public void testAuthorizationBadCredentials() {
		harness(handler(Response.OK))

				.exec(() -> tool(Roster.Tool).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.request(writer -> writer
						.method(Request.GET)
						.header("Authorization", "Bearer qwertyuiop")
						.done())

				.response(reader -> {

					assertEquals("error reported", Response.Unauthorized, reader.status());

					assertTrue("challenge included with error", reader
							.header("WWW-Authenticate").orElse("")
							.matches("Bearer realm=\".*\", error=\"invalid_token\""));

				});
	}

	@Test public void testAuthorizationForbidden() {
		harness(handler(Response.Forbidden))

				.exec(() -> tool(Roster.Tool).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.request(writer -> writer
						.method(Request.GET)
						.header("Authorization", "Bearer this")
						.done())

				.response(reader -> {

					assertEquals("error reported", Response.Forbidden, reader.status());

					assertFalse("challenge not included", reader
							.header("WWW-Authenticate")
							.isPresent());

				});
	}

	@Test public void testAuthorizationUnauthorized() {
		harness(handler(Response.Unauthorized))

				.exec(() -> tool(Roster.Tool).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.request(writer -> writer
						.method(Request.GET)
						.header("Authorization", "Bearer this")
						.done())

				.response(reader -> {

					assertEquals("error reported", Response.Unauthorized, reader.status());

					assertTrue("challenge included without error", reader
							.header("WWW-Authenticate").orElse("")
							.matches("Bearer realm=\".*\""));

				});
	}


	//// Other Authorization Scheme ////////////////////////////////////////////////////////////////////////////////////

	@Test public void testFallThroughToOtherSchemes() {
		harness(handler(Response.Unauthorized).wrap(handler -> (request, response) -> handler.exec(

				writer -> writer.copy(request).done(),

				reader -> response.copy(reader).header("WWW-Authenticate", "Custom").done()

		)))

				.exec(() -> tool(Roster.Tool).acquire(RosterTest.This, RosterTest.This)) // acquire token

				.request(writer -> writer
						.method(Request.GET)
						.header("Authorization", "Custom secret")
						.done())

				.response(reader -> {

					assertEquals("error reported", Response.Unauthorized, reader.status());

					assertEquals("fall-through challenge", "Custom",
							reader.header("WWW-Authenticate").orElse(""));

				});
	}

}
