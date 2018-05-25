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
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.things.Formats;
import com.metreeca.spec.things.Values;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.RepositoryResult;
import org.eclipse.rdf4j.rio.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import static com.metreeca.link.Handler.error;
import static com.metreeca.link.Handler.refused;
import static com.metreeca.link.handlers.Dispatcher.dispatcher;
import static com.metreeca.spec.Shape.only;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.statement;
import static com.metreeca.tray.Tray.tool;


/**
 * SPARQL 1.1 Graph Store endpoint.
 *
 * <p>Provides a standard SPARQL 1.1 Graph Store endpoint exposing the contents of the system {@linkplain
 * Graph#Tool graph database} at the server-relative <code>{@value #Path}</code> path.</p>
 *
 * <p>Endpoint behaviour may be fine tuned with custom <a href="/modules/com.metreeca:tray/0.0/references/configuration#queryupdate">configuration
 * properties</a>.</p>
 *
 * <p>Query operations are restricted to users in the {@linkplain Spec#root root} {@linkplain Request#roles()
 * role} , unless otherwise specified through configuration properties; update operations are restricted to users in the
 * root role.</p>
 *
 * @see <a href="http://www.w3.org/TR/sparql11-http-rdf-update">SPARQL 1.1 Graph Store HTTP Protocol</a>
 */
public final class Graphs implements Service {

	public static final String Path="/graphs";


	private static final Shape GraphsShape=trait(RDF.VALUE, and(
			trait(RDF.TYPE, only(VOID.DATASET))
	));


	private final Setup setup=tool(Setup.Tool);
	private final Index index=tool(Index.Tool);
	private final Graph graph=tool(Graph.Tool);
	private final Trace trace=tool(Trace.Tool);

	private final boolean publik=setup.get("graphs.public", false); // public availability of the endpoint


	@Override public void load() {
		index.insert(Path, dispatcher()

				.get(this::get)
				.put(this::put)
				.delete(this::delete)
				.post(this::post));

		// !!! port metadata

		//map(
		//
		//		entry(RDFS.LABEL, literal("SPARQL 1.1 Graph Store Endpoint"))
		//
		//)
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-get
	 */
	private void get(final Request request, final Response response) {
		final boolean catalog=request.query().isEmpty();

		final String target=graph(request);
		final Iterable<String> accept=request.headers("Accept");

		if ( target == null && !catalog ) {

			response.status(Response.BadRequest).json(error(
					"parameter-missing",
					"missing target graph parameter"
			));

		} else if ( !publik && !request.role(Spec.root) ) {

			refused(request, response);

		} else if ( catalog ) { // graph catalog

			final IRI focus=request.focus();
			final Collection<Statement> model=new ArrayList<>();

			try (
					final RepositoryConnection connection=graph.connect();
					final RepositoryResult<Resource> contexts=connection.getContextIDs()
			) {
				while ( contexts.hasNext() ) {

					final Resource context=contexts.next();

					model.add(statement(focus, RDF.VALUE, context));
					model.add(statement(context, RDF.TYPE, VOID.DATASET));

				}
			}

			response.status(Response.OK).rdf(model, GraphsShape);

		} else {

			final RDFWriterFactory factory=Formats.service(
					RDFWriterRegistry.getInstance(), RDFFormat.TURTLE, accept);

			final RDFFormat format=factory.getRDFFormat();

			final Resource context=target.isEmpty() ? null : iri(target);

			try (final RepositoryConnection connection=graph.connect()) {
				response.status(Response.OK)

						.header("Content-Type", format.getDefaultMIMEType())
						.header("Content-Disposition", "attachment; filename=\"%s.%s\"",
								target.isEmpty() ? "default" : target, format.getDefaultFileExtension())

						.output(stream -> connection.export(factory.getWriter(stream), context));

			}
		}
	}

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-put
	 */
	private void put(final Request request, final Response response) {

		final String target=graph(request);

		if ( target == null ) {

			response.status(Response.BadRequest).json(error("parameter-missing", "missing target graph parameter"));

		} else if ( !request.role(Spec.root) ) {

			refused(request, response);

		} else {

			final Resource context=target.isEmpty() ? null : iri(target);
			final Iterable<String> content=request.headers("Content-Type");

			// !!! If a clients issues a POST or PUT with a content type that is not understood by the
			// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

			final RDFParserFactory factory=Formats.service( // !!! review fallback handling
					RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);

			try (
					final RepositoryConnection connection=graph.connect();
					final InputStream input=request.input();
			) {

				final boolean exists=exists(connection, context);

				connection.clear(context);
				connection.add(input, request.base(), factory.getRDFFormat(), context);

				response.status(exists ? Response.NoContent : Response.Created).done();

			} catch ( final IOException e ) {

				trace.warning(this, "unable to read RDF payload", e);

				response.status(Response.InternalServerError).cause(e).json(error(
						"payload-unreadable", "I/O while reading RDF payload: see server logs for more detail"
				));

			} catch ( final RDFParseException e ) {

				trace.warning(this, "malformed RDF payload", e);

				response.status(Response.BadRequest).cause(e).json(error("payload-malformed",
						"malformed RDF payload: "+e.getLineNumber()+","+e.getColumnNumber()+") "+e.getMessage()));

			} catch ( final RepositoryException e ) {

				trace.warning(this, "unable to update graph "+context, e);

				response.status(Response.InternalServerError).cause(e).json(error(
						"update-aborted", "unable to update graph: see server logs for more detail"));

			}

		}
	}

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-delete
	 */
	private void delete(final Request request, final Response response) {

		final String target=graph(request);

		if ( target == null ) {

			response.status(Response.BadRequest).json(error("parameter-missing", "missing target graph parameter"));

		} else if ( !request.role(Spec.root) ) {

			refused(request, response);

		} else {

			final Resource context=target.isEmpty() ? null : iri(target);

			try (final RepositoryConnection connection=graph.connect()) {

				final boolean exists=exists(connection, context);

				connection.clear(context);

				response.status(exists ? Response.NoContent : Response.NotFound).done();

			} catch ( final RepositoryException e ) {

				trace.warning(this, "unable to update graph "+context, e);

				response.status(Response.InternalServerError).json(error(
						"update-aborted", "unable to delete graph: see server logs for more detail"
				));

			}

		}

	}

	/*
	 * https://www.w3.org/TR/sparql11-http-rdf-update/#http-post
	 */
	private void post(final Request request, final Response response) {

		// !!! support  "multipart/form-data"
		// !!! support graph creation with IRI identifying the underlying Graph Store

		final String target=graph(request);

		if ( target == null ) {

			response.status(Response.BadRequest).json(error("parameter-missing", "missing target graph parameter"));

		} else if ( !request.role(Spec.root) ) {

			refused(request, response);

		} else {

			final Resource context=target.isEmpty() ? null : iri(target);
			final Iterable<String> content=request.headers("Content-Type");

			// !!! If a clients issues a POST or PUT with a content type that is not understood by the
			// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

			final RDFParserFactory factory=Formats.service( // !!! review fallback handling
					RDFParserRegistry.getInstance(), RDFFormat.TURTLE, content);

			try (
					final RepositoryConnection connection=graph.connect();
					final InputStream input=request.input()
			) {

				final boolean exists=exists(connection, context);

				connection.add(input, request.base(), factory.getRDFFormat(), context);


				response.status(exists ? Response.NoContent : Response.Created).done();

			} catch ( final IOException e ) {

				trace.warning(this, "unable to read RDF payload", e);

				response.status(Response.InternalServerError).json(error("payload-unreadable", "I/O while reading RDF payload: see server logs for more detail"));

			} catch ( final RDFParseException e ) {

				trace.warning(this, "malformed RDF payload", e);

				response.status(Response.BadRequest).json(error("payload-malformed", "malformed RDF payload: "
						+e.getLineNumber()+","+e.getColumnNumber()+") "+e.getMessage()));

			} catch ( final RepositoryException e ) {

				trace.warning(this, "unable to update graph "+context, e);

				response.status(Response.InternalServerError).json(error("update-aborted", "unable to update graph: see server logs for more detail"));

			}

		}

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
