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

import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Statement;

import static com.metreeca.tray.Tray.tool;


public final class RestTest {

	public static Runnable dataset(final Iterable<Statement> model) {
		return () -> tool(Graph.Factory).update(connection -> { connection.add(model); });
	}


	//public Testbed exec(final Consumer<Request.Writer> request, final Consumer<Response.Reader> response) {
	//
	//	final AtomicBoolean invoked=new AtomicBoolean();
	//
	//	exec(() -> handler.get()
	//
	//			.wrap(parser()) // parse query string
	//			.wrap(persistor()) // make response body getters idempotent
	//
	//			.handle(
	//
	//					writer ->
	//
	//							request.accept(writer.base(ValuesTest.Base)),
	//
	//					reader -> {
	//
	//						invoked.set(true);
	//
	//						if ( reader.binary() ) {
	//
	//							final ByteArrayOutputStream buffer=new ByteArrayOutputStream();
	//
	//							reader.output(buffer);
	//
	//							if ( !reader.success() ) {
	//								Logger.getGlobal().severe("status code "+reader.status()
	//										+"\n\n"+buffer.toByteArray().length+" bytes");
	//							}
	//
	//						} else if ( reader.textual() ) {
	//
	//							final StringWriter buffer=new StringWriter();
	//
	//							reader.writer(buffer);
	//
	//							if ( !reader.success() ) {
	//								Logger.getGlobal().severe("status code "+reader.status()
	//										+"\n\n"+indent(buffer.toString().trim()));
	//							}
	//
	//						}
	//
	//						try {
	//							response.accept(reader);
	//						} catch ( final RuntimeException e ) {
	//							throw new AssertionError("exception in test code", e);
	//						}
	//
	//					}
	//
	//			));
	//
	//	if ( !invoked.get() ) {
	//		fail("no response");
	//	}
	//
	//	return this;
	//}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RestTest() {} // utility

}
