/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Optional;
import java.util.function.BiFunction;

import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.rdf.Graph.graph;

import static java.util.Objects.requireNonNull;


/**
 * Resource aliaser.
 *
 * <p>Redirects request for aliased resources to their canonical identifier.</p>
 *
 * <p>May be used as either a {@linkplain Wrapper wrapper} or a {@linkplain Handler handler}: {@linkplain
 * #Aliaser(BiFunction) unresolved} resources are:</p>
 *
 * <ul>
 * <li>delegated to the wrapped handler, if used as a wrapper;</li>
 * <li>reported with a {@link Response#NotFound} status code, if used as a handler.</li>
 * </ul>
 */
public final class Aliaser implements Wrapper, Handler {

	private final BiFunction<RepositoryConnection, Request, Optional<Resource>> resolver;

	private final Graph graph=tool(graph());


	/**
	 * Creates a resource aliaser.
	 *
	 * @param resolver the resource resolving function; takes as argument a connection to the shared system {@linkplain
	 *                 Graph} and a request and returns the canonical resource for the aliased request {@linkplain
	 *                 Request#item() item}, if one was identified, or an empty optional, otherwise
	 *
	 * @throws NullPointerException if {@code resolver} is null or returns a null value
	 */
	public Aliaser(final BiFunction<RepositoryConnection, Request, Optional<Resource>> resolver) {

		if ( resolver == null ) {
			throw new NullPointerException("null resolver");
		}

		this.resolver=resolver;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> alias(request).orElseGet(() -> handler.handle(request));
	}

	@Override public Responder handle(final Request request) {
		return alias(request).orElseGet(() -> request.reply(response -> response.status(Response.NotFound)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Responder> alias(final Request request) {
		return graph

				.query(connection -> {

					return requireNonNull(

							resolver.apply(connection, request), "null resolver return value"

					).filter(resource -> resource instanceof IRI);

				})

				.map(resource -> request.reply(response -> response
						.status(Response.SeeOther)
						.header("Location", resource.stringValue()))
				);
	}

}
