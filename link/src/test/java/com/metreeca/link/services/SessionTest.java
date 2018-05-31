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

package com.metreeca.link.services;

import com.metreeca.link.LinkTest.Testbed;
import com.metreeca.link.Request;
import com.metreeca.link.Response;
import com.metreeca.tray.iam.Roster;
import com.metreeca.tray.iam.RosterTest;
import com.metreeca.tray.iam.RosterTest.MockRoster;
import com.metreeca.tray.sys.Trace;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.metreeca.link.LinkTest.testbed;
import static com.metreeca.spec.things._JSON.field;
import static com.metreeca.spec.things._JSON.object;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.iam.Roster.CredentialsRejected;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.lang.System.currentTimeMillis;


public final class SessionTest {

	private Testbed harness() {
		return testbed()

				.toolkit(tray -> tray.set(Roster.Factory, tools -> new MockRoster()))
				.service(Session::new);
	}


	private Request.Writer anonymous(final Request.Writer writer) {
		return writer
				.method(Request.POST)
				.path("/~");
	}

	private Request.Writer authenticated(final Request.Writer request) {
		return anonymous(request)
				.user(tool(Roster.Factory).refresh(RosterTest.This).user());
	}


	private Object refresh() {
		return object();
	}

	private Object login(final String usr, final String pwd) {
		return object(
				field("usr", usr),
				field("pwd", pwd)
		);
	}

	private Object login(final String usr, final String old, final String neu) {
		return object(
				field("usr", usr),
				field("old", old),
				field("new", neu)
		);
	}

	private Object logout() {
		return object(
				field("usr", ""),
				field("pwd", "")
		);
	}


	//// Anonymous /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testAnonymousRefresh() {
		harness()

				.request(request -> anonymous(request)
						.json(refresh()))

				.response(response -> {

					assertEquals("success reported", Response.OK, response.status());
					assertEquals("empty ticket", object(), response.json());

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAnonymousLogout() {
		harness()

				.request(request -> anonymous(request)
						.json(logout()))

				.response(response -> {

					assertEquals("success reported", Response.OK, response.status());
					assertEquals("empty ticket", object(), response.json());

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAnonymousLogin() {
		harness()

				.request(request -> anonymous(request)
						.json(login(RosterTest.This, RosterTest.This)))

				.response(response -> {

					final Object json=response.json();

					assertEquals("success reported", Response.OK, response.status());
					assertTrue("ticket detailed", json instanceof Map);

					final Map<String, Object> ticket=(Map<String, Object>)json;

					assertTrue("label provided", ticket.get("label") instanceof String);
					assertTrue("roles provided", ticket.get("roles") instanceof List);
					assertTrue("token provided", ticket.get("token") instanceof String);
					assertTrue("lease defined", ticket.get("lease") instanceof Number);

					final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);

					assertTrue("user logged in", permit.valid(currentTimeMillis()));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAnonymousLoginBadCredentials() {
		harness()

				.request(request -> anonymous(request)
						.json(login(RosterTest.This, "qwertyuiop")))

				.response(response -> {

					final Object json=response.json();

					assertEquals("error reported", Response.Forbidden, response.status());
					assertTrue("error detailed", json instanceof Map);

					final Map<String, Object> report=(Map<String, Object>)json;

					assertEquals("error defined", CredentialsRejected, report.get("error"));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAnonymousActivate() {
		harness()

				.request(request -> anonymous(request)
						.json(login(RosterTest.This, RosterTest.This, "qwertyuiop")))

				.response(response -> {

					final Object json=response.json();

					assertEquals("success reported", Response.OK, response.status());
					assertTrue("ticket detailed", json instanceof Map);

					final Map<String, Object> ticket=(Map<String, Object>)json;

					assertTrue("label provided", ticket.get("label") instanceof String);
					assertTrue("roles provided", ticket.get("roles") instanceof List);
					assertTrue("token provided", ticket.get("token") instanceof String);
					assertTrue("lease defined", ticket.get("lease") instanceof Number);

					final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);

					assertTrue("new user logged in", permit.valid(currentTimeMillis()));
					assertEquals("password updated", "qwertyuiop", permit.token());

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAnonymousActivateBadCredentials() {
		harness()

				.request(request -> anonymous(request)
						.json(login(RosterTest.This, "qwertyuiop", "asdfghjkl")))

				.response(response -> {

					final Object json=response.json();

					assertEquals("error reported", Response.Forbidden, response.status());
					assertTrue("error detailed", json instanceof Map);

					final Map<String, Object> report=(Map<String, Object>)json;

					assertEquals("error defined", CredentialsRejected, report.get("error"));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}


	//// Authenticated /////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testAuthenticatedRefresh() {
		harness()

				.request(request -> authenticated(request)
						.json(refresh()))

				.response(response -> {

					final Object json=response.json();

					assertEquals("success reported", Response.OK, response.status());
					assertTrue("ticket detailed", json instanceof Map);

					final Map<String, Object> ticket=(Map<String, Object>)json;

					assertTrue("label provided", ticket.get("label") instanceof String);
					assertTrue("roles provided", ticket.get("roles") instanceof List);
					assertTrue("token provided", ticket.get("token") instanceof String);
					assertTrue("lease defined", ticket.get("lease") instanceof Number);

					final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);

					assertTrue("user logged in", permit.valid(currentTimeMillis()));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAuthenticatedLogout() {
		harness()

				.request(request -> authenticated(request)
						.json(logout()))

				.response(response -> {

					assertEquals("success reported", Response.OK, response.status());
					assertEquals("empty ticket", object(), response.json());

					final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);

					assertFalse("user logged out", permit.valid(currentTimeMillis()));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAuthenticatedLoginAgain() {
		harness()

				.request(request -> authenticated(request)
						.json(login(RosterTest.This, RosterTest.This)))

				.response(response -> {

					final Object json=response.json();

					assertEquals("success reported", Response.OK, response.status());
					assertTrue("ticket detailed", json instanceof Map);

					final Map<String, Object> ticket=(Map<String, Object>)json;

					assertTrue("label provided", ticket.get("label") instanceof String);
					assertTrue("roles provided", ticket.get("roles") instanceof List);
					assertTrue("token provided", ticket.get("token") instanceof String);
					assertTrue("lease defined", ticket.get("lease") instanceof Number);

					final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);

					assertTrue("user logged in", permit.valid(currentTimeMillis()));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAuthenticatedLoginAgainBadCredentials() {
		harness()

				.request(request -> authenticated(request)
						.json(login(RosterTest.This, "qwertyuiop")))

				.response(response -> {

					final Object json=response.json();

					assertEquals("error reported", Response.Forbidden, response.status());
					assertTrue("error detailed", json instanceof Map);

					final Map<String, Object> report=(Map<String, Object>)json;

					assertEquals("error defined", CredentialsRejected, report.get("error"));

					final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);

					assertTrue("user logged in", permit.valid(currentTimeMillis()));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAuthenticatedChangePassword() {
		harness()

				.request(request -> authenticated(request)
						.json(login(RosterTest.This, RosterTest.This, "qwertyuiop")))

				.response(response -> {

					final Object json=response.json();

					assertEquals("success reported", Response.OK, response.status());
					assertTrue("ticket detailed", json instanceof Map);

					final Map<String, Object> ticket=(Map<String, Object>)json;

					assertTrue("label provided", ticket.get("label") instanceof String);
					assertTrue("roles provided", ticket.get("roles") instanceof List);
					assertTrue("token provided", ticket.get("token") instanceof String);
					assertTrue("lease defined", ticket.get("lease") instanceof Number);

					final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);

					assertTrue("user logged in", permit.valid(currentTimeMillis()));
					assertEquals("password updated", "qwertyuiop", permit.token());

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAuthenticatedChangePasswordBadCredentials() {
		harness()

				.request(request -> authenticated(request)
						.json(login(RosterTest.This, "qwertyuiop", "asdfghjkl")))

				.response(response -> {

					final Object json=response.json();

					assertEquals("error reported", Response.Forbidden, response.status());
					assertTrue("error detailed", json instanceof Map);

					final Map<String, Object> report=(Map<String, Object>)json;

					assertEquals("error defined", CredentialsRejected, report.get("error"));

					final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);

					assertTrue("user logged in", permit.valid(currentTimeMillis()));
					Assert.assertEquals("password not updated", RosterTest.This, permit.token());

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}


	//// Authenticated/Switching ///////////////////////////////////////////////////////////////////////////////////////

	@Test public void testAuthenticatedSwitch() {
		harness()

				.request(request -> authenticated(request)
						.json(login(RosterTest.That, RosterTest.That)))

				.response(response -> {

					final Object json=response.json();

					assertEquals("success reported", Response.OK, response.status());
					assertTrue("ticket detailed", json instanceof Map);

					final Map<String, Object> ticket=(Map<String, Object>)json;

					assertTrue("label provided", ticket.get("label") instanceof String);
					assertTrue("roles provided", ticket.get("roles") instanceof List);
					assertTrue("token provided", ticket.get("token") instanceof String);
					assertTrue("lease defined", ticket.get("lease") instanceof Number);

					final Roster.Permit original=tool(Roster.Factory).profile(RosterTest.This);
					final Roster.Permit switched=tool(Roster.Factory).profile(RosterTest.That);

					assertFalse("old user logged out", original.valid(currentTimeMillis()));
					assertTrue("new user logged in", switched.valid(currentTimeMillis()));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAuthenticatedSwitchBadCredentials() {
		harness()

				.request(request -> anonymous(request)
						.json(login(RosterTest.That, "qwertyuiop")))

				.response(response -> {

					final Object json=response.json();

					assertEquals("error reported", Response.Forbidden, response.status());
					assertTrue("error detailed", json instanceof Map);

					final Map<String, Object> report=(Map<String, Object>)json;

					assertEquals("error defined", CredentialsRejected, report.get("error"));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}

	@Test public void testAuthenticatedSwitchAndActivate() {
		harness()

				.request(request -> anonymous(request)
						.json(login(RosterTest.That, RosterTest.That, "qwertyuiop")))

				.response(response -> {

					final Object json=response.json();

					assertEquals("success reported", Response.OK, response.status());
					assertTrue("ticket detailed", json instanceof Map);

					final Map<String, Object> ticket=(Map<String, Object>)json;

					assertTrue("label provided", ticket.get("label") instanceof String);
					assertTrue("roles provided", ticket.get("roles") instanceof List);
					assertTrue("token provided", ticket.get("token") instanceof String);
					assertTrue("lease defined", ticket.get("lease") instanceof Number);

					final Roster.Permit original=tool(Roster.Factory).profile(RosterTest.This);
					final Roster.Permit switched=tool(Roster.Factory).profile(RosterTest.That);

					assertFalse("old user logged out", original.valid(currentTimeMillis()));
					assertTrue("new user logged in", switched.valid(currentTimeMillis()));
					assertEquals("password updated", "qwertyuiop", switched.token());

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}


	@Test public void testAuthenticatedSwitchAndActivateBadCredentials() {
		harness()

				.request(request -> anonymous(request)
						.json(login(RosterTest.This, "qwertyuiop", "asdfghjkl")))

				.response(response -> {

					final Object json=response.json();

					assertEquals("error reported", Response.Forbidden, response.status());
					assertTrue("error detailed", json instanceof Map);

					final Map<String, Object> report=(Map<String, Object>)json;

					assertEquals("error defined", CredentialsRejected, report.get("error"));

					tool(Trace.Factory).info("sample request", response.request().text());
					tool(Trace.Factory).info("sample response", response.text());

				});
	}


	//// Common ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testMalformed() {
		harness()

				.request(request -> anonymous(request)
						.json(object(field("other", 123))))

				.response(response -> {

					assertEquals("error reported", Response.BadRequest, response.status());

				});
	}

}
