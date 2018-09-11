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

package com.metreeca.next.handlers;


import com.metreeca.next.Request;
import com.metreeca.next.Response;

import org.junit.jupiter.api.Test;

import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static java.util.Arrays.asList;


final class DispatcherTest {

	@Test void testHandleOPTIONSByDefault() {
		new Dispatcher()

				.get((request) -> request.response().status(Response.OK))

				.handle(new Request().method(Request.POST))

				.accept(response -> {

					assertEquals(Response.MethodNotAllowed, response.status(), "error reported");

					assertEquals(new HashSet<>(asList(Request.OPTIONS, Request.GET)),
							new HashSet<>(response.headers("Allow")),
							"allowed method reported");

				});
	}

}
