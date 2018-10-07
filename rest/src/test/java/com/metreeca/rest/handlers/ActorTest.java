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

package com.metreeca.rest.handlers;

import com.metreeca.form.Form;
import com.metreeca.form.truths.ModelAssert;
import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Shape.role;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.term;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.ShapeFormat.shape;

import static java.util.Collections.emptyList;
import static java.util.Collections.singleton;


final class ActorTest {

	private void exec(final Runnable task) {
		new Tray().exec(task).clear();
	}


	//// Direct ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDirectEnforceRoleBasedAccessControl() {
		exec(() -> access(true, RDF.FIRST).accept(response -> assertThat(response).hasStatus(Response.OK)));
		exec(() -> access(true, RDF.FIRST, RDF.FIRST).accept(response -> assertThat(response).hasStatus(Response.OK)));
		exec(() -> access(true, RDF.REST, RDF.FIRST).accept(response -> assertThat(response).hasStatus(Response.Unauthorized)));
	}


	//// Driven ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDrivenEnforceRoleBasedAccessControl() {
		exec(() -> access(false, RDF.FIRST).accept(response -> assertThat(response).hasStatus(Response.OK)));
		exec(() -> access(false, RDF.FIRST, RDF.FIRST).accept(response -> assertThat(response).hasStatus(Response.OK)));
		exec(() -> access(false, RDF.REST, RDF.FIRST).accept(response -> assertThat(response).hasStatus(Response.Unauthorized)));
	}


	//// Shared ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Disabled  @Test void testDelegateProcessor() {
		exec(() -> new TestActor()

				.pre((request, statements) -> {

					statements.add(statement(request.item(), RDF.VALUE, RDF.FIRST));

					return statements;
				})

				.post((request, statements) -> {

					statements.add(statement(request.item(), RDF.VALUE, RDF.REST));

					return statements;
				})

				.update("insert { ?this rdf:value rdf:nil } where {}")

				.handle(new Request().body(rdf()).set(emptyList())) // enable rdf pre-processing

				.accept(response -> {

					assertThat(response)
							.as("pre/post-processing filters executed")
							.hasBodyThat(rdf())
							.isIsomorphicTo(
									statement(response.item(), RDF.VALUE, RDF.FIRST), // pre-processor
									statement(response.item(), RDF.VALUE, RDF.REST) // post-processor
							);

					ModelAssert.assertThat(graph())
							.as("update script executed")
							.hasSubset(statement(response.item(), RDF.VALUE, RDF.NIL));

				}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder access(final boolean direct, final IRI effective, final IRI... permitted) {
		return new TestActor().roles(permitted).handle(new Request().roles(effective).map(request ->
				direct ? request : request.body(shape()).set(
						role(singleton(RDF.FIRST), clazz(term("Employee")))
				)
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestActor extends Actor<TestActor> {

		@Override public Responder handle(final Request request) {
			return handler(Form.relate, Form.detail, shape -> request.reply(response ->

					response.status(Response.OK)

							.map(r -> request.body(shape()).map( // echo shape body
									value -> r.body(shape()).set(value),
									error -> r
							))

							.map(r -> request.body(rdf()).map( // echo rdf body
									value -> r.body(rdf()).set(value),
									error -> r
							))

			)).handle(request);
		}

	}

}
