/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.actors;


public class _DeleterTest {

	//private Request.Writer std(final Request.Writer writer) {
	//	return writer
	//			.method(Request.DELETE)
	//			.path("/employees/1370");
	//}
	//
	//private Request.Writer delete(final Request.Writer request) {
	//	return std(request)
	//			.user(RDF.NIL)
	//			.roles(LinkTest.Manager)
	//			.done();
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test public void testDelete() {
	//	testbed().handler(() -> deleter(Employee))
	//
	//			.dataset(small())
	//
	//			.request(this::delete)
	//
	//			.response(response -> {
	//
	//				assertEquals("success reported", Response.NoContent, response.status());
	//				assertTrue("no details", response.text().isEmpty());
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					final Model model=construct(connection, "construct where { <employees/1370> ?p ?o }");
	//
	//					assertTrue("shape deleted", model.isEmpty());
	//
	//				}
	//
	//			});
	//}
	//
	//@Test public void testPostProcess() {
	//	testbed().handler(() -> server(deleter(Employee)
	//
	//			.wrap(processor().script(sparql("delete where { ?s ?p $this }")))))
	//
	//			.dataset(small())
	//
	//			.request(this::delete)
	//
	//			.response(response -> {
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//
	//					assertFalse("graph post-processed", connection.hasStatement(
	//							null, null, response.focus(), true
	//					));
	//
	//				}
	//			});
	//}
	//
	//@Test public void testPostProcessInsideTXN() {
	//	testbed().handler(() -> server(deleter(Employee)
	//
	//			.wrap(handler -> (request, response) -> {
	//				throw new RuntimeException("abort");  // should cause txn rollback
	//			})))
	//
	//			.dataset(small())
	//
	//			.request(this::delete)
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.InternalServerError, response.status());
	//				assertTrue("error detailed", response.json() instanceof Map);
	//
	//				try (final RepositoryConnection connection=tool(Graph.Factory).connect()) {
	//					assertIsomorphic("graph unchanged", export(connection), small());
	//				}
	//
	//			});
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Test public void testUnauthorized() {
	//	testbed().handler(() -> deleter(Employee))
	//
	//			.dataset(small())
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(LinkTest.Salesman)
	//					.done())
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.Forbidden, response.status());
	//
	//			});
	//}
	//
	//@Test public void testForbidden() {
	//	testbed().handler(() -> deleter(Employee))
	//
	//			.request(request -> std(request)
	//					.user(RDF.NIL)
	//					.roles(RDF.FIRST, RDF.REST)
	//					.done())
	//
	//			.response(response -> {
	//
	//				assertEquals("error reported", Response.Forbidden, response.status());
	//
	//			});
	//}

}
