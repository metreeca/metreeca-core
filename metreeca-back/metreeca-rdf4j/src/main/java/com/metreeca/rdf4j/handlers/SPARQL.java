/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf4j.handlers;

import com.metreeca.rdf.Formats;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Worker;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.*;

import java.util.*;

import static com.metreeca.rest.formats.OutputFormat.output;


/**
 * SPARQL 1.1 Query/Update endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 Query/Update endpoint exposing the contents of the shared {@linkplain Graph
 * graph}.</p>
 *
 * <p>Both {@linkplain #query(Collection) query} and {@linkplain #update(Collection) update} operations are disabled,
 * unless otherwise specified.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a>
 */
public final class SPARQL extends Endpoint<SPARQL> {

	public SPARQL() {
		delegate(new Worker()
				.get(this::process)
				.post(this::process)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> process(final Request request) {
		return consumer -> graph().exec(connection -> {
			try {

				final Operation operation=operation(request, connection);

				if ( operation == null ) { // !!! return void description for GET

					request.reply(new Failure()
							.status(Response.BadRequest)
							.error("parameter-missing")
							.notes("missing query/update parameter")
					).accept(consumer);

				} else if ( operation instanceof Query && !queryable(request.roles())
						|| operation instanceof Update && !updatable(request.roles())
				) {

					request.reply(response -> response.status(Response.Unauthorized)).accept(consumer);

				} else if ( operation instanceof BooleanQuery ) {

					process(request, (BooleanQuery)operation).accept(consumer);

				} else if ( operation instanceof TupleQuery ) {

					process(request, (TupleQuery)operation).accept(consumer);

				} else if ( operation instanceof GraphQuery ) {

					process(request, (GraphQuery)operation).accept(consumer);

				} else if ( operation instanceof Update ) {

					process(request, (Update)operation).accept(consumer);

				} else {

					request.reply(new Failure()
							.status(Response.NotImplemented)
							.error("operation-unsupported")
							.notes(operation.getClass().getName())
					).accept(consumer);

				}

			} catch ( final MalformedQueryException e ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("query-malformed")
						.notes(e.getMessage())
						.cause(e)
				).accept(consumer);

			} catch ( final IllegalArgumentException e ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("request-malformed")
						.notes(e.getMessage())
						.cause(e)
				).accept(consumer);

			} catch ( final UnsupportedOperationException e ) {

				request.reply(new Failure()
						.status(Response.NotImplemented)
						.error("operation-unsupported")
						.notes(e.getMessage())
						.cause(e)
				).accept(consumer);

			} catch ( final QueryEvaluationException e ) {

				// !!! fails for QueryInterruptedException (timeout) ≫ response is already committed

				request.reply(new Failure()
						.status(Response.InternalServerError)
						.error("query-evaluation")
						.notes(e.getMessage())
						.cause(e)
				).accept(consumer);

			} catch ( final UpdateExecutionException e ) {

				request.reply(new Failure()
						.status(Response.InternalServerError)
						.error("update-evaluation")
						.notes(e.getMessage())
						.cause(e)
				).accept(consumer);

			} catch ( final TupleQueryResultHandlerException e ) {

				request.reply(new Failure()
						.status(Response.InternalServerError)
						.error("response-error")
						.notes(e.getMessage())
						.cause(e)
				).accept(consumer);

			} catch ( final RuntimeException e ) {

				request.reply(new Failure()
						.status(Response.InternalServerError)
						.error("repository-error")
						.notes(e.getMessage())
						.cause(e)
				).accept(consumer);

			}
		});
	}

	private Operation operation(final Request request, final RepositoryConnection connection) {

		final Optional<String> query=request.parameter("query");
		final Optional<String> update=request.parameter("update");
		final Optional<String> infer=request.parameter("infer");

		final Collection<String> basics=request.parameters("default-graph-uri");
		final Collection<String> nameds=request.parameters("named-graph-uri");

		final Operation operation=query.isPresent() ? connection.prepareQuery(query.get())
				: update.map(connection::prepareUpdate).orElse(null);

		if ( operation != null ) {

			final ValueFactory factory=connection.getValueFactory();
			final SimpleDataset dataset=new SimpleDataset();

			basics.stream().distinct().forEachOrdered(basic -> dataset.addDefaultGraph(factory.createIRI(basic)));
			nameds.stream().distinct().forEachOrdered(named -> dataset.addNamedGraph(factory.createIRI(named)));

			operation.setDataset(dataset);
			operation.setMaxExecutionTime(timeout());
			operation.setIncludeInferred(infer.map(Boolean::parseBoolean).orElse(true));

		}

		return operation;

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> process(final Request request, final BooleanQuery query) {

		final boolean result=query.evaluate();

		final String accept=request.header("Accept").orElse("");

		final BooleanQueryResultWriterFactory factory=Formats.service(
				BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);

		return request.reply(response -> response.status(Response.OK)
				.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
				.body(output(), output -> factory.getWriter(output).handleBoolean(result))
		);
	}

	private Future<Response> process(final Request request, final TupleQuery query) {

		final TupleQueryResult result=query.evaluate();

		final String accept=request.header("Accept").orElse("");

		final TupleQueryResultWriterFactory factory=Formats.service(
				TupleQueryResultWriterRegistry.getInstance(), TupleQueryResultFormat.SPARQL, accept);

		return request.reply(response -> response.status(Response.OK)
				.header("Content-Type", factory.getTupleQueryResultFormat().getDefaultMIMEType())
				.body(output(), output -> {
					try {

						final TupleQueryResultWriter writer=factory.getWriter(output);

						writer.startDocument();
						writer.startQueryResult(result.getBindingNames());

						while ( result.hasNext() ) { writer.handleSolution(result.next());}

						writer.endQueryResult();

					} finally {
						result.close();
					}
				}));
	}

	private Future<Response> process(final Request request, final GraphQuery query) {

		final GraphQueryResult result=query.evaluate();

		final String accept=request.header("Accept").orElse("");

		final RDFWriterFactory factory=Formats.service(
				RDFWriterRegistry.getInstance(), RDFFormat.NTRIPLES, accept);

		return request.reply(response -> response.status(Response.OK)
				.header("Content-Type", factory.getRDFFormat().getDefaultMIMEType())
				.body(output(), output -> {

					final RDFWriter writer=factory.getWriter(output);

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

				}));
	}

	private Future<Response> process(final Request request, final Update update) {

		update.execute();

		final String accept=request.header("Accept").orElse("");

		final BooleanQueryResultWriterFactory factory=Formats.service(
				BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);

		return request.reply(response -> response.status(Response.OK)
				.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
				.body(output(), output -> factory.getWriter(output).handleBoolean(true))
		);
	}

}
