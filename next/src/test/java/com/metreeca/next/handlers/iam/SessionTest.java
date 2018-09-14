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

package com.metreeca.next.handlers.iam;

@SuppressWarnings("unchecked") final class SessionTest {

	//private Handler session() {
	//	return new Tray()
	//
	//			.set(Roster.Factory, MockRoster::new)
	//
	//			.get(Session::new);
	//}
	//
	//
	//private Request anonymous() {
	//	return new Request()
	//			.method(Request.POST);
	//}
	//
	////private Request.Writer authenticated(final Request.Writer request) {
	////	return anonymous()
	////			.user(tool(Roster.Factory).refresh(RosterTest.This).user());
	////}
	//
	//
	//private JsonObject refresh() {
	//	return JsonValue.EMPTY_JSON_OBJECT;
	//}
	//
	//private JsonObject login(final String usr, final String pwd) {
	//	return Json.createObjectBuilder()
	//			.add("usr", usr)
	//			.add("pwd", pwd)
	//			.build();
	//}
	//
	//private JsonObject login(final String usr, final String old, final String neu) {
	//	return Json.createObjectBuilder()
	//			.add("usr", usr)
	//			.add("old", old)
	//			.add("new", neu)
	//			.build();
	//
	//}
	//
	//private JsonObject logout() {
	//	return Json.createObjectBuilder()
	//			.add("usr", "")
	//			.add("pwd", "")
	//			.build();
	//}
	//
	//
	////// Anonymous /////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test void testAnonymousRefresh() {
	//	session()
	//
	//			.handle(anonymous().body(JSON.Format, refresh()))
	//
	//			.accept(response -> {
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//				Assertions.assertEquals("empty ticket", Json.createObjectBuilder().build(), response.body(JSON.Format));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAnonymousLogout() {
	//	session()
	//
	//			.handle(anonymous()
	//					.body(JSON.Format, logout()))
	//
	//			.accept(response -> {
	//
	//				Assert.assertEquals("success reported", Response.OK, response.status());
	//				Assert.assertEquals("empty ticket", JsonValue.EMPTY_JSON_OBJECT, response.body(JSON.Format));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAnonymousLogin() {
	//	session()
	//
	//			.handle(anonymous()
	//					.body(JSON.Format, login(RosterTest.This, RosterTest.This)))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//				Assertions.assertTrue("ticket detailed", json instanceof Map);
	//
	//				final Map<String, Object> ticket=(Map<String, Object>)json;
	//
	//				Assertions.assertTrue("label provided", ticket.get("label") instanceof String);
	//				Assertions.assertTrue("roles provided", ticket.get("roles") instanceof List);
	//				Assertions.assertTrue("token provided", ticket.get("token") instanceof String);
	//				Assertions.assertTrue("lease defined", ticket.get("lease") instanceof Number);
	//
	//				final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);
	//
	//				Assertions.assertTrue("user logged in", permit.valid(currentTimeMillis()));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAnonymousLoginBadCredentials() {
	//	session()
	//
	//			.handle(anonymous()
	//					.body(JSON.Format, login(RosterTest.This, "qwertyuiop")))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("error reported", Response.Forbidden, response.status());
	//				Assertions.assertTrue("error detailed", json instanceof Map);
	//
	//				final Map<String, Object> report=(Map<String, Object>)json;
	//
	//				Assertions.assertEquals("error defined", CredentialsRejected, report.get("error"));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAnonymousActivate() {
	//	session()
	//
	//			.handle(anonymous()
	//					.body(JSON.Format, login(RosterTest.This, RosterTest.This, "qwertyuiop")))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//				Assertions.assertTrue("ticket detailed", json instanceof Map);
	//
	//				final Map<String, Object> ticket=(Map<String, Object>)json;
	//
	//				Assertions.assertTrue("label provided", ticket.get("label") instanceof String);
	//				Assertions.assertTrue("roles provided", ticket.get("roles") instanceof List);
	//				Assertions.assertTrue("token provided", ticket.get("token") instanceof String);
	//				Assertions.assertTrue("lease defined", ticket.get("lease") instanceof Number);
	//
	//				final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);
	//
	//				Assertions.assertTrue("new user logged in", permit.valid(currentTimeMillis()));
	//				Assertions.assertEquals("password updated", "qwertyuiop", permit.token());
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAnonymousActivateBadCredentials() {
	//	session()
	//
	//			.handle(anonymous()
	//					.body(JSON.Format, login(RosterTest.This, "qwertyuiop", "asdfghjkl")))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("error reported", Response.Forbidden, response.status());
	//				Assertions.assertTrue("error detailed", json instanceof Map);
	//
	//				final Map<String, Object> report=(Map<String, Object>)json;
	//
	//				Assertions.assertEquals("error defined", CredentialsRejected, report.get("error"));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//
	////// Authenticated /////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test void testAuthenticatedRefresh() {
	//	session()
	//
	//			.handle(authenticated(request)
	//					.body(JSON.Format, refresh()))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//				Assertions.assertTrue("ticket detailed", json instanceof Map);
	//
	//				final Map<String, Object> ticket=(Map<String, Object>)json;
	//
	//				Assertions.assertTrue("label provided", ticket.get("label") instanceof String);
	//				Assertions.assertTrue("roles provided", ticket.get("roles") instanceof List);
	//				Assertions.assertTrue("token provided", ticket.get("token") instanceof String);
	//				Assertions.assertTrue("lease defined", ticket.get("lease") instanceof Number);
	//
	//				final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);
	//
	//				Assertions.assertTrue("user logged in", permit.valid(currentTimeMillis()));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAuthenticatedLogout() {
	//	session()
	//
	//			.handle(authenticated(request)
	//					.body(JSON.Format, logout()))
	//
	//			.accept(response -> {
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//				Assertions.assertEquals("empty ticket", JsonValue.EMPTY_JSON_OBJECT, response.body(JSON.Format));
	//
	//				final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);
	//
	//				Assertions.assertFalse("user logged out", permit.valid(currentTimeMillis()));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAuthenticatedLoginAgain() {
	//	session()
	//
	//			.handle(authenticated(request)
	//					.body(JSON.Format, login(RosterTest.This, RosterTest.This)))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//				Assertions.assertTrue("ticket detailed", json instanceof Map);
	//
	//				final Map<String, Object> ticket=(Map<String, Object>)json;
	//
	//				Assertions.assertTrue("label provided", ticket.get("label") instanceof String);
	//				Assertions.assertTrue("roles provided", ticket.get("roles") instanceof List);
	//				Assertions.assertTrue("token provided", ticket.get("token") instanceof String);
	//				Assertions.assertTrue("lease defined", ticket.get("lease") instanceof Number);
	//
	//				final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);
	//
	//				Assertions.assertTrue("user logged in", permit.valid(currentTimeMillis()));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAuthenticatedLoginAgainBadCredentials() {
	//	session()
	//
	//			.handle(authenticated(request)
	//					.body(JSON.Format, login(RosterTest.This, "qwertyuiop")))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("error reported", Response.Forbidden, response.status());
	//				Assertions.assertTrue("error detailed", json instanceof Map);
	//
	//				final Map<String, Object> report=(Map<String, Object>)json;
	//
	//				Assertions.assertEquals("error defined", CredentialsRejected, report.get("error"));
	//
	//				final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);
	//
	//				Assertions.assertTrue("user logged in", permit.valid(currentTimeMillis()));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAuthenticatedChangePassword() {
	//	session()
	//
	//			.handle(authenticated(request)
	//					.body(JSON.Format, login(RosterTest.This, RosterTest.This, "qwertyuiop")))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//				Assertions.assertTrue("ticket detailed", json instanceof Map);
	//
	//				final Map<String, Object> ticket=(Map<String, Object>)json;
	//
	//				Assertions.assertTrue("label provided", ticket.get("label") instanceof String);
	//				Assertions.assertTrue("roles provided", ticket.get("roles") instanceof List);
	//				Assertions.assertTrue("token provided", ticket.get("token") instanceof String);
	//				Assertions.assertTrue("lease defined", ticket.get("lease") instanceof Number);
	//
	//				final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);
	//
	//				Assertions.assertTrue("user logged in", permit.valid(currentTimeMillis()));
	//				Assertions.assertEquals("password updated", "qwertyuiop", permit.token());
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAuthenticatedChangePasswordBadCredentials() {
	//	session()
	//
	//			.handle(authenticated(request)
	//					.body(JSON.Format, login(RosterTest.This, "qwertyuiop", "asdfghjkl")))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("error reported", Response.Forbidden, response.status());
	//				Assertions.assertTrue("error detailed", json instanceof Map);
	//
	//				final Map<String, Object> report=(Map<String, Object>)json;
	//
	//				Assertions.assertEquals("error defined", CredentialsRejected, report.get("error"));
	//
	//				final Roster.Permit permit=tool(Roster.Factory).profile(RosterTest.This);
	//
	//				Assertions.assertTrue("user logged in", permit.valid(currentTimeMillis()));
	//				Assertions.assertEquals("password not updated", RosterTest.This, permit.token());
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//
	////// Authenticated/Switching ///////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test void testAuthenticatedSwitch() {
	//	session()
	//
	//			.handle(authenticated(request)
	//					.body(JSON.Format, login(RosterTest.That, RosterTest.That)))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//				Assertions.assertTrue("ticket detailed", json instanceof Map);
	//
	//				final Map<String, Object> ticket=(Map<String, Object>)json;
	//
	//				Assertions.assertTrue("label provided", ticket.get("label") instanceof String);
	//				Assertions.assertTrue("roles provided", ticket.get("roles") instanceof List);
	//				Assertions.assertTrue("token provided", ticket.get("token") instanceof String);
	//				Assertions.assertTrue("lease defined", ticket.get("lease") instanceof Number);
	//
	//				final Roster.Permit original=tool(Roster.Factory).profile(RosterTest.This);
	//				final Roster.Permit switched=tool(Roster.Factory).profile(RosterTest.That);
	//
	//				Assertions.assertFalse("old user logged out", original.valid(currentTimeMillis()));
	//				Assertions.assertTrue("new user logged in", switched.valid(currentTimeMillis()));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAuthenticatedSwitchBadCredentials() {
	//	session()
	//
	//			.handle(anonymous()
	//					.body(JSON.Format, login(RosterTest.That, "qwertyuiop")))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("error reported", Response.Forbidden, response.status());
	//				Assertions.assertTrue("error detailed", json instanceof Map);
	//
	//				final Map<String, Object> report=(Map<String, Object>)json;
	//
	//				Assertions.assertEquals("error defined", CredentialsRejected, report.get("error"));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//@Test void testAuthenticatedSwitchAndActivate() {
	//	session()
	//
	//			.handle(anonymous()
	//					.body(JSON.Format, login(RosterTest.That, RosterTest.That, "qwertyuiop")))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//				Assertions.assertTrue("ticket detailed", json instanceof Map);
	//
	//				final Map<String, Object> ticket=(Map<String, Object>)json;
	//
	//				Assertions.assertTrue("label provided", ticket.get("label") instanceof String);
	//				Assertions.assertTrue("roles provided", ticket.get("roles") instanceof List);
	//				Assertions.assertTrue("token provided", ticket.get("token") instanceof String);
	//				Assertions.assertTrue("lease defined", ticket.get("lease") instanceof Number);
	//
	//				final Roster.Permit original=tool(Roster.Factory).profile(RosterTest.This);
	//				final Roster.Permit switched=tool(Roster.Factory).profile(RosterTest.That);
	//
	//				Assertions.assertFalse("old user logged out", original.valid(currentTimeMillis()));
	//				Assertions.assertTrue("new user logged in", switched.valid(currentTimeMillis()));
	//				Assertions.assertEquals("password updated", "qwertyuiop", switched.token());
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//
	//@Test void testAuthenticatedSwitchAndActivateBadCredentials() {
	//	session()
	//
	//			.handle(anonymous()
	//					.body(JSON.Format, login(RosterTest.This, "qwertyuiop", "asdfghjkl")))
	//
	//			.accept(response -> {
	//
	//				final Object json=response.body(JSON.Format);
	//
	//				Assertions.assertEquals("error reported", Response.Forbidden, response.status());
	//				Assertions.assertTrue("error detailed", json instanceof Map);
	//
	//				final Map<String, Object> report=(Map<String, Object>)json;
	//
	//				Assertions.assertEquals("error defined", CredentialsRejected, report.get("error"));
	//
	//				tool(Trace.Factory).info("sample request", response.request().text());
	//				tool(Trace.Factory).info("sample response", response.text());
	//
	//			});
	//}
	//
	//
	////// Common ////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test void testMalformed() {
	//	session()
	//
	//			.handle(anonymous()
	//					.body(JSON.Format, Json.createObjectBuilder().add("other", 123).build()))
	//
	//			.accept(response ->
	//					Assertions.assertEquals(Response.BadRequest, response.status())
	//			);
	//}
	//
}
