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

package com.metreeca.link.services;

import com.metreeca.link.*;
import com.metreeca.spec.Spec;
import com.metreeca.spec.things.Formats;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.*;

import java.util.List;
import java.util.Map;

import static com.metreeca.link.Handler.error;
import static com.metreeca.link.Handler.refused;
import static com.metreeca.link.handlers.Dispatcher.dispatcher;
import static com.metreeca.tray.Tray.tool;

import static java.lang.Boolean.parseBoolean;


/**
 * SPARQL 1.1 Query/Update endpoint.
 *
 * <p>Provides a standard SPARQL 1.1 Query/Update endpoint exposing the contents of the system {@linkplain
 * Graph#Factory graph database} at the server-relative <code>{@value #Path}</code> path.</p>
 *
 * <p>Endpoint behaviour may be fine tuned with custom <a href="/modules/com.metreeca:tray/0.0/references/configuration#queryupdate">configuration properties</a>.</p>
 *
 * <p>Query operations are restricted to users in the {@linkplain Spec#root root} {@linkplain Request#roles()
 * role} , unless otherwise specified through configuration properties; update operations are restricted to users in the
 * root role.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a>
 */
public class SPARQL implements Service {

	private static final String Path="/sparql";


	private final Setup setup=tool(Setup.Factory);
	private final Index index=tool(Index.Factory);
	private final Graph graph=tool(Graph.Factory);

	private final int timeout=setup.get("sparql.timeout", 60); // [s]
	private final boolean publik=setup.get("sparql.public", false); // public availability of the endpoint


	@Override public void load() {

		index.insert(Path, dispatcher()

				.get(this::handle)
				.post(this::handle));

		// !!! port metadata
		//), map(
		//
		//		entry(RDFS.LABEL, literal("SPARQL 1.1 Query/Update Endpoint"))
		//
		//));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void handle(final Request request, final Response response) {
		try (final RepositoryConnection connection=graph.connect()) {

			final Operation operation=operation(request, connection);
			final String accept=request.header("Accept").orElse("");

			if ( operation == null ) { // !!! return void description for GET

				response.status(Response.BadRequest).json(error(
						"parameter-missing",
						"missing query/update parameter"
				));

			} else if ( !(publik && operation instanceof Query || request.role(Spec.root)) ) {

				refused(request, response);

			} else if ( operation instanceof BooleanQuery ) {

				final boolean result=((BooleanQuery)operation).evaluate();

				final BooleanQueryResultWriterFactory factory=Formats.service(
						BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);

				response.status(Response.OK)
						.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
						.output(stream -> factory.getWriter(stream).handleBoolean(result));

			} else if ( operation instanceof TupleQuery ) {

				// ;( execute outside body callback to avoid exceptions after response is committed // !!! review

				final TupleQueryResult result=((TupleQuery)operation).evaluate();

				final TupleQueryResultWriterFactory factory=Formats.service(
						TupleQueryResultWriterRegistry.getInstance(), TupleQueryResultFormat.SPARQL, accept);

				response.status(Response.OK)
						.header("Content-Type", factory.getTupleQueryResultFormat().getDefaultMIMEType())
						.output(stream -> {

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

				response.status(Response.OK)
						.header("Content-Type", factory.getRDFFormat().getDefaultMIMEType())
						.output(stream -> {

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

				response.status(Response.OK)
						.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
						.output(stream -> factory.getWriter(stream).handleBoolean(true));

			} else {

				response.status(Response.NotImplemented).json(error("operation-unsupported", operation.getClass().getName()));

			}

		} catch ( final MalformedQueryException e ) {

			response.status(Response.BadRequest).cause(e).json(error(
					"query-malformed",
					e.getMessage()
			));

		} catch ( final IllegalArgumentException e ) {

			response.status(Response.BadRequest).cause(e).json(error(
					"request-malformed",
					e.getMessage()
			));

		} catch ( final UnsupportedOperationException e ) {

			response.status(Response.NotImplemented).cause(e).json(error(
					"operation-unsupported",
					e.getMessage()
			));

		} catch ( final QueryEvaluationException e ) {

			// !!! fails for QueryInterruptedException (timeout) ≫ response is already committed

			response.status(Response.InternalServerError).cause(e).json(error(
					"query-evaluation",
					e.getMessage()
			));

		} catch ( final UpdateExecutionException e ) {

			response.status(Response.InternalServerError).cause(e).json(error(
					"update-evaluation",
					e.getMessage()
			));

		} catch ( final TupleQueryResultHandlerException e ) {

			response.status(Response.InternalServerError).cause(e).json(error(
					"response-error",
					e.getMessage()
			));

		} catch ( final RuntimeException e ) {

			response.status(Response.InternalServerError).cause(e).json(error(
					"repository-error",
					e.getMessage()
			));

		}

	}


	private Operation operation(final Request request, final RepositoryConnection connection) {

		final String query=request.parameter("query").orElse("");
		final String update=request.parameter("update").orElse("");
		final String infer=request.parameter("infer").orElse("");

		final List<String> basics=request.parameters("default-graph-uri");
		final List<String> nameds=request.parameters("named-graph-uri");

		final Operation operation=!query.isEmpty() ? connection.prepareQuery(query)
				: !update.isEmpty() ? connection.prepareUpdate(update)
				: null;

		if ( operation != null ) {

			final ValueFactory factory=connection.getValueFactory();
			final SimpleDataset dataset=new SimpleDataset();

			basics.stream().distinct().forEachOrdered(basic -> dataset.addDefaultGraph(factory.createIRI(basic)));
			nameds.stream().distinct().forEachOrdered(named -> dataset.addNamedGraph(factory.createIRI(named)));

			operation.setDataset(dataset);
			operation.setMaxExecutionTime(timeout);
			operation.setIncludeInferred(infer.isEmpty() || parseBoolean(infer));

		}

		return operation;

	}

}
