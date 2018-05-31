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

package com.metreeca.link;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

import static com.metreeca.link.Request.GET;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.Arrays.asList;


public final class HandlerTest {

	@Test public void testExecute() {

		final AtomicBoolean committed=new AtomicBoolean();

		final Handler handler=(request, response) -> {
			try {
				response.status(Response.OK).done();
			} finally {
				committed.set(true);
			}
		};

		handler.exec(writer -> writer.method(GET).done(), reader ->
				assertEquals("request/response paired", GET, reader.request().method()));

		assertTrue("target invoked", committed.get());

	}

	@Test public void testBefore() {

		final AtomicBoolean committed=new AtomicBoolean();

		final Handler handler=(request, response) -> {
			try {
				response.status(Response.OK).text("handler/"+request.text());
			} finally {
				committed.set(true);
			}
		};

		handler

				.wrap(wrapped -> (request, response) -> wrapped.exec(
						writer -> writer.copy(request).text("before/"+request.text()),
						reader -> response.copy(reader).done()
				))

				.exec(
						writer -> writer.method(GET).text("text"),
						reader -> assertEquals("wrapped", "handler/before/text", reader.text())
				);

		assertTrue("target invoked", committed.get());

	}

	@Test public void testAfter() {

		final AtomicBoolean committed=new AtomicBoolean();

		final Handler handler=(request, response) -> {
			try {
				response.status(Response.OK).text("handler/"+request.text());
			} finally {
				committed.set(true);
			}
		};

		handler

				.wrap(wrapped -> (request, response) -> wrapped.exec(
						writer -> writer.copy(request).done(),
						reader -> response.copy(reader).text("after/"+reader.text())
				))

				.exec(
						writer -> writer.method(GET).text("text"),
						reader -> assertEquals("wrapped", "after/handler/text", reader.text())
				);

		assertTrue("target invoked", committed.get());

	}

	@Test public void testReaderPairing() {

		final Handler handler=(request, response) -> response.status(Response.OK).done();

		handler

				.wrap(wrapped -> (request, response) -> wrapped.exec(

						writer -> writer.copy(request).user(RDF.REST).done(),

						reader -> {

							assertEquals("paired with wrapped request", RDF.REST, reader.request().user());

							response.copy(reader).done();

						}

				))

				.exec(
						writer -> writer.method(GET).user(RDF.FIRST).done(),
						reader -> assertEquals("paired with original request", RDF.FIRST, reader.request().user())
				);
	}

	@Test public void testResultStreaming() {

		final List<String> transaction=new ArrayList<>();

		final Handler handler=(request, response) -> {

			transaction.add("begin");

			response.status(Response.OK).text("inside");

			transaction.add("commit");

		};

		handler.exec(writer -> writer.method(GET).done(), reader -> transaction.add(reader.text()));

		assertEquals("", asList("begin", "inside", "commit"), transaction);
	}

}
