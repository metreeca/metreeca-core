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

package com.metreeca.next.handlers.sparql;

import com.metreeca.form.Form;
import com.metreeca.form.things.Formats;
import com.metreeca.next.*;
import com.metreeca.next.formats._Failure;
import com.metreeca.next.formats._Output;
import com.metreeca.next.handlers.Dispatcher;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.*;

import java.util.Collection;
import java.util.Map;

import static com.metreeca.next.Handler.refused;
import static com.metreeca.tray._Tray.tool;

import static java.lang.Boolean.parseBoolean;


/**
 * SPARQL 1.1 Query/Update endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 Query/Update endpoint exposing the contents of the system {@linkplain
 * Graph#Factory graph database}.</p>
 *
 * <p>Query operations are restricted to users in the {@linkplain Form#root root} {@linkplain Request#roles()
 * role}, unless otherwise {@linkplain #publik(boolean) specified}; update operations are always restricted to users in
 * the {@linkplain Form#root root} role.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a>
 */
@Deprecated public class SPARQL implements Handler {

	private int timeout=60; // endpoint operations timeout [s]
	private boolean publik; // public availability of the endpoint

	private final Graph graph=tool(Graph.Factory);

	private final Handler delegate=new Dispatcher()
			.get(this::process)
			.post(this::process);


	@Override public Responder handle(final Request request) {
		return delegate.handle(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures timeout for endpoint requests.
	 *
	 * @param timeout the timeout for endpoint requests in seconds or 0 to disable timeouts
	 *
	 * @return this endpoint handler
	 *
	 * @throws IllegalArgumentException if {@code timeout} is less than 0
	 */
	public SPARQL timeout(final int timeout) {

		if ( timeout < 0 ) {
			throw new IllegalArgumentException("illegal timeout ["+timeout+"]");
		}

		this.timeout=timeout;

		return this;
	}

	/**
	 * Configures public visibility for query endpoint operations.
	 *
	 * @param publik {@code true} if query endpoint operations should be available to any user; {@code false} otherwise
	 *
	 * @return this endpoint handler
	 */
	public SPARQL publik(final boolean publik) {

		this.publik=publik;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder process(final Request request) {
		try (final RepositoryConnection connection=graph.connect()) {

			final Operation operation=operation(request, connection);
			final String accept=request.header("Accept").orElse("");

			if ( operation == null ) { // !!! return void description for GET

				return request.reply(response -> response.body(_Failure.Format, new Failure(
						Response.BadRequest, "parameter-missing", "missing query/update parameter"
				)));

			} else if ( !(publik && operation instanceof Query || request.role(Form.root)) ) {

				return refused(request);

			} else if ( operation instanceof BooleanQuery ) {

				final boolean result=((BooleanQuery)operation).evaluate();

				final BooleanQueryResultWriterFactory factory=Formats.service(
						BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);

				return request.reply(response -> response.status(Response.OK)
						.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
						.body(_Output.Format, output -> factory.getWriter(output).handleBoolean(result))
				);

			} else if ( operation instanceof TupleQuery ) {

				// ;( execute outside body callback to avoid exceptions after response is committed // !!! review

				final TupleQueryResult result=((TupleQuery)operation).evaluate();

				final TupleQueryResultWriterFactory factory=Formats.service(
						TupleQueryResultWriterRegistry.getInstance(), TupleQueryResultFormat.SPARQL, accept);

				return request.reply(response -> response.status(Response.OK)
						.header("Content-Type", factory.getTupleQueryResultFormat().getDefaultMIMEType())
						.body(_Output.Format, output -> {
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

			} else if ( operation instanceof GraphQuery ) {

				// ;( execute outside body callback to avoid exceptions after response is committed // !!! review

				final GraphQueryResult result=((GraphQuery)operation).evaluate();

				final RDFWriterFactory factory=Formats.service(
						RDFWriterRegistry.getInstance(), RDFFormat.NTRIPLES, accept);

				return request.reply(response -> response.status(Response.OK)
						.header("Content-Type", factory.getRDFFormat().getDefaultMIMEType())
						.body(_Output.Format, output -> {

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

			} else if ( operation instanceof Update ) {

				try {

					connection.begin();

					((Update)operation).execute();

					connection.commit();

				} catch ( final Throwable e ) {

					connection.rollback();

					throw e;
				}

				final BooleanQueryResultWriterFactory factory=Formats.service(
						BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);

				return request.reply(response -> response.status(Response.OK)
						.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
						.body(_Output.Format, output -> factory.getWriter(output).handleBoolean(true))
				);

			} else {

				return request.reply(response -> response.body(_Failure.Format, new Failure(
						Response.NotImplemented, "operation-unsupported", operation.getClass().getName()
				)));

			}

		} catch ( final MalformedQueryException e ) {

			return request.reply(response -> response.cause(e).body(_Failure.Format, new Failure(
					Response.BadRequest, "query-malformed", e
			)));

		} catch ( final IllegalArgumentException e ) {

			return request.reply(response -> response.cause(e).body(_Failure.Format, new Failure(
					Response.BadRequest, "request-malformed", e
			)));

		} catch ( final UnsupportedOperationException e ) {

			return request.reply(response -> response.cause(e).body(_Failure.Format, new Failure(
					Response.NotImplemented, "operation-unsupported", e
			)));

		} catch ( final QueryEvaluationException e ) {

			// !!! fails for QueryInterruptedException (timeout) ≫ response is already committed

			return request.reply(response -> response.cause(e).body(_Failure.Format, new Failure(
					Response.InternalServerError, "query-evaluation", e
			)));

		} catch ( final UpdateExecutionException e ) {

			return request.reply(response -> response.cause(e).body(_Failure.Format, new Failure(
					Response.InternalServerError, "update-evaluation", e
			)));

		} catch ( final TupleQueryResultHandlerException e ) {

			return request.reply(response -> response.cause(e).body(_Failure.Format, new Failure(
					Response.InternalServerError, "response-error", e
			)));

		} catch ( final RuntimeException e ) {

			return request.reply(response -> response.cause(e).body(_Failure.Format, new Failure(
					Response.InternalServerError, "repository-error", e
			)));

		}

	}


	private Operation operation(final Request request, final RepositoryConnection connection) {

		final String query=request.parameter("query").orElse("");
		final String update=request.parameter("update").orElse("");
		final String infer=request.parameter("infer").orElse("");

		final Collection<String> basics=request.parameters("default-graph-uri");
		final Collection<String> nameds=request.parameters("named-graph-uri");

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
