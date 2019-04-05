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

package com.metreeca.rest.handlers.sparql;

import com.metreeca.form.Form;
import com.metreeca.form.things.Values;
import com.metreeca.form.truths.JsonAssert;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.GraphTest;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Set;
import java.util.function.Consumer;

import javax.json.JsonValue;

import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.bodies.JSONBody.json;
import static com.metreeca.rest.bodies.RDFBody.rdf;

import static org.assertj.core.api.Assertions.assertThat;

import static java.lang.String.format;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;


final class SPARQLTest {

	private static final Set<Statement> First=singleton(statement(RDF.NIL, RDF.VALUE, RDF.FIRST));
	private static final Set<Statement> Rest=singleton(statement(RDF.NIL, RDF.VALUE, RDF.REST));


	@SafeVarargs private final Tray with(final Collection<Statement>... datasets) {

		final Tray tray=new Tray();

		for (final Collection<Statement> dataset : datasets) {
			tray.exec(GraphTest.graph(dataset, RDF.NIL));
		}

		return tray;
	}


	private SPARQL endpoint() {
		return new SPARQL();
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


	private Request bool() {
		return new Request()
				.header("Accept", "application/json")
				.parameters("query", "ask { ?s ?p ?o }");
	}

	private Request tuple() {
		return new Request()
				.header("Accept", "application/json")
				.parameters("query", "select ?o { ?s ?p ?o }");
	}

	private Request graph() {
		return new Request()
				.header("Accept", "text/turtle")
				.parameters("query", "construct where { ?s ?p ?o }");
	}

	private Request update() {
		return new Request()
				.header("Accept", "application/json")
				.parameter("update", format(
						"prefix rdf: <%s> insert data { rdf:nil rdf:value rdf:rest }", RDF.NAMESPACE)
				);
	}

	private Request malformed(final String type) {
		return new Request().parameter(type, "!!!");
	}


	private Consumer<JsonValue> hasBooleanValue(final boolean value) {
		return json -> JsonAssert.assertThat(json.asJsonObject()).hasField("boolean", value);
	}

	private Consumer<JsonValue> hasBindings(final IRI... iris) {
		return json -> assertThat(json
				.asJsonObject()
				.getJsonObject("results")
				.getJsonArray("bindings")
				.stream()
				.map(b -> b.asJsonObject().getJsonObject("o"))
				.filter(b -> b.getString("type").equals("uri"))
				.map(b -> b.getString("value"))
				.map(Values::iri)
				.collect(toList())
		).containsExactly(iris);
	}

	private Consumer<Model> hasObjects(final IRI... objects) {
		return model -> assertThat(model.objects()).containsExactly(objects);
	}


	//// Boolean Query /////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPrivateAnonymousGETBooleanQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(anonymous(get(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAnonymousPOSTBooleanQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(anonymous(post(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAuthenticatedGETBooleanQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(authenticated(get(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json)
								.satisfies(hasBooleanValue(true))
						)
				)
		);
	}

	@Test void testPrivateAuthenticatedPOSTBooleanQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(authenticated(post(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json ->
								JsonAssert.assertThat(json).satisfies(hasBooleanValue(true)))));
	}


	@Test void testPublicAnonymousGETBooleanQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(anonymous(get(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBooleanValue(true)))
				));
	}

	@Test void testPublicAnonymousPOSTBooleanQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(anonymous(post(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBooleanValue(true)))
				));
	}

	@Test void testPublicAuthenticatedGETBooleanQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(authenticated(get(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBooleanValue(true)))
				));
	}

	@Test void testPublicAuthenticatedPOSTBooleanQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(authenticated(post(bool())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBooleanValue(true)))
				));
	}


	//// Tuple Query ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPrivateAnonymousGETTupleQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(anonymous(get(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAnonymousPOSTTupleQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(anonymous(post(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAuthenticatedGETTupleQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(authenticated(get(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPrivateAuthenticatedPOSTTupleQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(authenticated(post(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}


	@Test void testPublicAnonymousGETTupleQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(anonymous(get(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAnonymousPOSTTupleQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(anonymous(post(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAuthenticatedGETTupleQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(authenticated(get(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAuthenticatedPOSTTupleQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(authenticated(post(tuple())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBindings(RDF.FIRST, RDF.REST)))
				));
	}


	//// Graph Query ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPrivateAnonymousGETGraphQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(anonymous(get(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAnonymousPOSTGraphQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(anonymous(post(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAuthenticatedGETGraphQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(authenticated(get(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf).satisfies(hasObjects(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPrivateAuthenticatedPOSTGraphQuery() {
		with(First, Rest).exec(() -> _private(endpoint())

				.handle(authenticated(post(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf).satisfies(hasObjects(RDF.FIRST, RDF.REST)))
				));
	}


	@Test void testPublicAnonymousGETGraphQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(anonymous(get(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf).satisfies(hasObjects(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAnonymousPOSTGraphQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(anonymous(post(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf).satisfies(hasObjects(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAuthenticatedGETGraphQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(authenticated(get(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf).satisfies(hasObjects(RDF.FIRST, RDF.REST)))
				));
	}

	@Test void testPublicAuthenticatedPOSTGraphQuery() {
		with(First, Rest).exec(() -> _public(endpoint())

				.handle(authenticated(post(graph())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
						.hasBody(rdf(), rdf -> assertThat(rdf).satisfies(hasObjects(RDF.FIRST, RDF.REST)))
				));
	}


	//// Update ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPrivateAnonymousGETUpdate() {
		with(First).exec(() -> _private(endpoint())

				.handle(anonymous(get(update())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAnonymousPOSTUpdate() {
		with(First).exec(() -> _private(endpoint())

				.handle(anonymous(post(update())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPrivateAuthenticatedGETUpdate() {
		with(First).exec(() -> _private(endpoint())

				.handle(authenticated(get(update())))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBooleanValue(true)));

					assertThat(GraphTest.graph()).satisfies(hasObjects(RDF.FIRST, RDF.REST));

				}));
	}

	@Test void testPrivateAuthenticatedPOSTUpdate() {
		with(First).exec(() -> _private(endpoint())

				.handle(authenticated(post(update())))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBooleanValue(true)));

					assertThat(GraphTest.graph()).satisfies(hasObjects(RDF.FIRST, RDF.REST));

				}));
	}


	@Test void testPublicAnonymousGETUpdate() {
		with(First).exec(() -> _public(endpoint())

				.handle(anonymous(get(update())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPublicAnonymousPOSTUpdate() {
		with(First).exec(() -> _public(endpoint())

				.handle(anonymous(post(update())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.Unauthorized)
						.doesNotHaveBody()
				));
	}

	@Test void testPublicAuthenticatedGETUpdate() {
		with(First).exec(() -> _private(endpoint())

				.handle(authenticated(post(update())))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBooleanValue(true)));

					assertThat(GraphTest.graph()).satisfies(hasObjects(RDF.FIRST, RDF.REST));

				}));
	}

	@Test void testPublicAuthenticatedPOSTUpdate() {
		with(First).exec(() -> _private(endpoint())

				.handle(authenticated(post(update())))

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(json(), json -> JsonAssert.assertThat(json).satisfies(hasBooleanValue(true)));

					assertThat(GraphTest.graph()).satisfies(hasObjects(RDF.FIRST, RDF.REST));

				}));
	}


	//// Malformed /////////////////////////////////////////////////////////////////////////////////////////////////////


	@Test void testGETNoAction() {
		with().exec(() -> _private(endpoint())

				.handle(authenticated(get(new Request())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testPOSTNoAction() {
		with().exec(() -> _private(endpoint())

				.handle(authenticated(post(new Request())))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}


	@Test void testGETBothQueryAndUpdate() {
		with().exec(() -> _private(endpoint())

				.handle(authenticated(get(new Request())
						.parameter("query", "query")
						.parameter("update", "update")
				))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testPOSTBothQueryAndUpdate() {
		with().exec(() -> _private(endpoint())

				.handle(authenticated(post(new Request())
						.parameter("query", "query")
						.parameter("update", "update")
				))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}


	@Test void testGETQueryMalformed() {
		with().exec(() -> _private(endpoint())

				.handle(authenticated(get(malformed("query"))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testPOSTQueryMalformed() {
		with().exec(() -> _private(endpoint())

				.handle(authenticated(post(malformed("query"))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testGETUpdateMalformed() {
		with().exec(() -> _private(endpoint())

				.handle(authenticated(get(malformed("update"))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

	@Test void testPOSTUpdateMalformed() {
		with().exec(() -> _private(endpoint())

				.handle(authenticated(post(malformed("update"))))

				.accept(response -> assertThat(response)
						.hasStatus(Response.BadRequest)
						.hasBody(json()).isNotNull()
				));
	}

}
