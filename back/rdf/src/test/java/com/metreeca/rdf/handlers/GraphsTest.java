/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rdf.handlers;

import com.metreeca.rdf.ValuesTest;
import com.metreeca.rdf._Form;
import com.metreeca.rdf.services.Graph;
import com.metreeca.rdf.services.GraphTest;
import com.metreeca.rest.Context;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats.InputFormat;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;

import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.statement;
import static com.metreeca.rdf.ValuesTest.encode;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf.services.GraphTest.export;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.ResponseAssert.assertThat;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;


final class GraphsTest {

	private static final Statement First=statement(RDF.NIL, RDF.VALUE, RDF.FIRST);
	private static final Statement Rest=statement(RDF.NIL, RDF.VALUE, RDF.REST);


	private Context with(final Runnable... datasets) {

		final Context tray=new Context().set(Graph.graph(), GraphTest::graph);

		for (final Runnable dataset : datasets) {
			tray.exec(dataset);
		}

		return tray;
	}


	private Model catalog() {
		return new LinkedHashModel(asList(
				statement(iri(ValuesTest.Base), RDF.VALUE, RDF.NIL),
				statement(RDF.NIL, RDF.TYPE, VOID.DATASET)
		));
	}

	private Model dflt() {
		return service(Graph.graph()).exec(connection -> {

			return export(connection, (Resource)null);

		});
	}

	private Model named() {
		return service(Graph.graph()).exec(connection -> {

			return export(connection, RDF.NIL).stream()
					.map(s -> statement(s.getSubject(), s.getPredicate(), s.getObject())) // strip context info
					.collect(toCollection(LinkedHashModel::new));

		});
	}


	private Model model(final Statement... model) {
		return new LinkedHashModel(asList(model));
	}

	private Runnable named(final Statement... model) {
		return GraphTest.model(asList(model), RDF.NIL);
	}

	private Runnable dflt(final Statement... model) {
		return GraphTest.model(asList(model), (Resource)null);
	}


	private Graphs endpoint() {
		return new Graphs();
	}

	private Request request() {
		return new Request().base(ValuesTest.Base);
	}


	private Graphs _private(final Graphs endpoint) {
		return endpoint;
	}

	private Graphs _public(final Graphs endpoint) {
		return endpoint.query();
	}


	private Request anonymous(final Request request) {
		return request;
	}

	private Request authenticated(final Request request) {
		return request.roles(_Form.root);
	}


	private Request catalog(final Request request) {
		return request.method(Request.GET);
	}

	private Request get(final Request request) {
		return request.method(Request.GET);
	}

	private Request put(final Request request) {
		return request.method(Request.PUT).body(InputFormat.input(), () ->
				new ByteArrayInputStream(encode(model(Rest)).getBytes(UTF_8))
		);
	}

	private Request delete(final Request request) {
		return request.method(Request.DELETE);
	}

	private Request post(final Request request) {
		return request.method(Request.POST).body(InputFormat.input(), () ->
				new ByteArrayInputStream(encode(model(Rest)).getBytes(UTF_8))
		);
	}


	private Request dflt(final Request request) {
		return request.parameter("default", "");
	}

	private Request named(final Request request) {
		return request.parameter("graph", RDF.NIL.stringValue());
	}


	//// Catalog ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGETCatalogPrivateAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(anonymous(catalog(request())))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

				}));
	}

	@Test void testGETCatalogPrivateAuthorized() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(authenticated(catalog(request())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(catalog())
						)));
	}

	@Test void testGETCatalogPublicAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(anonymous(catalog(request())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(catalog())
						)));
	}

	@Test void testGETCatalogPublicAuthorized() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(authenticated(catalog(request())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(catalog())
						)));
	}


	//// GET ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGETDefaultPrivateAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(anonymous(dflt(get(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

				}));
	}

	@Test void testGETDefaultPrivateAuthenticated() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(authenticated(dflt(get(request()))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(First)
						)
				)
		);
	}

	@Test void testGETDefaultPublicAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(anonymous(dflt(get(request()))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(First)
						)));
	}

	@Test void testGETDefaultPublicAuthenticated() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(authenticated(dflt(get(request()))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(First)
						)
				)
		);
	}


	@Test void testGETNamedPrivateAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(anonymous(named(get(request()))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()));
	}

	@Test void testGETNamedPrivateAuthenticated() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(authenticated(named(get(request()))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(Rest)
						)));
	}

	@Test void testGETNamedPublicAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(anonymous(named(get(request()))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(Rest)
						)));
	}

	@Test void testGETNamedPublicAuthenticated() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(authenticated(named(get(request()))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf)
								.isIsomorphicTo(Rest)
						)));
	}


	//// PUT ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPUTDefaultPrivateAnonymous() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(anonymous(dflt(put(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testPUTDefaultPrivateAuthenticated() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(authenticated(dflt(put(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(Rest);

				}));
	}

	@Test void testPUTDefaultPublicAnonymous() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(anonymous(dflt(put(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testPUTDefaultPublicAuthenticated() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(authenticated(dflt(put(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(Rest);

				}));
	}


	@Test void testPUTNamedPrivateAnonymous() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(anonymous(named(put(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testPUTNamedPrivateAuthenticated() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(authenticated(named(put(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(Rest);

				}));
	}

	@Test void testPUTNamedPublicAnonymous() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(anonymous(named(put(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testPUTNamedPublicAuthenticated() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(authenticated(named(put(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(Rest);

				}));
	}


	//// DELETE ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDELETEDefaultPrivateAnonymous() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(anonymous(dflt(delete(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testDELETEDefaultPrivateAuthenticated() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(authenticated(dflt(delete(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(dflt())
							.isEmpty();

				}));
	}

	@Test void testDELETEDefaultPublicAnonymous() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(anonymous(dflt(delete(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testDELETEDefaultPublicAuthenticated() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(authenticated(dflt(delete(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(dflt())
							.isEmpty();

				}));
	}


	@Test void testDELETENamedPrivateAnonymous() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(anonymous(named(delete(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testDELETENamedPrivateAuthenticated() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(authenticated(named(delete(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();
					assertThat(named()).isEmpty();

				}));
	}

	@Test void testDELETENamedPublicAnonymous() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(anonymous(named(delete(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testDELETENamedPublicAuthenticated() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(authenticated(named(delete(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();
					assertThat(named()).isEmpty();

				}));
	}


	//// POST ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPOSTDefaultPrivateAnonymous() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(anonymous(dflt(post(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testPOSTDefaultPrivateAuthenticated() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(authenticated(dflt(post(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(model(First, Rest));

				}));
	}

	@Test void testPOSTDefaultPublicAnonymous() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(anonymous(dflt(post(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testPOSTDefaultPublicAuthenticated() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(authenticated(dflt(post(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(dflt())
							.isIsomorphicTo(model(First, Rest));

				}));
	}


	@Test void testPOSTNamedPrivateAnonymous() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(anonymous(named(post(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testPOSTNamedPrivateAuthenticated() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(authenticated(named(post(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(model(First, Rest));

				}));
	}

	@Test void testPOSTNamedPublicAnonymous() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(anonymous(named(post(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.Unauthorized)
							.doesNotHaveBody();

					assertThat(named())
							.isIsomorphicTo(First);

				}));
	}

	@Test void testPOSTNamedPublicAuthenticated() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(authenticated(named(post(request()))))

				.accept(response -> {

					assertThat(response)
							.isSuccess()
							.doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(model(First, Rest));

				}));
	}

}
