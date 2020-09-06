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

package com.metreeca.rdf4j.assets;


import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.*;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;
import java.util.UUID;

import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.Values.statement;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.stream.Collectors.toList;


final class GraphCreator extends GraphProcessor {

	private final Graph graph=Context.asset(graph());


	Future<Response> handle(final Request request) {
		return request.collection() ? holder(request) : member(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> holder(final Request request) {
		return request

				.body(rdf())

				.process(rdf -> graph.exec(connection -> {

					final IRI holder=iri(request.item());
					final IRI member=iri(request.item()+request.header("Slug") // assign entity a slug-based id

							.map(text -> { // encode slug as IRI path component
								try {
									return URLEncoder.encode(text, UTF_8.name());
								} catch ( final UnsupportedEncodingException unexpected ) {
									throw new UncheckedIOException(unexpected);
								}
							})

							.orElseGet(() -> UUID.randomUUID().toString()) // !! sequential generator
					);

					final boolean clashing=connection.hasStatement(member, null, null, true)
							|| connection.hasStatement(null, null, member, true);

					if ( clashing ) { // report clashing slug

						return Error(status(InternalServerError,
								new IllegalStateException("clashing entity slug {"+member+"}")));

					} else { // store model

						connection.add(outline(member, filter(request.shape())));
						connection.add(rewrite(member, holder, rdf));

						return Value(request.reply(response -> response
								.status(Response.Created)
								.header("Location", member.stringValue()))
						);

					}

				}))

				.fold(future -> future, request::reply);
	}

	private Future<Response> member(final Request request) {
		return request.reply(status(InternalServerError, new UnsupportedOperationException("member POST "
				+"method")));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> rewrite(final IRI target, final IRI source, final Collection<Statement> model) {
		return model.stream().map(statement -> rewrite(target, source, statement)).collect(toList());
	}

	private Statement rewrite(final IRI target, final IRI source, final Statement statement) {
		return statement(
				rewrite(target, source, statement.getSubject()),
				rewrite(target, source, statement.getPredicate()),
				rewrite(target, source, statement.getObject()),
				rewrite(target, source, statement.getContext())
		);
	}

	private <T extends Value> T rewrite(final T target, final T source, final T value) {
		return source.equals(value) ? target : value;
	}

}
