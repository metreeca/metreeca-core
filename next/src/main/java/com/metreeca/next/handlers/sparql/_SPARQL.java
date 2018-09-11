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
import com.metreeca.next.*;
import com.metreeca.next.handlers.Dispatcher;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.query.Operation;
import org.eclipse.rdf4j.query.impl.SimpleDataset;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;

import static com.metreeca.tray._Tray.tool;

import static java.lang.Boolean.parseBoolean;


/**
 * SPARQL 1.1 Query/Update endpoint.
 *
 * <p>Provides a standard SPARQL 1.1 Query/Update endpoint exposing the contents of the system {@linkplain Graph#Factory
 * graph database}.</p>
 *
 * <p>Query operations are restricted to users in the {@linkplain Form#root root} {@linkplain Request#roles()
 * role}, unless otherwise specified through configuration properties; update operations are always restricted to users
 * in the  {@linkplain Form#root root} role.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-protocol/">SPARQL 1.1 Protocol</a>
 */
public class _SPARQL implements Handler {

	private int timeout=60; // [s]
	private boolean publik=false; // public availability of the endpoint

	private final Graph graph=tool(Graph.Factory);

	private final Handler delegate=new Dispatcher()
			.get(this::process)
			.post(this::process);


	@Override public Lazy<Response> handle(final Request request) {
		return delegate.handle(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public _SPARQL timeout(final int timeout) {

		if ( timeout < 0 ) {
			throw new IllegalArgumentException("illegal timeout ["+timeout+"]");
		}

		this.timeout=timeout;

		return this;
	}

	public _SPARQL publik(final boolean publik) {

		this.publik=publik;

		return this;
	}




	private Lazy<Response> process(final Request request) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	//private void process(final Request request, final Response response) {
	//	try (final RepositoryConnection connection=graph.connect()) {
	//
	//		final Operation operation=operation(request, connection);
	//		final String accept=request.header("Accept").orElse("");
	//
	//		if ( operation == null ) { // !!! return void description for GET
	//
	//			response.status(Response.BadRequest).json(Handler.error(
	//					"parameter-missing",
	//					"missing query/update parameter"
	//			));
	//
	//		} else if ( !(publik && operation instanceof Query || request.as(Form.root)) ) {
	//
	//			Handler.refused(request);
	//
	//		} else if ( operation instanceof BooleanQuery ) {
	//
	//			final boolean result=((BooleanQuery)operation).evaluate();
	//
	//			final BooleanQueryResultWriterFactory factory=Formats.service(
	//					BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);
	//
	//			response.status(Response.OK)
	//					.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
	//					.output(stream -> factory.getWriter(stream).handleBoolean(result));
	//
	//		} else if ( operation instanceof TupleQuery ) {
	//
	//			// ;( execute outside body callback to avoid exceptions after response is committed // !!! review
	//
	//			final TupleQueryResult result=((TupleQuery)operation).evaluate();
	//
	//			final TupleQueryResultWriterFactory factory=Formats.service(
	//					TupleQueryResultWriterRegistry.getInstance(), TupleQueryResultFormat.SPARQL, accept);
	//
	//			response.status(Response.OK)
	//					.header("Content-Type", factory.getTupleQueryResultFormat().getDefaultMIMEType())
	//					.output(stream -> {
	//
	//						final TupleQueryResultWriter writer=factory.getWriter(stream);
	//
	//						writer.startDocument();
	//						writer.startQueryResult(result.getBindingNames());
	//
	//						try {
	//							while ( result.hasNext() ) { writer.handleSolution(result.next());}
	//						} finally {
	//							result.close();
	//						}
	//
	//						writer.endQueryResult();
	//
	//					});
	//
	//		} else if ( operation instanceof GraphQuery ) {
	//
	//			// ;( execute outside body callback to avoid exceptions after response is committed // !!! review
	//
	//			final GraphQueryResult result=((GraphQuery)operation).evaluate();
	//
	//			final RDFWriterFactory factory=Formats.service(
	//					RDFWriterRegistry.getInstance(), RDFFormat.NTRIPLES, accept);
	//
	//			response.status(Response.OK)
	//					.header("Content-Type", factory.getRDFFormat().getDefaultMIMEType())
	//					.output(stream -> {
	//
	//						final RDFWriter writer=factory.getWriter(stream);
	//
	//						writer.startRDF();
	//
	//						for (final Map.Entry<String, String> entry : result.getNamespaces().entrySet()) {
	//							writer.handleNamespace(entry.getKey(), entry.getValue());
	//						}
	//
	//						try {
	//							while ( result.hasNext() ) { writer.handleStatement(result.next());}
	//						} finally {
	//							result.close();
	//						}
	//
	//						writer.endRDF();
	//
	//					});
	//
	//		} else if ( operation instanceof Update ) {
	//
	//			try {
	//
	//				connection.begin();
	//
	//				((Update)operation).execute();
	//
	//				connection.commit();
	//
	//			} catch ( final Throwable e ) {
	//
	//				connection.rollback();
	//
	//				throw e;
	//			}
	//
	//			final BooleanQueryResultWriterFactory factory=Formats.service(
	//					BooleanQueryResultWriterRegistry.getInstance(), BooleanQueryResultFormat.SPARQL, accept);
	//
	//			response.status(Response.OK)
	//					.header("Content-Type", factory.getBooleanQueryResultFormat().getDefaultMIMEType())
	//					.output(stream -> factory.getWriter(stream).handleBoolean(true));
	//
	//		} else {
	//
	//			response.status(Response.NotImplemented).json(Handler.error("operation-unsupported", operation.getClass().getName()));
	//
	//		}
	//
	//	} catch ( final MalformedQueryException e ) {
	//
	//		response.status(Response.BadRequest).cause(e).json(Handler.error(
	//				"query-malformed",
	//				e.getMessage()
	//		));
	//
	//	} catch ( final IllegalArgumentException e ) {
	//
	//		response.status(Response.BadRequest).cause(e).json(Handler.error(
	//				"request-malformed",
	//				e.getMessage()
	//		));
	//
	//	} catch ( final UnsupportedOperationException e ) {
	//
	//		response.status(Response.NotImplemented).cause(e).json(Handler.error(
	//				"operation-unsupported",
	//				e.getMessage()
	//		));
	//
	//	} catch ( final QueryEvaluationException e ) {
	//
	//		// !!! fails for QueryInterruptedException (timeout) ≫ response is already committed
	//
	//		response.status(Response.InternalServerError).cause(e).json(Handler.error(
	//				"query-evaluation",
	//				e.getMessage()
	//		));
	//
	//	} catch ( final UpdateExecutionException e ) {
	//
	//		response.status(Response.InternalServerError).cause(e).json(Handler.error(
	//				"update-evaluation",
	//				e.getMessage()
	//		));
	//
	//	} catch ( final TupleQueryResultHandlerException e ) {
	//
	//		response.status(Response.InternalServerError).cause(e).json(Handler.error(
	//				"response-error",
	//				e.getMessage()
	//		));
	//
	//	} catch ( final RuntimeException e ) {
	//
	//		response.status(Response.InternalServerError).cause(e).json(Handler.error(
	//				"repository-error",
	//				e.getMessage()
	//		));
	//
	//	}
	//
	//}


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
