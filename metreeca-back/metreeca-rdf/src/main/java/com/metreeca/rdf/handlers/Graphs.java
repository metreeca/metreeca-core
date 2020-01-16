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

package com.metreeca.rdf.handlers;

import com.metreeca.rdf.Formats;
import com.metreeca.rdf.Values;
import com.metreeca.rdf.services.Graph;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.InputFormat;
import com.metreeca.rest.handlers.Worker;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.statement;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.tree.Shape.only;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;

import static java.lang.String.format;


/**
 * SPARQL 1.1 Graph Store endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 Graph Store endpoint exposing the contents of the shared {@linkplain Graph
 * graph}.</p>
 *
 * <p>Both {@linkplain #query(Collection) query} and {@linkplain #update(Collection) update} operations are disabled,
 * unless otherwise specified.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update">SPARQL 1.1 Graph Store HTTP Protocol</a>
 */
public final class Graphs extends Endpoint<Graphs> {

	private static final Shape GraphsShape=field(RDF.VALUE, and(
			field(RDF.TYPE, only(VOID.DATASET))
	));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final InputFormat input=input();


	public Graphs() {
		delegate(new Worker()
				.get(this::get)
				.put(this::put)
				.delete(this::delete)
				.post(this::post)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-get
	 */
	private Future<Response> get(final Request request) {
		return consumer -> {

			final boolean catalog=request.parameters().isEmpty();

			final String target=graph(request);
			final Iterable<String> accept=request.headers("Accept");

			if ( target == null && !catalog ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("parameter-missing")
						.notes("missing target graph parameter")
				).accept(consumer);

			} else if ( !queryable(request.roles()) ) {

				request.reply(response -> response.status(Response.Unauthorized)).accept(consumer);

			} else if ( catalog ) { // graph catalog

				final IRI focus=iri(request.item());
				final Collection<Statement> model=new ArrayList<>();

				graph().exec(connection -> {
					try (final RepositoryResult<Resource> contexts=connection.getContextIDs()) {
						while ( contexts.hasNext() ) {

							final Resource context=contexts.next();

							model.add(statement(focus, RDF.VALUE, context));
							model.add(statement(context, RDF.TYPE, VOID.DATASET));

						}
					}
				});

				request.reply(response -> response.status(Response.OK)
						.shape(GraphsShape)
						.body(rdf(), model)
				).accept(consumer);

			} else {

				final RDFWriterFactory factory=Formats.service(
						RDFWriterRegistry.getInstance(), RDFFormat.TURTLE, accept);

				final RDFFormat format=factory.getRDFFormat();

				final Resource context=target.isEmpty() ? null : iri(target);

				graph().exec(connection -> {
					request.reply(response -> response.status(Response.OK)

							.header("Content-Type", format.getDefaultMIMEType())
							.header("Content-Disposition", format("attachment; filename=\"%s.%s\"",
									target.isEmpty() ? "default" : target, format.getDefaultFileExtension()
							))

							.body(output(), _target -> {
								try (final OutputStream output=_target.get()) {
									connection.export(factory.getWriter(output), context);
								} catch ( final IOException e ) {
									throw new UncheckedIOException(e);
								}
							})

					).accept(consumer);
				});
			}
		};
	}

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-put
	 */
	private Future<Response> put(final Request request) {
		return consumer -> {

			final String target=graph(request);

			if ( target == null ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("parameter-missing")
						.notes("missing target graph parameter")
				).accept(consumer);

			} else if ( !updatable(request.roles()) ) {

				request.reply(response -> response.status(Response.Unauthorized)).accept(consumer);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);
				final Iterable<String> content=request.headers("Content-Type");

				// !!! If a clients issues a POST or PUT with a content type that is not understood by the
				// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

				final RDFParserFactory factory=Formats.service(
						RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content // !!! review fallback handling
				);

				graph().exec(connection -> {
					try (final InputStream input=request.body(this.input).value() // binary format >> no rewriting
							.orElseThrow(() -> new IllegalStateException("missing raw body")) // internal error
							.get()) {

						final boolean exists=exists(connection, context);

						connection.clear(context);
						connection.add(input, request.base(), factory.getRDFFormat(), context);

						request.reply(response ->
								response.status(exists ? Response.NoContent : Response.Created)
						).accept(consumer);

					} catch ( final IOException e ) {

						logger().warning(this, "unable to read RDF payload", e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("payload-unreadable")
								.notes("I/O while reading RDF payload: see server logs for details")
								.cause(e)
						).accept(consumer);

					} catch ( final RDFParseException e ) {

						logger().warning(this, "malformed RDF payload", e);

						request.reply(new Failure()
								.status(Response.BadRequest)
								.error("payload-malformed")
								.notes("malformed RDF payload: "+e.getLineNumber()+","+e.getColumnNumber()+") "+e.getMessage())
								.cause(e)
						).accept(consumer);

					} catch ( final RepositoryException e ) {

						logger().warning(this, "unable to update graph "+context, e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("update-aborted")
								.notes("unable to update graph: see server logs for details")
								.cause(e)
						).accept(consumer);

					}
				});
			}

		};
	}

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-delete
	 */
	private Future<Response> delete(final Request request) {
		return consumer -> {

			final String target=graph(request);

			if ( target == null ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("parameter-missing")
						.notes("missing target graph parameter")
				).accept(consumer);

			} else if ( !updatable(request.roles()) ) {

				request.reply(response -> response.status(Response.Unauthorized)).accept(consumer);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);

				graph().exec(connection -> {
					try {

						final boolean exists=exists(connection, context);

						connection.clear(context);

						request.reply(response ->
								response.status(exists ? Response.NoContent : Response.NotFound)
						).accept(consumer);

					} catch ( final RepositoryException e ) {

						logger().warning(this, "unable to update graph "+context, e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("update-aborted")
								.notes("unable to delete graph: see server logs for details")
						).accept(consumer);

					}
				});
			}

		};
	}

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-post
	 */
	private Future<Response> post(final Request request) {
		return consumer -> {

			// !!! support  "multipart/form-data"
			// !!! support graph creation with IRI identifying the underlying Graph Store

			final String target=graph(request);

			if ( target == null ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("parameter-missing")
						.notes("missing target graph parameter")
				).accept(consumer);

			} else if ( !updatable(request.roles()) ) {

				request.reply(response -> response.status(Response.Unauthorized)).accept(consumer);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);
				final Iterable<String> content=request.headers("Content-Type");

				// !!! If a clients issues a POST or PUT with a content type that is not understood by the
				// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

				final RDFParserFactory factory=Formats.service( // !!! review fallback handling
						RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);

				graph().exec(connection -> {
					try (final InputStream input=request.body(this.input).value() // binary format >> no rewriting
							.orElseThrow(() -> new IllegalStateException("missing raw body")) // internal error
							.get()) {

						final boolean exists=exists(connection, context);

						connection.add(input, request.base(), factory.getRDFFormat(), context);

						request.reply(response ->
								response.status(exists ? Response.NoContent : Response.Created)
						).accept(consumer);

					} catch ( final IOException e ) {

						logger().warning(this, "unable to read RDF payload", e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("payload-unreadable")
								.notes("I/O while reading RDF payload: see server logs for details")
								.cause(e)
						).accept(consumer);

					} catch ( final RDFParseException e ) {

						logger().warning(this, "malformed RDF payload", e);

						request.reply(new Failure()
								.status(Response.BadRequest)
								.error("payload-malformed")
								.notes("malformed RDF payload: "+e.getLineNumber()+","+e.getColumnNumber()+") "+e.getMessage())
								.cause(e)
						).accept(consumer);

					} catch ( final RepositoryException e ) {

						logger().warning(this, "unable to update graph "+context, e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("update-aborted")
								.notes("unable to update graph: see server logs for details")
								.cause(e)
						).accept(consumer);

					}
				});

			}

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String graph(final Request request) {

		final List<String> defaults=request.parameters("default");
		final List<String> nameds=request.parameters("graph");

		final boolean dflt=defaults.size() == 1 && defaults.get(0).isEmpty();
		final boolean named=nameds.size() == 1 && Values.AbsoluteIRIPattern.matcher(nameds.get(0)).matches();

		return dflt && named ? null : dflt ? "" : named ? nameds.get(0) : null;
	}

	private boolean exists(final RepositoryConnection connection, final Resource context) {

		try (final RepositoryResult<Resource> contexts=connection.getContextIDs()) {

			while ( contexts.hasNext() ) {
				if ( contexts.next().equals(context) ) { return true; }
			}

		}

		return connection.hasStatement(null, null, null, true, context);
	}

}
