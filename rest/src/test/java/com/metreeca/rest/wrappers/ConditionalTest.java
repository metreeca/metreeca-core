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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.Wrapper;
import com.metreeca.tray.Tray;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;


final class ConditionalTest {

	private static final int HandlerStatus=Response.OK;
	private static final int WrapperStatus=Response.Accepted;

	private static final Handler Handler=request ->
			request.reply(response -> response.status(HandlerStatus));

	private static final Wrapper Wrapper=handler -> request ->
			handler.handle(request).map(response -> response.status(WrapperStatus));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDelegateHandler() {
		new Tray()

				.get(() -> new Conditional()
						.test(request -> true)
						.wrap(Handler))

				.handle(new Request().method(Request.GET))

				.accept(reader -> assertEquals(HandlerStatus, reader.status(), "handler delegated"));
	}

	@Test void testBypassHandler() {
		new Tray()

				.get(() -> new Conditional()
						.test(request -> false)
						.wrap(Handler))

				.handle(new Request().method(Request.GET))

				.accept(reader -> assertEquals(0, reader.status(), "handler bypassed"));
	}

	@Test void testDelegateWrapper() {
		new Tray()

				.get(() -> new Conditional()
						.test(request -> true)
						.wrap(Wrapper)
						.wrap(Handler))

				.handle(new Request().method(Request.GET))

				.accept(reader -> assertEquals(WrapperStatus, reader.status(), "wrapper delegated"));
	}

	@Test void testBypassWrapper() {
		new Tray()

				.get(() -> new Conditional()
						.test(request -> false)
						.wrap(Wrapper)
						.wrap(Handler))

				.handle(new Request().method(Request.GET))

				.accept(reader -> assertEquals(HandlerStatus, reader.status(), "wrapper bypassed"));
	}

}
