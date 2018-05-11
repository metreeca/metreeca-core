/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.link.handlers;

import com.metreeca.link.*;
import com.metreeca.mill.tasks.file.JSON;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.Ignore;
import org.junit.Test;

import static com.metreeca.link.HandlerTest.*;
import static com.metreeca.spec.Shape.only;
import static com.metreeca.spec.Shape.update;
import static com.metreeca.spec.Shape.verify;
import static com.metreeca.spec.Values.iri;
import static com.metreeca.spec.Values.literal;
import static com.metreeca.spec.Values.statement;
import static com.metreeca.spec.ValuesTest.*;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Any.any;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.When.when;

import static org.eclipse.rdf4j.model.util.Models.isSubset;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


public final class ContainerTest {

	//// Relate ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testRelateContainer() {
		tools(tools -> {

			final Model items=parse(
					"<item-1> a :Item; rdf:value 1 . <item-2> a :Item; rdf:value 2 . <item-3> a :Item; rdf:value 3 ."
			);

			model(tools.get(Graph.Tool), items);

			final IRI target=item("target/");
			final String accept="text/turtle";

			response(

					tools,

					new Container(tools, and(
							trait(RDFS.LABEL, verify(only(literal("Test Catalog")))),
							trait(LDP.CONTAINS, and(
									trait(RDF.TYPE, all(term("Item"))),
									trait(RDF.VALUE)
							))
					)),

					new Request()
							.setMethod(Request.GET)
							.setTarget(target.stringValue())
							.setHeader("Accept", accept),

					(request, response) -> {

						final Model model=parse(response.getText());

						assertEquals("resource related",
								Response.OK, response.getStatus());

						assertEquals("content-type header set as required in the request",
								accept, response.getHeader("Content-Type").orElse(""));

						assertTrue("no filtering > container metadata included", isSubset(
								parse("<target/> rdfs:label 'Test Catalog'."), model));

						assertTrue("items retrieved", isSubset(items, model));

						assertTrue("items linked to container", isSubset(items.subjects().stream()
								.map(item -> statement(target, LDP.CONTAINS, item))
								.collect(toSet()), model));

					});

		});
	}

	@Test public void testRelateMinimalContainer() {
		tools(tools -> {

			final Model items=parse(
					"<item-1> a :Item; rdf:value 1 . <item-2> a :Item; rdf:value 2 . <item-3> a :Item; rdf:value 3 ."
			);

			model(tools.get(Graph.Tool), items);

			final IRI target=item("target/");
			final String accept="text/turtle";

			response(

					tools,

					new Container(tools, and(
							trait(RDFS.LABEL, verify(only(literal("Test Catalog")))),
							trait(LDP.CONTAINS, and(
									trait(RDF.TYPE, all(term("Item"))),
									trait(RDF.VALUE)
							))
					)),

					new Request()
							.setMethod(Request.GET)
							.setTarget(target.stringValue())
							.setHeader("Prefer", "return=representation; include=\"http://www.w3.org/ns/ldp#PreferMinimalContainer\"")
							.setHeader("Accept", accept),

					(request, response) -> {

						final Model model=parse(response.getText());

						assertEquals("resource related",
								Response.OK, response.getStatus());

						assertEquals("content-type header set as required in the request",
								accept, response.getHeader("Content-Type").orElse(""));

						assertTrue("minimal container > container metadata included", isSubset(
								parse("<target/> rdfs:label 'Test Catalog'."), model));

						assertFalse("minimal container > items not retrieved",
								model.contains(target, LDP.CONTAINS, null));

					});

		});
	}

	@Ignore @Test public void testRelateContainerFiltered() {}

	@Test public void testRelateUnsupported() {
		tools(tools -> {

			final IRI target=item("target/");

			response(

					tools, new Container(tools, update(trait(LDP.CONTAINS))), // not relatable

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.GET)
							.setTarget(target.stringValue()),

					(request, response) -> assertEquals("unsupported method",
							Response.MethodNotAllowed, response.getStatus()));

		});
	}

	@Test public void testRelateRejectUnauthorizedUser() {
		tools(tools -> {

			final Model model=parse("<target> rdf:value rdf:first.");
			final Graph graph=model(tools.get(Graph.Tool), model);

			final IRI target=item("target/");

			response(

					tools, new Container(tools, trait(LDP.CONTAINS, and(
							and(Shape.relate(when(Spec.role, RDF.FIRST))), // not relatable by anonymous users
							trait(RDF.TYPE)
					))),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.GET)
							.setTarget(target.stringValue()),

					(request, response) -> {

						assertEquals("unauthorized",
								Response.Unauthorized, response.getStatus());

						assertTrue("repository not modified",
								Models.isomorphic(model, model(graph)));

					});

		});
	}

	@Test public void testRelateRejectMalformedQuery() {
		tools(catalog -> {

			final IRI target=item("target/");

			exception(catalog, handler(catalog),

					new Request()
							.setMethod(Request.GET)
							.setTarget(target.stringValue())
							.setQuery("{ \"filter\": { \"unknown\": [] }}"),

					(request, exception) -> assertEquals("resource related",
							Response.BadRequest, exception.getStatus()));

		});
	}


	//// Create ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testCreateResource() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final IRI target=item("target/");
			final String accept="text/plain";
			final String body="<> rdf:value rdf:first.";

			response(catalog, handler(catalog),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.POST)
							.setTarget(target.stringValue())
							.setHeader("Accept", accept)
							.setHeader("Content-Type", "text/turtle")
							.setText(body),

					(request, response) -> {

						final String location=response.getHeader("Location").orElse("");

						final IRI iri=iri(location);
						final IRI base=iri(Base);

						assertEquals("resource created",
								Response.Created, response.getStatus());

						assertTrue("location header set to a target-derived iri",
								location.startsWith(target.stringValue()));

						assertTrue("submitted statements are rewritten to the created resource and stored in repository",
								isSubset(
										parse(body).stream()
												.map(s -> statement(
														s.getSubject().equals(base) ? iri : s.getSubject(),
														s.getPredicate(),
														s.getObject().equals(base) ? iri : s.getObject()))
												.collect(toList()),
										model(graph)
								));

					});

		});
	}


	@Test public void testCreateResourceJSON() { // !!! factor
		tools(catalog -> {

			final IRI target=item("target/");
			final String accept=JSON.MIME;
			final String body="{ \"value\": { \"this\" : \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" }}";

			response(catalog, handler(catalog),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.POST)
							.setTarget(target.stringValue())
							.setHeader("Accept", accept)
							.setHeader("Content-Type", accept)
							.setText(body),

					(request, response) -> {

						final String location=response.getHeader("Location").orElse("");

						final IRI iri=iri(location);
						final IRI base=iri(Base);

						assertEquals("resource created",
								Response.Created, response.getStatus());

						assertTrue("location header set to a target-derived iri",
								location.startsWith(target.stringValue()));

					});

		});
	}

	@Test public void testCreateUnsupported() {
		tools(tools -> {

			final IRI target=item("target/");

			response(

					tools, new Container(tools, update(trait(LDP.CONTAINS))), // not creatable

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.POST)
							.setTarget(target.stringValue()),

					(request, response) -> assertEquals("unsupported method",
							Response.MethodNotAllowed, response.getStatus()));

		});
	}

	@Test public void testCreateRejectUnauthorizedUser() {
		tools(tools -> {

			final Graph graph=tools.get(Graph.Tool);

			final IRI target=item("target/");
			final Model model=parse("<target> rdf:value rdf:first.");

			model(graph, model);

			response(

					tools, new Container(tools, trait(LDP.CONTAINS, and(
							and(Shape.create(when(Spec.role, RDF.FIRST))), // not creatable by anonymous users
							trait(RDF.TYPE)
					))),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.POST)
							.setTarget(target.stringValue()),

					(request, response) -> {

						assertEquals("unauthorized",
								Response.Unauthorized, response.getStatus());

						assertTrue("repository not modified",
								Models.isomorphic(model, model(graph)));

					});

		});
	}

	@Test public void testCreateRejectInvalidData() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final IRI target=item("target/");

			exception(catalog, handler(catalog),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.POST)
							.setTarget(target.stringValue())
							.setText("<target> rdf:value rdf:nil."),

					(request, exception) -> {

						assertEquals("invalid data",
								Response.UnprocessableEntity, exception.getStatus());

						//assertNull("location header not set",
						//		exception.getHeader("Location"));

						assertTrue("repository not modified",
								model(graph).isEmpty());

					});

		});
	}

	@Test public void testCreateRejectUnreachableStatements() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final IRI target=item("target/");

			exception(catalog, handler(catalog),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.POST)
							.setTarget(target.stringValue())
							.setText("<x> a <y>."),

					(request, exception) -> {

						assertEquals("reachability error",
								Response.UnprocessableEntity, exception.getStatus());

						//assertNull("location header not set",
						//		exception.getHeader("Location"));

						assertTrue("repository not modified",
								model(graph).isEmpty());

					});

		});
	}

	@Test public void testCreateRejectOutOfEnvelopeStatements() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final IRI target=item("target/");

			exception(catalog, handler(catalog),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.POST)
							.setTarget(target.stringValue())
							.setText("<> a <y>."),

					(request, exception) -> {

						assertEquals("envelope error",
								Response.UnprocessableEntity, exception.getStatus());

						// !!! assertNull("location header not set",
						//		exception.getHeader("Location"));

						assertTrue("repository not modified",
								model(graph).isEmpty());

					});

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Handler handler(final Tool.Loader tools) {
		return new Container(tools, trait(LDP.CONTAINS, trait(RDF.VALUE, any(RDF.FIRST, RDF.REST))));
	}

}
