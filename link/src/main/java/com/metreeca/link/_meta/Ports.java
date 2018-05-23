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

package com.metreeca.link._meta;

import com.metreeca.link.*;
import com.metreeca.link.handlers.Container;
import com.metreeca.link.handlers.Dispatcher;
import com.metreeca.link.handlers.Resource;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.things.Values;
import com.metreeca.spec.things._Cell;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.function.BiConsumer;

import static com.metreeca.link._Handler.sysadm;
import static com.metreeca.spec.Shape.*;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Datatype.datatype;
import static com.metreeca.spec.shapes.Pattern.pattern;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Maps.map;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.literal;


/**
 * LDP port catalog.
 */
public final class Ports implements _Service {

	private static final String ContainerLabel="Linked Data Ports";


	private static final String PathPattern="(/[-+\\w]+)*(\\?|/|/\\*)?"; // keep aligned with client

	private static final Shape ResourceShape=and(  // !!! review for hard ports

			trait(RDF.TYPE, and(required(), only(Link.Port))),

			verify(

					trait(RDFS.LABEL, and(required(), datatype(XMLSchema.STRING))),
					trait(RDFS.COMMENT, and(optional(), datatype(XMLSchema.STRING))),

					trait(Link.root, and(required(), datatype(XMLSchema.BOOLEAN))),
					trait(Link.path, and(required(), pattern(PathPattern))),

					and( // !!! as detail (currently unsupported by SPARQL probe)

							trait(Link.spec, and(required(), datatype(XMLSchema.STRING))),

							trait(Link.create, and(optional(), datatype(XMLSchema.STRING))),
							trait(Link.update, and(optional(), datatype(XMLSchema.STRING))),
							trait(Link.delete, and(optional(), datatype(XMLSchema.STRING))),
							trait(Link.mutate, and(optional(), datatype(XMLSchema.STRING)))),

					server(

							trait(Link.uuid, and(required(), datatype(XMLSchema.STRING))),
							trait(Link.soft, and(required(), datatype(XMLSchema.BOOLEAN))),

							trait(RDFS.ISDEFINEDBY, and(optional(), datatype(Values.IRIType),
									relate(trait(RDFS.LABEL, optional()))
							)))));

	private static final Shape ContainerShape=verify(

			trait(RDF.TYPE, and(required(), only(LDP.BASIC_CONTAINER))),
			trait(RDFS.LABEL, and(required(), only(literal(ContainerLabel)))),

			trait(LDP.CONTAINS, ResourceShape));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void load(final Tool.Loader tools) {
		tools.get(Index.Tool).exec(index -> index

				.insert("/!/ports/", new ContainerHandler(tools), map(
						entry(RDFS.LABEL, literal(ContainerLabel))
				))

				.insert("/!/ports/*", new ResourceHandler(tools), map(entry(RDFS.LABEL, literal(ContainerLabel+Index.ResourcesSuffix))
				))
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ContainerHandler implements _Handler {

		private final Index index;
		private final Graph graph;

		private final _Handler container; // shaped delegate

		private final _Handler dispatcher=new Dispatcher(map(entry(_Request.GET, sysadm(this::get)), entry(_Request.POST, sysadm(this::post))
		));


		private ContainerHandler(final Tool.Loader tools) {

			final Setup setup=tools.get(Setup.Tool);


			this.index=tools.get(Index.Tool);
			this.graph=tools.get(Graph.Tool);

			this.container=new Container(tools, ContainerShape);
		}


		@Override public void handle(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

			dispatcher.handle(tools, request, response, sink);

		}


		private void get(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

			container.handle(tools, request, response, sink);

		}

		private void post(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

			final Graph graph=request.map(this.graph);

			final Shape creating=ResourceShape.accept(task(Spec.create));
			final Shape authorized=creating.accept(role(request.getRoles()));

			index.exec(_index -> graph.update(connection -> { // inside index/graph transactions

				container.handle(tools, request, response, (_request, _response) -> { // !!! insert uuid slug in request

					if ( _response.getStatus()/100 == 2 ) {

						// retrieve assigned port identifier

						final IRI iri=_response.getHeader("Location").map(Values::iri).orElseThrow(() ->
								new IllegalStateException("missing Location: header in response")); // unexpected

						// retrieve port specification and attach port

						index.insert(graph.get(and(all(iri), authorized)));

					}

					sink.accept(_request, _response);

				});

				return null;

			}));

		}

	}

	private static final class ResourceHandler implements _Handler {

		private final Index index;
		private final Graph graph;

		private final _Handler resource; // shaped delegate

		private final _Handler dispatcher=new Dispatcher(map(entry(_Request.GET, sysadm(this::get)), entry(_Request.PUT, sysadm(this::put)), entry(_Request.DELETE, sysadm(this::delete))
		));


		private ResourceHandler(final Tool.Loader tools) {

			final Setup setup=tools.get(Setup.Tool);

			this.index=tools.get(Index.Tool);
			this.graph=tools.get(Graph.Tool);

			this.resource=new Resource(tools, ResourceShape);
		}


		@Override public void handle(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

			dispatcher.handle(tools, request, response, sink);

		}


		private void get(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

			resource.handle(tools, request, response, sink);

		}

		private void put(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

			// !!! prevent hard-wired port updating

			final Graph graph=request.map(this.graph);

			final Shape updating=ResourceShape.accept(task(Spec.update));
			final Shape authorized=updating.accept(role(request.getRoles()));
			final Shape shape=and(all(iri(request.getTarget())), authorized);

			index.exec(_index -> graph.update(connection -> { // inside index/graph transactions

				final _Cell current=graph.get(shape); // retrieve current port specs

				resource.handle(tools, request, response, (_request, _response) -> {

					if ( _response.getStatus()/100 == 2 ) {

						final _Cell updated=graph.get(shape); // retrieve updated port specs

						index.remove(current);

						try {

							index.insert(updated);

							sink.accept(_request, _response);

						} catch ( final Throwable t ) {

							try { // !!! convert to status code outside update txn

								throw new _LinkException(_Response.UnprocessableEntity, t.getMessage(), t);

							} finally {
								index.insert(current); // try to restore the current configuration
							}

						}

					}

				});

				return null;

			}));

		}

		private void delete(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

			final Graph graph=request.map(this.graph);

			final Shape deleting=ResourceShape.accept(task(Spec.delete));
			final Shape authorized=deleting.accept(role(request.getRoles()));
			final Shape shape=and(all(iri(request.getTarget())), authorized);

			index.exec(_index -> graph.update(connection -> { // inside index/graph transactions

				final _Cell current=graph.get(shape); // retrieve current port specs

				if ( !current.forward(Link.soft).bool().orElse(false) ) { // prevent hard-wired port deletion

					sink.accept(request, response.setStatus(_Response.Forbidden)); // !!! convert to status code outside update txn

				} else {

					resource.handle(tools, request, response, (_request, _response) -> {

						if ( _response.getStatus()/100 == 2 ) {
							index.remove(current);
						}

						sink.accept(_request, _response);

					});
				}

				return null;

			}));

		}

	}

}
