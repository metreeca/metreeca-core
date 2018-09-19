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

package com.metreeca.next.handlers.shape;


import com.metreeca.form.things.ValuesTest;
import com.metreeca.next.Request;
import com.metreeca.next.formats._RDF;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.junit.jupiter.api.Test;

import static com.metreeca.tray.Tray.tool;


final class RelatorTest {

	//private Testbed testbed() {
	//	return LinkTest.testbed()
	//
	//			.dataset(ValuesTest.small())
	//
	//			.handler(() -> relator().shape(LinkTest.Employee));
	//}


	private Request request() {
		return new Request()
				.method(Request.GET)
				.base(ValuesTest.Base)
				.path("/employees/1370");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	@Test void test() { // !!! test CBD
		new Tray()

				.run(() -> tool(Graph.Factory).update(connection -> { connection.add(ValuesTest.small()); }))

				.get(Relator::new)

				.handle(request())

				.accept(response -> response.body(_RDF.Format).value(statements -> {

					System.out.println(statements);

					return null;

				}));
	}

	//@Test void testRelate() {
	//	testbed()
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(ValuesTest.Manager)
	//					.done())
	//
	//			.response(response -> {
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					final Model expected=ValuesTest.construct(connection,
	//							"construct where { <employees/1370> a :Employee; :code ?c; :seniority ?s }");
	//
	//					final Model actual=response.rdf();
	//
	//					ValuesTest.assertSubset("items retrieved", expected, actual);
	//
	//				}
	//
	//			});
	//}
	//
	//@Test void testRelateLimited() {
	//	testbed()
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(ValuesTest.Salesman)
	//					.done())
	//
	//			.response(response -> {
	//
	//				Assertions.assertEquals("success reported", Response.OK, response.status());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					final Model expected=ValuesTest.construct(connection,
	//							"construct where { <employees/1370> a :Employee; :code ?c }");
	//
	//					final Model actual=response.rdf();
	//
	//					ValuesTest.assertSubset("items retrieved", expected, actual);
	//
	//					Assertions.assertTrue("properties restricted to manager role not included",
	//							actual.filter(null, ValuesTest.term("seniority"), null).isEmpty());
	//
	//				}
	//
	//			});
	//}
	//
	//@Test void testRelatePiped() {
	//	testbed()
	//
	//			.handler(() -> relator().shape(ValuesTest.Employee)
	//
	//					.pipe((request, model) -> {
	//
	//						model.add(statement(request.focus(), RDF.VALUE, RDF.FIRST));
	//
	//						return model;
	//
	//					})
	//
	//					.pipe((request, model) -> {
	//
	//						model.add(statement(request.focus(), RDF.VALUE, RDF.REST));
	//
	//						return model;
	//
	//					}))
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(LinkTest.Manager)
	//					.done())
	//
	//			.response(response -> {
	//
	//				ValuesTest.assertSubset("items retrieved", asList(
	//
	//						statement(response.focus(), RDF.VALUE, RDF.FIRST),
	//						statement(response.focus(), RDF.VALUE, RDF.REST)
	//
	//				), response.rdf());
	//
	//			});
	//
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test void testUnauthorized() {
	//	testbed()
	//
	//			.request(request -> std(request)
	//					.user(Form.none)
	//					.done())
	//
	//			.response(response -> {
	//
	//				Assertions.assertEquals("error reported", Response.Unauthorized, response.status());
	//
	//			});
	//}
	//
	//@Test void testForbidden() {
	//	testbed()
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(RDF.FIRST, RDF.REST)
	//					.done())
	//
	//			.response(response -> {
	//
	//				Assertions.assertEquals("error reported", Response.Forbidden, response.status());
	//
	//			});
	//}
	//
	//@Test void testUnknown() {
	//	testbed()
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(ValuesTest.Salesman)
	//					.path("/employees/9999")
	//					.done())
	//
	//			.response(response -> {
	//
	//				Assertions.assertEquals("error reported", Response.NotFound, response.status());
	//
	//			});
	//}

}
