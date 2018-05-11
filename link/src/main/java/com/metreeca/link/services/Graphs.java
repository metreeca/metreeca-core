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
import com.metreeca.spec.Shape;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;
import java.util.regex.Pattern;

import static com.metreeca.jeep.Jeep.entry;
import static com.metreeca.jeep.Jeep.map;
import static com.metreeca.link.Handler.unauthorized;
import static com.metreeca.spec.Shape.only;
import static com.metreeca.spec.Values.iri;
import static com.metreeca.spec.Values.literal;
import static com.metreeca.spec.Values.statement;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Trait.trait;


/**
 * SPARQL 1.1 Graph Store endpoint.
 *
 * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update">SPARQL 1.1 Graph Store HTTP Protocol</a>
 */
public final class Graphs implements Service {

	private static final Shape GraphsShape=trait(RDF.VALUE, and(
			trait(RDF.TYPE, only(VOID.DATASET))
	));

	private static final Pattern AbsoluteIRIPattern=Pattern.compile("^\\w+:\\S+$");


	private boolean publik; // public availability of the endpoint

	private Graph graph;


	@Override public void load(final Tool.Loader tools) {

		final Setup setup=tools.get(Setup.Tool);
		final Index index=tools.get(Index.Tool);

		publik=setup.get("graphs.public", false);

		graph=tools.get(Graph.Tool);

		index.insert("/graphs", new Dispatcher(map(

				entry(Request.GET, this::get),
				entry(Request.PUT, this::put),
				entry(Request.DELETE, this::delete),
				entry(Request.POST, this::post)

		)), map(

				entry(RDFS.LABEL, literal("SPARQL 1.1 Graph Store Endpoint"))

		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void get(final Tool.Loader tools, // https://www.w3.org/TR/sparql11-http-rdf-update/#http-get
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		if ( response.getStatus() != 0 ) { sink.accept(request, response); } else {
			request.map(graph).browse(connection -> {

				final boolean catalog=request.getQuery().isEmpty();

				final String target=graph(request);
				final Iterable<String> accept=request.getHeaders("Accept");

				if ( target == null && !catalog ) {

					throw new LinkException(Response.BadRequest, "malformed request");

				} else if ( !publik && !request.isSysAdm() ) {

					unauthorized(tools, request, response, sink);

				} else if ( catalog ) { // graph catalog

					final IRI focus=iri(request.getTarget());
					final Collection<Statement> model=new ArrayList<>();

					try (final RepositoryResult<Resource> contexts=connection.getContextIDs()) {
						while ( contexts.hasNext() ) {

							final Resource context=contexts.next();

							model.add(statement(focus, RDF.VALUE, context));
							model.add(statement(context, RDF.TYPE, VOID.DATASET));

						}
					}

					response.setStatus(Response.OK);

					new Transfer(request, response).model(model, GraphsShape);

				} else {

					final RDFWriterFactory factory=Formats.service(
							RDFWriterRegistry.getInstance(), RDFFormat.TURTLE, accept);

					final RDFFormat format=factory.getRDFFormat();

					final Resource context=target.isEmpty() ? null : iri(target);

					response.setStatus(Response.OK);
					response.setHeader("Content-Type", format.getDefaultMIMEType());
					response.setHeader("Content-Disposition", "attachment; filename=\"%s.%s\"",
							target.isEmpty() ? "default" : target, format.getDefaultFileExtension());

					response.setBody(stream -> connection.export(factory.getWriter(stream), context));

				}

				sink.accept(request, response);

				return null;

			});
		}
	}

	private void put(final Tool.Loader tools, // https://www.w3.org/TR/sparql11-http-rdf-update/#http-put
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		if ( response.getStatus() != 0 ) { sink.accept(request, response); } else {

			final String target=graph(request);

			if ( target == null ) {

				throw new LinkException(Response.BadRequest, "malformed request");

			} else if ( !request.isSysAdm() ) {

				unauthorized(tools, request, response, sink);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);
				final Iterable<String> content=request.getHeaders("Content-Type");

				// !!! If a clients issues a POST or PUT with a content type that is not understood by the
				// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

				final RDFParserFactory factory=Formats.service( // !!! review fallback handling
						RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);

				try {

					request.map(graph).update(connection -> {

						try {

							final boolean exists=exists(connection, context);

							connection.clear(context);
							connection.add(request.getBody().get(), request.getBase(), factory.getRDFFormat(), context);

							response.setStatus(exists ? Response.NoContent : Response.Created);

							return null;

						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}

					});

				} catch ( final UncheckedIOException e ) {

					tools.get(Trace.Tool).warning(this, "unable to read RDF payload", e);

					response.setStatus(Response.InternalServerError).setText(e.getMessage());

				} catch ( final RDFParseException e ) {

					tools.get(Trace.Tool).warning(this, "malformed RDF payload", e);

					response.setStatus(Response.BadRequest)
							.setText("("+e.getLineNumber()+","+e.getColumnNumber()+") "+e.getMessage());

				} catch ( final RepositoryException e ) {

					tools.get(Trace.Tool).warning(this, "unable to update graph "+context, e);

					response.setStatus(Response.InternalServerError).setText(e.getMessage());

				}

			}

			sink.accept(request, response);

		}

	}

	private void delete(final Tool.Loader tools, // https://www.w3.org/TR/sparql11-http-rdf-update/#http-delete
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		if ( response.getStatus() != 0 ) { sink.accept(request, response); } else {

			final String target=graph(request);

			if ( target == null ) {

				throw new LinkException(Response.BadRequest, "malformed request");

			} else if ( !request.isSysAdm() ) {

				unauthorized(tools, request, response, sink);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);

				try {

					request.map(graph).update(connection -> {

						final boolean exists=exists(connection, context);

						connection.clear(context);

						response.setStatus(exists ? Response.NoContent : Response.NotFound);

						return null;

					});

				} catch ( final RepositoryException e ) {

					tools.get(Trace.Tool).warning(this, "unable to update graph "+context, e);

					response.setStatus(Response.InternalServerError).setText(e.getMessage());

				}

			}

			sink.accept(request, response);
		}

	}

	private void post(final Tool.Loader tools, // https://www.w3.org/TR/sparql11-http-rdf-update/#http-post
			final Request request, final Response response, final BiConsumer<Request, Response> sink) {

		// !!! support  "multipart/form-data"
		// !!! support graph creation with IRI identifying the underlying Graph Store

		if ( response.getStatus() != 0 ) { sink.accept(request, response); } else {

			final String target=graph(request);

			if ( target == null ) {

				throw new LinkException(Response.BadRequest, "malformed request");

			} else if ( !request.isSysAdm() ) {

				unauthorized(tools, request, response, sink);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);
				final Iterable<String> content=request.getHeaders("Content-Type");

				// !!! If a clients issues a POST or PUT with a content type that is not understood by the
				// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

				final RDFParserFactory factory=Formats.service( // !!! review fallback handling
						RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);

				try {

					request.map(graph).update(connection -> {

						final boolean exists=exists(connection, context);

						try {
							connection.add(request.getBody().get(), request.getBase(), factory.getRDFFormat(), context);
						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}

						response.setStatus(exists ? Response.NoContent : Response.Created);

						return null;

					});

				} catch ( final UncheckedIOException e ) {

					tools.get(Trace.Tool).warning(this, "unable to read RDF payload", e);

					response.setStatus(Response.InternalServerError).setText(e.getMessage());

				} catch ( final RDFParseException e ) {

					tools.get(Trace.Tool).warning(this, "malformed RDF payload", e);

					response.setStatus(Response.BadRequest)
							.setText("("+e.getLineNumber()+","+e.getColumnNumber()+") "+e.getMessage());

				} catch ( final RepositoryException e ) {

					tools.get(Trace.Tool).warning(this, "unable to update graph "+context, e);

					response.setStatus(Response.InternalServerError).setText(e.getMessage());

				}

				sink.accept(request, response);

			}
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String graph(final Request request) {

		final List<String> defaults=request.getParameters("default");
		final List<String> nameds=request.getParameters("graph");

		final boolean dflt=defaults.size() == 1 && defaults.get(0).isEmpty();
		final boolean named=nameds.size() == 1 && AbsoluteIRIPattern.matcher(nameds.get(0)).matches();

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
