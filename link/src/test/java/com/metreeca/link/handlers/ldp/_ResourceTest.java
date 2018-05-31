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

package com.metreeca.link.handlers.ldp;

import com.metreeca.link._junk._Handler;
import com.metreeca.link._junk._Request;
import com.metreeca.link._junk._Response;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.junit.Test;

import static com.metreeca.link._junk._HandlerTest.*;
import static com.metreeca.spec.Shape.delete;
import static com.metreeca.spec.Shape.relate;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Any.any;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.When.when;
import static com.metreeca.spec.things.ValuesTest.item;
import static com.metreeca.spec.things.ValuesTest.parse;

import static org.eclipse.rdf4j.model.util.Models.isomorphic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.Collections.singleton;


public final class _ResourceTest {

	//// Relate ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testRelateResource() {
		tools(catalog -> {

			final Graph graph=model(catalog.get(Graph.Tool), parse(
					"<target> rdf:value rdf:first, rdf:rest."
			));

			final IRI target=item("target");
			final String accept="text/turtle";

			response(catalog, handler(catalog),

					new _Request().setMethod(_Request.GET)
							.setTarget(target.stringValue())
							.setHeader("Accept", accept),

					(request, response) -> {

						final RDFFormat format=RDFParserRegistry
								.getInstance()
								.getFileFormatForMIMEType(accept)
								.orElseThrow(UnsupportedOperationException::new);

						assertEquals("resource related", _Response.OK, response.getStatus());

						assertEquals("content-type header set as required in the request",
								accept, response.getHeader("Content-Type").orElse(""));

						assertTrue("body include resource description",
								isomorphic(parse(response.getText(), format), model(graph)));
					});

		});
	}

	@Test public void testRelateRejectUnsupported() {
		tools(tools -> {

			final IRI target=item("target");

			response(

					tools, new _Resource(tools, and()),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setMethod(_Request.GET)
							.setTarget(target.stringValue()),

					(request, response) -> {
						assertEquals("unsupported method", _Response.MethodNotAllowed, response.getStatus());
					}

			);

		});
	}

	@Test public void testRelateRejectUnknownResource() {
		tools(tools -> {

			final IRI target=item("unknown");
			final String accept="text/turtle";

			exception(

					tools, new _Resource(tools, and(

							trait(RDF.TYPE, all(RDF.NIL)) // implied statement (must not be included in response)

					)),

					new _Request().setMethod(_Request.GET)
							.setTarget(target.stringValue())
							.setHeader("Accept", accept),

					(request, e) -> {

						assertEquals("unsupported method", _Response.NotFound, e.getStatus());

					}

			);

		});
	}

	@Test public void testRelateRejectUnauthorizedUser() {
		tools(tools -> {

			final Graph graph=tools.get(Graph.Tool);

			final IRI target=item("target");
			final Model model=parse("<target> rdf:value rdf:first.");

			model(graph, model);

			response(

					tools, new _Resource(tools, and(
							and(relate(when(Spec.role, RDF.FIRST))), // not relatable by anonymous users
							trait(RDF.TYPE)
					)),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setMethod(_Request.GET)
							.setTarget(target.stringValue()),

					(request, response) -> {

						assertEquals("unauthorized", _Response.Unauthorized, response.getStatus());
						assertTrue("repository not modified", isomorphic(model, model(graph)));

					}

			);

		});
	}


	////// Update ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testUpdateResource() {
		tools(catalog -> {

			final Graph graph=model(catalog.get(Graph.Tool), parse(

					"<target> rdf:type rdf:nil; rdf:value rdf:first."

			));

			final IRI target=item("target");
			final String mime="text/turtle";

			response(catalog, handler(catalog),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setMethod(_Request.PUT)
							.setTarget(target.stringValue())
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime)
							.setText("<> rdf:value rdf:rest."),

					(request, response) -> {

						final Model model=new LinkedHashModel(model(graph));

						assertEquals("resource updated", _Response.NoContent, response.getStatus());

						assertTrue("no content",
								response.getText().isEmpty());

						assertTrue("repository is updated",
								model.contains(target, RDF.VALUE, RDF.REST)
										&& !model.contains(target, RDF.VALUE, RDF.FIRST));

						assertTrue("non updatable statements are preserved",
								model.contains(target, RDF.TYPE, RDF.NIL));

					});

		});

	}

	@Test public void testUpdateResourceJSON() { // !!! factor
		tools(tools -> {

			final Graph graph=tools.get(Graph.Tool);

			model(graph, parse("<target> rdf:type rdf:nil; rdf:value rdf:first."));

			final IRI target=item("target");
			final String mime="text/turtle";

			response(tools, handler(tools),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setMethod(_Request.PUT)
							.setTarget(target.stringValue())
							.setHeader("Content-Type", "application/json")
							.setHeader("Accept", mime)
							.setText("{ \"value\": { \"this\" : \"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\" }}"),

					(request, response) -> {

						final Model model=new LinkedHashModel(model(graph));

						assertEquals("resource updated", _Response.NoContent, response.getStatus());

						assertTrue("repository is updated",
								model.contains(target, RDF.VALUE, RDF.REST)
										&& !model.contains(target, RDF.VALUE, RDF.FIRST));

						assertTrue("non updatable statements are preserved",
								model.contains(target, RDF.TYPE, RDF.NIL));
					});

		});
	}

	@Test public void testUpdateRejectUnsupported() {
		tools(tools -> {

			final IRI target=item("target");

			response(

					tools, new _Resource(tools, relate(and())),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setMethod(_Request.PUT)
							.setTarget(target.stringValue()),

					(request, response) -> {

						assertEquals("unsupported method", _Response.MethodNotAllowed, response.getStatus());

					});

		});
	}

	@Test public void testUpdateRejectUnknownResource() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final IRI target=item("target");
			final String mime="text/turtle";

			exception(catalog, handler(catalog),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setMethod(_Request.PUT)
							.setTarget(target.stringValue())
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime)
							.setText("<> rdf:value rdf:first, rdf:rest."),

					(request, e) -> {

						assertEquals("resource not found", _Response.NotFound, e.getStatus());

						assertTrue("repository not modified",
								model(graph).isEmpty());

					});

		});
	}

	@Test public void testUpdateRejectUnauthorizedUser() {
		tools(tools -> {

			final Graph graph=tools.get(Graph.Tool);

			final Model model=parse("<target> rdf:value rdf:first.");

			model(graph, model);

			final IRI target=item("target");

			response(

					tools, new _Resource(tools, and(
							and(Shape.update(when(Spec.role, RDF.FIRST))), // not updatable by anonymous users
							trait(RDF.TYPE)
					)),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setMethod(_Request.PUT)
							.setTarget(target.stringValue()),

					(request, response) -> {

						assertEquals("unauthorized", _Response.Unauthorized, response.getStatus());

						assertTrue("repository not modified",
								isomorphic(model, model(graph)));

					});

		});
	}

	@Test public void testUpdateRejectInvalidData() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final Model model=parse("<target> rdf:type rdf:nil.");

			model(graph, model);

			final IRI target=item("target");
			final String mime="text/turtle";

			exception(catalog, handler(catalog),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setTarget(target.stringValue()).setMethod(_Request.PUT)
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime)
							.setText("<> rdf:value rdf:nil."),

					(request, exception) -> {

						assertEquals("invalid data", _Response.UnprocessableEntity, exception.getStatus());

						assertTrue("repository not modified",
								isomorphic(model, model(graph)));

					});

		});
	}

	@Test public void testUpdateRejectUnreachableStatements() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final Model model=parse("<target> rdf:type rdf:nil.");

			model(graph, model);

			final IRI target=item("target");
			final String mime="text/turtle";

			exception(catalog, handler(catalog), new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setTarget(target.stringValue())
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime).setText("<> rdf:value rdf:first. <x> rdf:value 'x'.").setMethod(_Request.PUT),

					(request, exception) -> {

						assertEquals("reachability error", _Response.UnprocessableEntity, exception.getStatus());

						assertTrue("repository not modified",
								isomorphic(model, model(graph)));

					});

		});
	}

	@Test public void testUpdateRejectOutOfEnvelopeStatements() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final Model model=parse("<target> rdf:type rdf:nil.");

			model(graph, model);

			final IRI target=item("target");
			final String mime="text/turtle";

			exception(catalog, handler(catalog),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setTarget(target.stringValue())
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime).setText("<> rdf:type ''; rdf:value rdf:first.").setMethod(_Request.PUT),

					(request, exception) -> {

						assertEquals("envelope error", _Response.UnprocessableEntity, exception.getStatus());

						assertTrue("repository not modified",
								isomorphic(model, model(graph)));

					});

		});
	}


	//// Delete ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testDeleteResource() {
		tools(tools -> {

			final Graph graph=tools.get(Graph.Tool);

			model(graph, parse("<target> rdf:type rdf:nil; rdf:value rdf:first. <other> rdf:value rdf:rest."));

			final IRI target=item("target");

			response(

					tools, new _Resource(tools, and(

							relate(trait(RDF.TYPE)), // not updatable/deletable

							trait(RDF.VALUE, any(RDF.FIRST, RDF.REST))

					)),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setMethod(_Request.DELETE)
							.setTarget(target.stringValue()),

					(request, response) -> {

						final Model model=new LinkedHashModel(model(graph));

						assertEquals("resource delete", _Response.NoContent, response.getStatus());

						assertTrue("repository is updated",
								!model.contains(target, RDF.VALUE, RDF.FIRST));

						assertTrue("non deletable statements are preserved",
								model.contains(target, RDF.TYPE, RDF.NIL));

						assertTrue("empty response body",
								response.getText().isEmpty());
					});

		});
	}

	@Test public void testDeleteUnsupported() {
		tools(tools -> response(tools,

				new _Resource(tools, relate(and())),

				new _Request()

						.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
						.setTarget(item("target").stringValue()).setMethod(_Request.DELETE),

				(request, response) -> assertEquals("unsupported method", _Response.MethodNotAllowed, response.getStatus())));
	}

	@Test public void testDeleteRejectUnknownResource() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final IRI target=item("target");

			response(

					catalog, handler(catalog),

					new _Request()

							.setRoles(singleton(Spec.root)) // !!! remove after testing shape-based authorization
							.setTarget(target.stringValue()).setMethod(_Request.DELETE),

					(request, response) -> {

						assertEquals("resource not found", _Response.NotFound, response.getStatus());

						assertTrue("repository not modified",
								model(graph).isEmpty());

					});

		});
	}

	@Test public void testDeleteRejectUnauthorizedUser() {
		tools(tools -> {

			final Graph graph=tools.get(Graph.Tool);

			final Model model=parse("<target> rdf:value rdf:first.");

			model(graph, model);

			final IRI target=item("target");

			response(tools,

					new _Resource(tools, and(
							delete(when(Spec.role, Spec.root)), // not deletable by anonymous users
							trait(RDF.TYPE)
					)),

					new _Request()

							.setTarget(target.stringValue()).setMethod(_Request.DELETE),

					(request, response) -> {

						assertEquals("unauthorized", _Response.Unauthorized, response.getStatus());

						assertTrue("repository not modified",
								isomorphic(model, model(graph)));

					});

		});
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Handler handler(final Tool.Loader tools) {
		return new _Resource(tools, and(

				relate(trait(RDF.TYPE)), // not updatable/deletable

				trait(RDF.VALUE, any(RDF.FIRST, RDF.REST))

		));
	}

}
