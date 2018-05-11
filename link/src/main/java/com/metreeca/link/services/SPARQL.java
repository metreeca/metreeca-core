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

package com.metreeca.link.services;

import com.metreeca.link.*;
import com.metreeca.link.handlers.Dispatcher;
import com.metreeca.spec.Formats;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.*;

import java.util.HashSet;
import java.util.Map;
import java.util.function.BiConsumer;

import static com.metreeca.jeep.Jeep.entry;
import static com.metreeca.jeep.Jeep.map;
import static com.metreeca.link.Handler.unauthorized;
import static com.metreeca.spec.Values.literal;

import static java.lang.Boolean.parseBoolean;


/**
 * SPARQL 1.1 Query/Update endpoint.
 *
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a>
 */
public class SPARQL implements Service {

	private int timeout; // [s]
	private boolean publik; // public availability of the endpoint

	private Graph graph;


	@Override public void load(final Tool.Loader tools) {

		final Setup setup=tools.get(Setup.Tool);
		final Index index=tools.get(Index.Tool);

		timeout=setup.get("sparql.timeout", 60);
		publik=setup.get("sparql.public", false);

		graph=tools.get(Graph.Tool);

		index.insert("/sparql", new Dispatcher(map(

				entry(Request.GET, this::handle),
				entry(Request.POST, this::handle)

		)), map(

				entry(RDFS.LABEL, literal("SPARQL 1.1 Query/Update Endpoint"))

		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void handle(final Tool.Loader tools,
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		if ( response.getStatus() != 0 ) { sink.accept(request, response); } else {
			graph.browse(connection -> {

				try {

					final Operation operation=operation(request, connection);
					final Iterable<String> accept=request.getHeaders("Accept");

					if ( operation == null ) { // !!! return void description for GET

						throw new LinkException(Response.BadRequest, "missing query/update parameter");

					} else if ( !(publik && operation instanceof Query || request.isSysAdm()) ) {

						unauthorized(tools, request, response, sink);

					} else if ( operation instanceof BooleanQuery ) {

						final boolean result=((BooleanQuery)operation).evaluate();

						final BooleanQueryResultWriterFactory factory=Formats.service(
								BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);

						response.setStatus(Response.OK);
						response.setHeader("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType());
						response.setBody(stream -> factory.getWriter(stream).handleBoolean(result));

					} else if ( operation instanceof TupleQuery ) {

						// ;( execute outside body callback to avoid exceptions after response is committed // !!! review

						final TupleQueryResult result=((TupleQuery)operation).evaluate();

						final TupleQueryResultWriterFactory factory=Formats.service(
								TupleQueryResultWriterRegistry.getInstance(), TupleQueryResultFormat.SPARQL, accept);

						response.setStatus(Response.OK);
						response.setHeader("Content-Type", factory.getTupleQueryResultFormat().getDefaultMIMEType());
						response.setBody(stream -> {

							final TupleQueryResultWriter writer=factory.getWriter(stream);

							writer.startDocument();
							writer.startQueryResult(result.getBindingNames());

							try {
								while ( result.hasNext() ) { writer.handleSolution(result.next());}
							} finally {
								result.close();
							}

							writer.endQueryResult();

						});

					} else if ( operation instanceof GraphQuery ) {

						// ;( execute outside body callback to avoid exceptions after response is committed // !!! review

						final GraphQueryResult result=((GraphQuery)operation).evaluate();

						final RDFWriterFactory factory=Formats.service(
								RDFWriterRegistry.getInstance(), RDFFormat.NTRIPLES, accept);

						response.setStatus(Response.OK);
						response.setHeader("Content-Type", factory.getRDFFormat().getDefaultMIMEType());
						response.setBody(stream -> {

							final RDFWriter writer=factory.getWriter(stream);

							writer.startRDF();

							for (final Map.Entry<String, String> entry : result.getNamespaces().entrySet()) {
								writer.handleNamespace(entry.getKey(), entry.getValue());
							}

							try {
								while ( result.hasNext() ) { writer.handleStatement(result.next());}
							} finally {
								result.close();
							}

							writer.endRDF();
						});

					} else if ( operation instanceof Update ) {

						graph.update(_connection -> {

							((Update)operation).execute();

							return null;

						});

						final BooleanQueryResultWriterFactory factory=Formats.service(
								BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);

						response.setStatus(Response.OK);
						response.setHeader("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType());
						response.setBody(stream -> factory.getWriter(stream).handleBoolean(true));

					} else {

						throw new LinkException(Response.NotImplemented,
								"unsupported operation ["+operation.getClass().getName()+"]");

					}

					sink.accept(request, response);

				} catch ( final LinkException e ) {

					throw e;

				} catch ( final MalformedQueryException e ) {

					throw new LinkException(Response.BadRequest, "malformed query: "+e.getMessage(), e);

				} catch ( final IllegalArgumentException e ) {

					throw new LinkException(Response.BadRequest, "malformed request", e);

				} catch ( final UnsupportedOperationException e ) {

					throw new LinkException(Response.NotImplemented, "unsupported operation", e);

				} catch ( final QueryEvaluationException e ) {

					throw new LinkException(Response.InternalServerError, "query evaluation error", e);

				} catch ( final UpdateExecutionException e ) {

					throw new LinkException(Response.InternalServerError, "update execution error", e);

				} catch ( final TupleQueryResultHandlerException e ) {

					throw new LinkException(Response.InternalServerError, "response I/O error", e);

				} catch ( final RuntimeException e ) {

					throw new LinkException(Response.InternalServerError, "repository error", e);

				}

				return this;

			});
		}
	}


	private Operation operation(final Request request, final RepositoryConnection connection) {

		final String query=request.getParameter("query").orElse("");
		final String update=request.getParameter("update").orElse("");
		final String infer=request.getParameter("infer").orElse("");

		final Iterable<String> basics=new HashSet<>(request.getParameters("default-graph-uri"));
		final Iterable<String> nameds=new HashSet<>(request.getParameters("named-graph-uri"));

		final Operation operation=!query.isEmpty() ? connection.prepareQuery(query)
				: !update.isEmpty() ? connection.prepareUpdate(update)
				: null;

		if ( operation != null ) {

			final ValueFactory factory=connection.getValueFactory();
			final SimpleDataset dataset=new SimpleDataset();

			for (final String basic : basics) {
				dataset.addDefaultGraph(factory.createIRI(basic));
			}

			for (final String named : nameds) {
				dataset.addNamedGraph(factory.createIRI(named));
			}

			operation.setDataset(dataset);
			operation.setMaxExecutionTime(timeout);
			operation.setIncludeInferred(infer.isEmpty() || parseBoolean(infer));

		}

		return operation;

	}

}
