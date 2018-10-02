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

package com.metreeca.rest.handlers.sparql;

import com.metreeca.form.Form;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;

import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.RestTest.dataset;

import static java.util.Collections.singleton;


final class SPARQLTest {

	private static final Set<Statement> First=singleton(statement(RDF.NIL, RDF.VALUE, RDF.FIRST));
	private static final Set<Statement> Rest=singleton(statement(RDF.NIL, RDF.VALUE, RDF.REST));


	@SafeVarargs private final Tray with(final Collection<Statement>... datasets) {

		final Tray tray=new Tray();

		for (final Collection<Statement> dataset : datasets) {
			tray.exec(dataset(dataset, RDF.NIL));
		}

		return tray;
	}


	private SPARQL endpoint() {
		return new SPARQL();
	}

	private Request request() {
		return new Request().base(ValuesTest.Base);
	}


	private SPARQL _private(final SPARQL endpoint) {
		return endpoint;
	}

	private SPARQL _public(final SPARQL endpoint) {
		return endpoint.publik(true);
	}


	private Request anonymous(final Request request) {
		return request;
	}

	private Request authenticated(final Request request) {
		return request.roles(Form.root);
	}


	private Request get(final Request request) {
		return request.method(Request.GET);
	}

	private Request post(final Request request) {
		return request.method(Request.POST);
	}


	private Request query() {
		return new Request()
				.header("Accept", "application/json")
				.parameters("query", "select ?o { ?s ?p ?o }");
	}


	//// Query /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGETQueryPrivateAnonymous() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(get(anonymous(query())))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).hasEmptyBody();

				}));
	}

	@Test void testPOSTQueryPrivateAnonymous() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(post(anonymous(query())))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).hasEmptyBody();

				}));
	}

	//@Test void testGETQueryPrivateAuthenticated() {
	//	with(First, Rest).exec(() -> _private(endpoint())
	//
	//			.handle(get(authenticated(query())))
	//
	//			.accept(response -> {
	//
	//				assertThat(response).hasStatus(Response.OK);
	//
	//				System.out.println(response
	//
	//						.body(reader()).set(new Supplier<Reader>() {
	//							@Override public Reader get() {
	//								return () -> Transputs.reader(source.get());
	//							}
	//						})
	//						.body(json()).get().orElseGet(() -> fail("missing body"))
	//
	//
	//				);
	//
	//			}));
	//}

}
