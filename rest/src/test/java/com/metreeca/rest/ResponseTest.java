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

package com.metreeca.rest;

import org.junit.Test;

import java.util.function.Consumer;

import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.RewriterTest.External;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;


public class ResponseTest {

	private void response(final Consumer<Response> task, final Consumer<Response.Reader> target) {
		new Request.Writer(request -> task.accept(new Response(request, target)))
				.method(Request.GET)
				.base(External)
				.done();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testTraceCommitmentState() {
		response(response -> {

			assertFalse("initially uncommitted", response.committed());
			assertTrue("finally committed", response.status(OK).text("").committed());

		}, reader -> {});
	}


	@Test(expected=IllegalStateException.class) public void testPreventCommitWithoutCode() {
		response(Response::done, reader -> {});
	}

	@Test(expected=IllegalStateException.class) public void testPreventDoubleCommit() {
		response(response -> {

			response.status(OK).done();
			response.done();

		}, reader -> {});
	}


	@Test public void testHeaderOverwriting() {
		response(response -> response.status(OK)

						.header("test", "uno")
						.header("test", "due")
						.header("test", "tre")

						.done(),

				reader ->

						assertEquals("header overwritten", singletonList("tre"), reader.headers("test")));

	}

	@Test public void testHeaderMerging() {
		response(response -> response.status(OK)

						.header("test", "", "uno")
						.header("test", "", "due")
						.header("test", "", "tre")

						.done(),

				reader ->

						assertEquals("header merged", asList("uno", "due", "tre"), reader.headers("test")));
	}

	@Test public void testHeaderClearing() {
		response(response -> response.status(OK)

						.header("test", "", "uno")
						.header("test", "", "due")
						.header("test")

						.done(),

				reader ->

						assertEquals("header cleared", emptyList(), reader.headers("test")));
	}

}
