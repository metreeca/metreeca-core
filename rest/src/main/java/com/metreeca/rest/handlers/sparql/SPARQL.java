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
import com.metreeca.form.things.Formats;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Worker;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.*;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.query.resultio.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.*;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.Map;

import static com.metreeca.rest.Handler.refused;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.tray.Tray.tool;

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
public final class SPARQL implements Handler {

	private int timeout=60; // endpoint operations timeout [s]
	private boolean publik; // public availability of the endpoint

	private final Graph graph=tool(Graph.Factory);

	private final Handler delegate=new Worker()
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
		return consumer -> graph.query(connection -> {
			try {

				final Operation operation=operation(request, connection);
				final String accept=request.header("Accept").orElse("");

				if ( operation == null ) { // !!! return void description for GET

					request.reply(new Failure<>()
							.status(Response.BadRequest)
							.error("parameter-missing")
							.cause("missing query/update parameter")
					).accept(consumer);

				} else if ( !(publik && operation instanceof Query || request.role(Form.root)) ) {

					refused(request).accept(consumer);

				} else if ( operation instanceof BooleanQuery ) {

					final boolean result=((BooleanQuery)operation).evaluate();

					final BooleanQueryResultWriterFactory factory=Formats.service(
							BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);

					request.reply(response -> response.status(Response.OK)
							.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
							.body(output()).set(target -> {
								try (final OutputStream output=target.get()) {

									factory.getWriter(output).handleBoolean(result);

								} catch ( final IOException e ) {
									throw new UncheckedIOException(e);
								}
							})
					).accept(consumer);

				} else if ( operation instanceof TupleQuery ) {

					// ;( execute outside body callback to avoid exceptions after response is committed // !!! review

					final TupleQueryResult result=((TupleQuery)operation).evaluate();

					final TupleQueryResultWriterFactory factory=Formats.service(
							TupleQueryResultWriterRegistry.getInstance(), TupleQueryResultFormat.SPARQL, accept);

					request.reply(response -> response.status(Response.OK)
							.header("Content-Type", factory.getTupleQueryResultFormat().getDefaultMIMEType())
							.body(output()).set(target -> {
								try (final OutputStream output=target.get()) {

									final TupleQueryResultWriter writer=factory.getWriter(output);

									writer.startDocument();
									writer.startQueryResult(result.getBindingNames());

									while ( result.hasNext() ) { writer.handleSolution(result.next());}

									writer.endQueryResult();

								} catch ( final IOException e ) {
									throw new UncheckedIOException(e);
								} finally {
									result.close();
								}
							})).accept(consumer);

				} else if ( operation instanceof GraphQuery ) {

					// ;( execute outside body callback to avoid exceptions after response is committed // !!! review

					final GraphQueryResult result=((GraphQuery)operation).evaluate();

					final RDFWriterFactory factory=Formats.service(
							RDFWriterRegistry.getInstance(), RDFFormat.NTRIPLES, accept);

					request.reply(response -> response.status(Response.OK)
							.header("Content-Type", factory.getRDFFormat().getDefaultMIMEType())
							.body(output()).set(target -> {
								try (final OutputStream output=target.get()) {

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

								} catch ( final IOException e ) {
									throw new UncheckedIOException(e);
								}
							})).accept(consumer);

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

					request.reply(response -> response.status(Response.OK)
							.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
							.body(output()).set(target -> {
								try (final OutputStream output=target.get()) {
									factory.getWriter(output).handleBoolean(true);
								} catch ( final IOException e ) {
									throw new UncheckedIOException(e);
								}
							})
					).accept(consumer);

				} else {

					request.reply(new Failure<>()
							.status(Response.NotImplemented)
							.error("operation-unsupported")
							.cause(operation.getClass().getName())
					).accept(consumer);

				}

			} catch ( final MalformedQueryException e ) {

				request.reply(new Failure<>()
						.status(Response.BadRequest)
						.error("query-malformed")
						.cause(e)
				).accept(consumer);

			} catch ( final IllegalArgumentException e ) {

				request.reply(new Failure<>()
						.status(Response.BadRequest)
						.error("request-malformed")
						.cause(e)
				).accept(consumer);

			} catch ( final UnsupportedOperationException e ) {

				request.reply(new Failure<>()
						.status(Response.NotImplemented)
						.error("operation-unsupported")
						.cause(e)
				).accept(consumer);

			} catch ( final QueryEvaluationException e ) {

				// !!! fails for QueryInterruptedException (timeout) ≫ response is already committed

				request.reply(new Failure<>()
						.status(Response.InternalServerError)
						.error("query-evaluation")
						.cause(e)
				).accept(consumer);

			} catch ( final UpdateExecutionException e ) {

				request.reply(new Failure<>()
						.status(Response.InternalServerError)
						.error("update-evaluation")
						.cause(e)
				).accept(consumer);

			} catch ( final TupleQueryResultHandlerException e ) {

				request.reply(new Failure<>()
						.status(Response.InternalServerError)
						.error("response-error")
						.cause(e)
				).accept(consumer);

			} catch ( final RuntimeException e ) {

				request.reply(new Failure<>()
						.status(Response.InternalServerError)
						.error("repository-error")
						.cause(e)
				).accept(consumer);

			}
		});
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
