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

package com.metreeca.next;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static java.util.Arrays.asList;


final class HandlerTest {

	//@Test public void testExecute() {
	//
	//	final AtomicBoolean committed=new AtomicBoolean();
	//
	//	final Handler handler=(request, response) -> {
	//		try {
	//			response.status(Response.OK).done();
	//		} finally {
	//			committed.set(true);
	//		}
	//	};
	//
	//	handler.handle(writer -> writer.method(Request.GET).done(), reader ->
	//			Assert.assertEquals("request/response paired", Request.GET, reader.request().method()));
	//
	//	Assert.assertTrue("target invoked", committed.get());
	//
	//}

	//@Test public void testBefore() {
	//
	//	final AtomicBoolean committed=new AtomicBoolean();
	//
	//	final Handler handler=(request, response) -> {
	//		try {
	//			response.status(Response.OK).text("handler/"+request.text());
	//		} finally {
	//			committed.set(true);
	//		}
	//	};
	//
	//	handler
	//
	//			.wrap(wrapped -> (request, response) -> wrapped.handle(
	//					writer -> writer.copy(request).text("before/"+request.text()),
	//					reader -> response.copy(reader).done()
	//			))
	//
	//			.handle(
	//					writer -> writer.method(Request.GET).text("text"),
	//					reader -> Assert.assertEquals("wrapped", "handler/before/text", reader.text())
	//			);
	//
	//	Assert.assertTrue("target invoked", committed.get());
	//
	//}

	//@Test public void testAfter() {
	//
	//	final AtomicBoolean committed=new AtomicBoolean();
	//
	//	final Handler handler=(request, response) -> {
	//		try {
	//			response.status(Response.OK).text("handler/"+request.text());
	//		} finally {
	//			committed.set(true);
	//		}
	//	};
	//
	//	handler
	//
	//			.wrap(wrapped -> (request, response) -> wrapped.handle(
	//					writer -> writer.copy(request).done(),
	//					reader -> response.copy(reader).text("after/"+reader.text())
	//			))
	//
	//			.handle(
	//					writer -> writer.method(Request.GET).text("text"),
	//					reader -> Assert.assertEquals("wrapped", "after/handler/text", reader.text())
	//			);
	//
	//	Assert.assertTrue("target invoked", committed.get());
	//
	//}

	@Test void testResultStreaming() {

		final List<String> transaction=new ArrayList<>();

		final Handler handler=request -> consumer -> {

			transaction.add("begin");

			consumer.accept(request.response().status(Response.OK)/* !!! .text("inside")*/);

			transaction.add("commit");

		};

		handler.handle(new Request()).accept(response -> transaction.add(String.valueOf(response.status()) /* !!! response.text()*/));

		assertEquals(asList("begin", "200" /* !!! "inside"*/, "commit"), transaction);
	}

}
