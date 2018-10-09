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
import com.metreeca.form.things.Codecs;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.Set;

import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.form.things.Sets.union;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.encode;
import static com.metreeca.form.things.ValuesTest.export;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toCollection;


final class GraphsTest {

	private static final Set<Statement> First=singleton(statement(RDF.NIL, RDF.VALUE, RDF.FIRST));
	private static final Set<Statement> Rest=singleton(statement(RDF.NIL, RDF.VALUE, RDF.REST));


	private Tray with(final Runnable... datasets) {

		final Tray tray=new Tray();

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
		return tool(Graph.Factory).query(connection -> {

			return export(connection, (Resource)null);

		});
	}

	private Model named() {
		return tool(Graph.Factory).query(connection -> {

			return export(connection, RDF.NIL).stream()
					.map(s -> statement(s.getSubject(), s.getPredicate(), s.getObject())) // strip context info
					.collect(toCollection(LinkedHashModel::new));

		});
	}


	private Runnable named(final Iterable<Statement> model) {
		return graph(model, RDF.NIL);
	}

	private Runnable dflt(final Iterable<Statement> model) {
		return graph(model, (Resource)null);
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
		return endpoint.publik(true);
	}


	private Request anonymous(final Request request) {
		return request;
	}

	private Request authenticated(final Request request) {
		return request.roles(Form.root);
	}


	private Request catalog(final Request request) {
		return request.method(Request.GET);
	}

	private Request get(final Request request) {
		return request.method(Request.GET);
	}

	private Request put(final Request request) {
		return request.method(Request.PUT).body(input()).set(() ->
				new ByteArrayInputStream(encode(Rest).getBytes(Codecs.UTF8))
		);
	}

	private Request delete(final Request request) {
		return request.method(Request.DELETE);
	}

	private Request post(final Request request) {
		return request.method(Request.POST).body(input()).set(() ->
				new ByteArrayInputStream(encode(Rest).getBytes(Codecs.UTF8))
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

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();

				}));
	}

	@Test void testGETCatalogPrivateAuthorized() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(authenticated(catalog(request())))

				.accept(response -> {

					assertThat(response).hasStatus(Response.OK);
					assertThat(response).hasBodyThat(rdf()).isIsomorphicTo(catalog());

				}));
	}

	@Test void testGETCatalogPublicAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(anonymous(catalog(request())))

				.accept(response -> {

					assertThat(response).hasStatus(Response.OK);
					assertThat(response).hasBodyThat(rdf()).isIsomorphicTo(catalog());

				}));
	}

	@Test void testGETCatalogPublicAuthorized() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(authenticated(catalog(request())))

				.accept(response -> {

					assertThat(response).hasStatus(Response.OK);
					assertThat(response).hasBodyThat(rdf()).isIsomorphicTo(catalog());

				}));
	}


	//// GET ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGETDefaultPrivateAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(anonymous(dflt(get(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();

				}));
	}

	@Test void testGETDefaultPrivateAuthenticated() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(authenticated(dflt(get(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.OK);
					assertThat(response).hasBodyThat(rdf()).isIsomorphicTo(First);

				}));
	}

	@Test void testGETDefaultPublicAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(anonymous(dflt(get(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.OK);
					assertThat(response).hasBodyThat(rdf()).isIsomorphicTo(First);

				}));
	}

	@Test void testGETDefaultPublicAuthenticated() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(authenticated(dflt(get(request()))))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.OK)
							.hasBodyThat(rdf()).isIsomorphicTo(First);

				}));
	}


	@Test void testGETNamedPrivateAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(anonymous(named(get(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).hasBodyThat(rdf()).isEmpty();

				}));
	}

	@Test void testGETNamedPrivateAuthenticated() {
		with(dflt(First), named(Rest)).exec(() -> _private(endpoint())

				.handle(authenticated(named(get(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.OK);
					assertThat(response).hasBodyThat(rdf()).isIsomorphicTo(Rest);

				}));
	}

	@Test void testGETNamedPublicAnonymous() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(anonymous(named(get(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.OK);
					assertThat(response).hasBodyThat(rdf()).isIsomorphicTo(Rest);

				}));
	}

	@Test void testGETNamedPublicAuthenticated() {
		with(dflt(First), named(Rest)).exec(() -> _public(endpoint())

				.handle(authenticated(named(get(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.OK);
					assertThat(response).hasBodyThat(rdf()).isIsomorphicTo(Rest);

				}));
	}


	//// PUT ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPUTDefaultPrivateAnonymous() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(anonymous(dflt(put(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(First);

				}));
	}

	@Test void testPUTDefaultPrivateAuthenticated() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(authenticated(dflt(put(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(Rest);

				}));
	}

	@Test void testPUTDefaultPublicAnonymous() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(anonymous(dflt(put(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(First);

				}));
	}

	@Test void testPUTDefaultPublicAuthenticated() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(authenticated(dflt(put(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(Rest);

				}));
	}


	@Test void testPUTNamedPrivateAnonymous() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(anonymous(named(put(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(First);

				}));
	}

	@Test void testPUTNamedPrivateAuthenticated() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(authenticated(named(put(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(Rest);

				}));
	}

	@Test void testPUTNamedPublicAnonymous() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(anonymous(named(put(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(First);

				}));
	}

	@Test void testPUTNamedPublicAuthenticated() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(authenticated(named(put(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(Rest);

				}));
	}


	//// DELETE ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDELETEDefaultPrivateAnonymous() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(anonymous(dflt(delete(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(First);

				}));
	}

	@Test void testDELETEDefaultPrivateAuthenticated() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(authenticated(dflt(delete(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isEmpty();

				}));
	}

	@Test void testDELETEDefaultPublicAnonymous() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(anonymous(dflt(delete(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(First);

				}));
	}

	@Test void testDELETEDefaultPublicAuthenticated() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(authenticated(dflt(delete(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isEmpty();

				}));
	}


	@Test void testDELETENamedPrivateAnonymous() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(anonymous(named(delete(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(First);

				}));
	}

	@Test void testDELETENamedPrivateAuthenticated() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(authenticated(named(delete(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isEmpty();

				}));
	}

	@Test void testDELETENamedPublicAnonymous() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(anonymous(named(delete(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(First);

				}));
	}

	@Test void testDELETENamedPublicAuthenticated() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(authenticated(named(delete(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isEmpty();

				}));
	}


	//// POST ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPOSTDefaultPrivateAnonymous() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(anonymous(dflt(post(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(First);

				}));
	}

	@Test void testPOSTDefaultPrivateAuthenticated() {
		with(dflt(First)).exec(() -> _private(endpoint())

				.handle(authenticated(dflt(post(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(union(First, Rest));

				}));
	}

	@Test void testPOSTDefaultPublicAnonymous() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(anonymous(dflt(post(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(First);

				}));
	}

	@Test void testPOSTDefaultPublicAuthenticated() {
		with(dflt(First)).exec(() -> _public(endpoint())

				.handle(authenticated(dflt(post(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(dflt()).isIsomorphicTo(union(First, Rest));

				}));
	}


	@Test void testPOSTNamedPrivateAnonymous() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(anonymous(named(post(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(First);

				}));
	}

	@Test void testPOSTNamedPrivateAuthenticated() {
		with(named(First)).exec(() -> _private(endpoint())

				.handle(authenticated(named(post(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(union(First, Rest));

				}));
	}

	@Test void testPOSTNamedPublicAnonymous() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(anonymous(named(post(request()))))

				.accept(response -> {

					assertThat(response).hasStatus(Response.Unauthorized);
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(First);

				}));
	}

	@Test void testPOSTNamedPublicAuthenticated() {
		with(named(First)).exec(() -> _public(endpoint())

				.handle(authenticated(named(post(request()))))

				.accept(response -> {

					assertThat(response).isSuccess();
					assertThat(response).doesNotHaveBody();
					assertThat(named()).isIsomorphicTo(union(First, Rest));

				}));
	}

}
