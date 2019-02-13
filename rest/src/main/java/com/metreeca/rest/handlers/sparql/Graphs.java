/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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
import com.metreeca.form.Shape;
import com.metreeca.form.things.Formats;
import com.metreeca.form.things.Values;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Worker;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.rio.*;

import java.io.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.metreeca.form.Shape.only;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.Handler.refused;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;

import static java.lang.String.format;


/**
 * SPARQL 1.1 Graph Store endpoint handler.
 *
 * <p>Provides a standard SPARQL 1.1 Graph Store endpoint exposing the contents of the system {@linkplain Graph#Factory
 * graph database}</p>*
 *
 * <p>Query operations are restricted to users in the {@linkplain Form#root root} {@linkplain Request#roles()
 * role}, unless otherwise {@linkplain #publik(boolean) specified}; update operations are always restricted to users in
 * the root role.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update">SPARQL 1.1 Graph Store HTTP Protocol</a>
 */
public final class Graphs extends Delegator {

	private static final Shape GraphsShape=field(RDF.VALUE, and(
			field(RDF.TYPE, only(VOID.DATASET))
	));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean publik; // public availability of the endpoint

	private final Graph graph=tool(Graph.Factory);
	private final Trace trace=tool(Trace.Factory);


	public Graphs() {
		delegate(new Worker()
				.get(this::get)
				.put(this::put)
				.delete(this::delete)
				.post(this::post));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures public visibility for query endpoint operations.
	 *
	 * @param publik {@code true} if query endpoint operations should be available to any user; {@code false} otherwise
	 *
	 * @return this endpoint handler
	 */
	public Graphs publik(final boolean publik) {

		this.publik=publik;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-get
	 */
	private Responder get(final Request request) {
		return consumer -> {

			final boolean catalog=request.parameters().isEmpty();

			final String target=graph(request);
			final Iterable<String> accept=request.headers("Accept");

			if ( target == null && !catalog ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("parameter-missing")
						.cause("missing target graph parameter")
				).accept(consumer);

			} else if ( !publik && !request.role(Form.root) ) {

				refused(request).accept(consumer);

			} else if ( catalog ) { // graph catalog

				final IRI focus=request.item();
				final Collection<Statement> model=new ArrayList<>();

				graph.query(connection -> {
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

				graph.query(connection -> {
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
	private Responder put(final Request request) {
		return consumer -> {

			final String target=graph(request);

			if ( target == null ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("parameter-missing")
						.cause("missing target graph parameter")
				).accept(consumer);

			} else if ( !request.role(Form.root) ) {

				refused(request).accept(consumer);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);
				final Iterable<String> content=request.headers("Content-Type");

				// !!! If a clients issues a POST or PUT with a content type that is not understood by the
				// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

				final RDFParserFactory factory=Formats.service(
						RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content // !!! review fallback handling
				);

				graph.update(connection -> {
					try (final InputStream input=request.body(input()).value() // binary format >> no rewriting
							.orElseThrow(() -> new IllegalStateException("missing raw body")) // internal error
							.get()) {

						final boolean exists=exists(connection, context);

						connection.clear(context);
						connection.add(input, request.base(), factory.getRDFFormat(), context);

						request.reply(response ->
								response.status(exists ? Response.NoContent : Response.Created)
						).accept(consumer);

					} catch ( final IOException e ) {

						trace.warning(this, "unable to read RDF payload", e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("payload-unreadable")
								.cause("I/O while reading RDF payload: see server logs for more detail")
								.cause(e)
						).accept(consumer);

					} catch ( final RDFParseException e ) {

						trace.warning(this, "malformed RDF payload", e);

						request.reply(new Failure()
								.status(Response.BadRequest)
								.error("payload-malformed")
								.cause("malformed RDF payload: "+e.getLineNumber()+","+e.getColumnNumber()+") "+e.getMessage())
								.cause(e)
						).accept(consumer);

					} catch ( final RepositoryException e ) {

						trace.warning(this, "unable to update graph "+context, e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("update-aborted")
								.cause("unable to update graph: see server logs for more detail")
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
	private Responder delete(final Request request) {
		return consumer -> {

			final String target=graph(request);

			if ( target == null ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("parameter-missing")
						.cause("missing target graph parameter")
				).accept(consumer);

			} else if ( !request.role(Form.root) ) {

				refused(request).accept(consumer);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);

				graph.update(connection -> {
					try {

						final boolean exists=exists(connection, context);

						connection.clear(context);

						request.reply(response ->
								response.status(exists ? Response.NoContent : Response.NotFound)
						).accept(consumer);

					} catch ( final RepositoryException e ) {

						trace.warning(this, "unable to update graph "+context, e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("update-aborted")
								.cause("unable to delete graph: see server logs for more detail")
						).accept(consumer);

					}
				});
			}

		};
	}

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-post
	 */
	private Responder post(final Request request) {
		return consumer -> {

			// !!! support  "multipart/form-data"
			// !!! support graph creation with IRI identifying the underlying Graph Store

			final String target=graph(request);

			if ( target == null ) {

				request.reply(new Failure()
						.status(Response.BadRequest)
						.error("parameter-missing")
						.cause("missing target graph parameter")
				).accept(consumer);

			} else if ( !request.role(Form.root) ) {

				refused(request).accept(consumer);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);
				final Iterable<String> content=request.headers("Content-Type");

				// !!! If a clients issues a POST or PUT with a content type that is not understood by the
				// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

				final RDFParserFactory factory=Formats.service( // !!! review fallback handling
						RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);

				graph.update(connection -> {
					try (final InputStream input=request.body(input()).value() // binary format >> no rewriting
							.orElseThrow(() -> new IllegalStateException("missing raw body")) // internal error
							.get()) {

						final boolean exists=exists(connection, context);

						connection.add(input, request.base(), factory.getRDFFormat(), context);

						request.reply(response ->
								response.status(exists ? Response.NoContent : Response.Created)
						).accept(consumer);

					} catch ( final IOException e ) {

						trace.warning(this, "unable to read RDF payload", e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("payload-unreadable")
								.cause("I/O while reading RDF payload: see server logs for more detail")
								.cause(e)
						).accept(consumer);

					} catch ( final RDFParseException e ) {

						trace.warning(this, "malformed RDF payload", e);

						request.reply(new Failure()
								.status(Response.BadRequest)
								.error("payload-malformed")
								.cause("malformed RDF payload: "+e.getLineNumber()+","+e.getColumnNumber()+") "+e.getMessage())
								.cause(e)
						).accept(consumer);

					} catch ( final RepositoryException e ) {

						trace.warning(this, "unable to update graph "+context, e);

						request.reply(new Failure()
								.status(Response.InternalServerError)
								.error("update-aborted")
								.cause("unable to update graph: see server logs for more detail")
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
