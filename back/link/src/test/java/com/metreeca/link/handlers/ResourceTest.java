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
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.eclipse.rdf4j.rio.RDFParserRegistry;
import org.junit.Test;

import static com.metreeca.jeep.rdf.ValuesTest.item;
import static com.metreeca.jeep.rdf.ValuesTest.parse;
import static com.metreeca.link.HandlerTest.*;
import static com.metreeca.spec.Shape.delete;
import static com.metreeca.spec.Shape.relate;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Any.any;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.When.when;

import static org.eclipse.rdf4j.model.util.Models.isomorphic;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.Collections.singleton;


public final class ResourceTest {

	//// Relate ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testRelateResource() {
		tools(catalog -> {

			final Graph graph=model(catalog.get(Graph.Tool), parse(
					"<target> rdf:value rdf:first, rdf:rest."
			));

			final IRI target=item("target");
			final String accept="text/turtle";

			response(catalog, handler(catalog),

					new Request()
							.setMethod(Request.GET)
							.setTarget(target.stringValue())
							.setHeader("Accept", accept),

					(request, response) -> {

						final RDFFormat format=RDFParserRegistry
								.getInstance()
								.getFileFormatForMIMEType(accept)
								.orElseThrow(UnsupportedOperationException::new);

						assertEquals("resource related",
								Response.OK, response.getStatus());

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

					tools, new Resource(tools, and()),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.GET)
							.setTarget(target.stringValue()),

					(request, response) -> {
						assertEquals("unsupported method", Response.MethodNotAllowed, response.getStatus());
					}

			);

		});
	}

	@Test public void testRelateRejectUnknownResource() {
		tools(tools -> {

			final IRI target=item("unknown");
			final String accept="text/turtle";

			exception(

					tools, new Resource(tools, and(

							trait(RDF.TYPE, all(RDF.NIL)) // implied statement (must not be included in response)

					)),

					new Request()
							.setMethod(Request.GET)
							.setTarget(target.stringValue())
							.setHeader("Accept", accept),

					(request, e) -> {

						assertEquals("unsupported method", Response.NotFound, e.getStatus());

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

					tools, new Resource(tools, and(
							and(relate(when(Spec.role, RDF.FIRST))), // not relatable by anonymous users
							trait(RDF.TYPE)
					)),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.GET)
							.setTarget(target.stringValue()),

					(request, response) -> {

						assertEquals("unauthorized", Response.Unauthorized, response.getStatus());
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

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.PUT)
							.setTarget(target.stringValue())
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime)
							.setText("<> rdf:value rdf:rest."),

					(request, response) -> {

						final Model model=new LinkedHashModel(model(graph));

						assertEquals("resource updated",
								Response.NoContent, response.getStatus());

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

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.PUT)
							.setTarget(target.stringValue())
							.setHeader("Content-Type", JSON.MIME)
							.setHeader("Accept", mime)
							.setText("{ \"value\": { \"this\" : \"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\" }}"),

					(request, response) -> {

						final Model model=new LinkedHashModel(model(graph));

						assertEquals("resource updated",
								Response.NoContent, response.getStatus());

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

					tools, new Resource(tools, relate(and())),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.PUT)
							.setTarget(target.stringValue()),

					(request, response) -> {

						assertEquals("unsupported method",
								Response.MethodNotAllowed, response.getStatus());

					});

		});
	}

	@Test public void testUpdateRejectUnknownResource() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final IRI target=item("target");
			final String mime="text/turtle";

			exception(catalog, handler(catalog),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.PUT)
							.setTarget(target.stringValue())
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime)
							.setText("<> rdf:value rdf:first, rdf:rest."),

					(request, e) -> {

						assertEquals("resource not found",
								Response.NotFound, e.getStatus());

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

					tools, new Resource(tools, and(
							and(Shape.update(when(Spec.role, RDF.FIRST))), // not updatable by anonymous users
							trait(RDF.TYPE)
					)),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.PUT)
							.setTarget(target.stringValue()),

					(request, response) -> {

						assertEquals("unauthorized",
								Response.Unauthorized, response.getStatus());

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

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setTarget(target.stringValue())
							.setMethod(Request.PUT)
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime)
							.setText("<> rdf:value rdf:nil."),

					(request, exception) -> {

						assertEquals("invalid data",
								Response.UnprocessableEntity, exception.getStatus());

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

			exception(catalog, handler(catalog), new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setTarget(target.stringValue())
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime)
							.setText("<> rdf:value rdf:first. <x> rdf:value 'x'.").setMethod(Request.PUT),

					(request, exception) -> {

						assertEquals("reachability error",
								Response.UnprocessableEntity, exception.getStatus());

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

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setTarget(target.stringValue())
							.setHeader("Content-Type", mime)
							.setHeader("Accept", mime)
							.setText("<> rdf:type ''; rdf:value rdf:first.").setMethod(Request.PUT),

					(request, exception) -> {

						assertEquals("envelope error",
								Response.UnprocessableEntity, exception.getStatus());

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

					tools, new Resource(tools, and(

							relate(trait(RDF.TYPE)), // not updatable/deletable

							trait(RDF.VALUE, any(RDF.FIRST, RDF.REST))

					)),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setMethod(Request.DELETE)
							.setTarget(target.stringValue()),

					(request, response) -> {

						final Model model=new LinkedHashModel(model(graph));

						assertEquals("resource delete",
								Response.NoContent, response.getStatus());

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

				new Resource(tools, relate(and())),

				new Request()

						.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
						.setTarget(item("target").stringValue())
						.setMethod(Request.DELETE),

				(request, response) -> assertEquals("unsupported method",
						Response.MethodNotAllowed, response.getStatus())));
	}

	@Test public void testDeleteRejectUnknownResource() {
		tools(catalog -> {

			final Graph graph=catalog.get(Graph.Tool);

			final IRI target=item("target");

			response(

					catalog, handler(catalog),

					new Request()

							.setRoles(singleton(Link.SysAdm)) // !!! remove after testing shape-based authorization
							.setTarget(target.stringValue())
							.setMethod(Request.DELETE),

					(request, response) -> {

						assertEquals("resource not found",
								Response.NotFound, response.getStatus());

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

					new Resource(tools, and(
							delete(when(Spec.role, Link.SysAdm)), // not deletable by anonymous users
							trait(RDF.TYPE)
					)),

					new Request()

							.setTarget(target.stringValue())
							.setMethod(Request.DELETE),

					(request, response) -> {

						assertEquals("unauthorized",
								Response.Unauthorized, response.getStatus());

						assertTrue("repository not modified",
								isomorphic(model, model(graph)));

					});

		});
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Handler handler(final Tool.Loader tools) {
		return new Resource(tools, and(

				relate(trait(RDF.TYPE)), // not updatable/deletable

				trait(RDF.VALUE, any(RDF.FIRST, RDF.REST))

		));
	}

}
