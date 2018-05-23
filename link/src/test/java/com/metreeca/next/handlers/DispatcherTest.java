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

import com.metreeca.link.Request;
import com.metreeca.link.Response;

import org.junit.Test;

import java.util.HashSet;

import static com.metreeca.link.LinkTest.testbed;
import static com.metreeca.next.handlers.Dispatcher.dispatcher;

import static org.junit.Assert.assertEquals;

import static java.util.Arrays.asList;


public class DispatcherTest {

	@Test public void testHandleOPTIONSByDefault() {
		testbed()

				.handler(() -> dispatcher().get((request, response) -> response.status(Response.OK).done()))

				.request(request -> request
						.method(Request.POST)
						.done())

				.response(response -> {

					assertEquals("error reported", Response.MethodNotAllowed, response.status());

					assertEquals("allowed method reported",
							new HashSet<>(asList(Request.OPTIONS, Request.GET)),
							new HashSet<>(response.headers("Allow")));

				});
	}

}
