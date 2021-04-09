/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rdf4j.handlers;

import com.metreeca.json.Shape;
import com.metreeca.json.Values;
import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.VOID;
import org.eclipse.rdf4j.repository.*;
import org.eclipse.rdf4j.rio.*;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.function.Supplier;

import static com.metreeca.json.Shape.exactly;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.statement;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rest.Format.mimes;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.BadRequest;
import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.Xtream.task;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.handlers.Router.router;

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

	private static final Shape GraphsShape=field(RDF.VALUE,
			field(RDF.TYPE), exactly(VOID.DATASET)
	);


	/**
	 * Creates a graph store endpoint
	 *
	 * @return a new graph store endpoint
	 */
	public static Graphs graphs() {
		return new Graphs();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Graphs() {
		delegate(router()
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
			final String accept=request.header("Accept").orElse("");

			if ( target == null && !catalog ) {

				request.reply(status(BadRequest, "missing target graph parameter")).accept(consumer);

			} else if ( !queryable(request.roles()) ) {

				request.reply(response -> response.status(Response.Unauthorized)).accept(consumer);

			} else if ( catalog ) { // graph catalog

				final IRI focus=iri(request.item());
				final Collection<Statement> model=new ArrayList<>();

				graph().query(task(connection -> {
					try ( final RepositoryResult<Resource> contexts=connection.getContextIDs() ) {
						while ( contexts.hasNext() ) {

							final Resource context=contexts.next();

							model.add(statement(focus, RDF.VALUE, context));
							model.add(statement(context, RDF.TYPE, VOID.DATASET));

						}
					}
				}));

				request.reply(response -> response.status(Response.OK)
						.set(JSONLDFormat.shape(), GraphsShape)
						.body(rdf(), model)
				).accept(consumer);

			} else {

				final RDFWriterFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
						RDFWriterRegistry.getInstance(), RDFFormat.TURTLE, mimes(accept)
				);

				final RDFFormat format=factory.getRDFFormat();

				final Resource context=target.isEmpty() ? null : iri(target);

				graph().query(task(connection -> {
					request.reply(response -> response.status(Response.OK)

							.header("Content-Type", format.getDefaultMIMEType())
							.header("Content-Disposition", format("attachment; filename=\"%s.%s\"",
									target.isEmpty() ? "default" : target, format.getDefaultFileExtension()
							))

							.body(output(), output -> { connection.export(factory.getWriter(output), context); })

					).accept(consumer);
				}));
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

				request.reply(status(BadRequest, "missing target graph parameter")).accept(consumer);

			} else if ( !updatable(request.roles()) ) {

				request.reply(response -> response.status(Response.Unauthorized)).accept(consumer);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);
				final String content=request.header("Content-Type").orElse("");

				// !!! If a clients issues a POST or PUT with a content type that is not understood by the
				// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

				final RDFParserFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
						RDFParserRegistry.getInstance(), RDFFormat.TURTLE, mimes(content) // !!! review fallback
						// handling
				);

				graph().update(task(connection -> { // binary format >> no rewriting
					try ( final InputStream input=request.body(input()).fold(e -> Xtream.input(), Supplier::get) ) {

						final boolean exists=exists(connection, context);

						connection.clear(context);
						connection.add(input, request.base(), factory.getRDFFormat(), context);

						request.reply(response ->
								response.status(exists ? Response.NoContent :
										Response.Created)
						).accept(consumer);

					} catch ( final IOException e ) {

						logger().warning(this, "unable to read RDF payload", e);

						request.reply(status(InternalServerError, e)).accept(consumer);

					} catch ( final RDFParseException e ) {

						logger().warning(this, "malformed RDF payload", e);

						request.reply(status(BadRequest, e)).accept(consumer);

					} catch ( final RepositoryException e ) {

						logger().warning(this, "unable to update graph "+context, e);

						request.reply(status(InternalServerError, e)).accept(consumer);

					}
				}));
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

				request.reply(status(BadRequest, "missing target graph parameter")).accept(consumer);

			} else if ( !updatable(request.roles()) ) {

				request.reply(response -> response.status(Response.Unauthorized)).accept(consumer);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);

				graph().update(task(connection -> {
					try {

						final boolean exists=exists(connection, context);

						connection.clear(context);

						request.reply(response ->
								response.status(exists ? Response.NoContent :
										Response.NotFound)
						).accept(consumer);

					} catch ( final RepositoryException e ) {

						logger().warning(this, "unable to update graph "+context, e);

						request.reply(status(InternalServerError, e)).accept(consumer);

					}
				}));
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

				request.reply(status(BadRequest, "missing target graph parameter")).accept(consumer);

			} else if ( !updatable(request.roles()) ) {

				request.reply(response -> response.status(Response.Unauthorized)).accept(consumer);

			} else {

				final Resource context=target.isEmpty() ? null : iri(target);
				final String content=request.header("Content-Type").orElse("");

				// !!! If a clients issues a POST or PUT with a content type that is not understood by the
				// !!! graph store, the implementation MUST respond with 415 Unsupported Media Type.

				final RDFParserFactory factory=com.metreeca.rdf.formats.RDFFormat.service(
						RDFParserRegistry.getInstance(), RDFFormat.TURTLE, mimes(content) // !!! review fallback
				);

				graph().update(task(connection -> { // binary format >> no rewriting
					try ( final InputStream input=request.body(input()).fold(e -> Xtream.input(),
							Supplier::get) ) {

						final boolean exists=exists(connection, context);

						connection.add(input, request.base(), factory.getRDFFormat(), context);

						request.reply(response ->
								response.status(exists ? Response.NoContent : Response.Created)
						).accept(consumer);

					} catch ( final IOException e ) {

						logger().warning(this, "unable to read RDF payload", e);

						request.reply(status(InternalServerError, e)).accept(consumer);

					} catch ( final RDFParseException e ) {

						logger().warning(this, "malformed RDF payload", e);

						request.reply(status(BadRequest, e)).accept(consumer);

					} catch ( final RepositoryException e ) {

						logger().warning(this, "unable to update graph "+context, e);

						request.reply(status(InternalServerError, e)).accept(consumer);

					}
				}));

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

		try ( final RepositoryResult<Resource> contexts=connection.getContextIDs() ) {

			while ( contexts.hasNext() ) {
				if ( contexts.next().equals(context) ) { return true; }
			}

		}

		return connection.hasStatement(null, null, null, true, context);
	}

}
