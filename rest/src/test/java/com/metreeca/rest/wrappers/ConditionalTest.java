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

import com.metreeca.rest.*;

import org.junit.Test;

import static com.metreeca.rest.LinkTest.testbed;

import static org.junit.Assert.assertEquals;


public final class ConditionalTest {

	private static final int HandlerStatus=Response.OK;
	private static final int WrapperStatus=Response.Accepted;

	private static final Handler Handler=(request, response) -> response.status(HandlerStatus).done();

	private static final Wrapper Wrapper=handler -> (request, response) -> handler.handle(
			writer -> writer.copy(request).done(),
			reader -> response.copy(reader).status(WrapperStatus).done()
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testDelegateHandlerUnconditonally() {
		testbed()

				.handler(() -> new Conditional()
						.test(request -> false)
						.wrap(Handler))

				.request(writer -> writer.method(Request.GET).done())

				.response(reader -> assertEquals("handler delegated", HandlerStatus, reader.status()));
	}

	@Test public void testDelegateWrapper() {
		testbed()

				.handler(() -> new Conditional()
						.test(request -> true)
						.wrap(Wrapper)
						.wrap(Handler))

				.request(writer -> writer.method(Request.GET).done())

				.response(reader -> assertEquals("wrapper delegated", WrapperStatus, reader.status()));
	}

	@Test public void testBypassWrapper() {
		testbed()

				.handler(() -> new Conditional()
						.test(request -> false)
						.wrap(Wrapper)
						.wrap(Handler))

				.request(writer -> writer.method(Request.GET).done())

				.response(reader -> assertEquals("wrapper bypassed", HandlerStatus, reader.status()));
	}

}
