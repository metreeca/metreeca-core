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

package com.metreeca.link.handlers.ldp;

import com.metreeca.link.Handler;
import com.metreeca.link.Request;
import com.metreeca.link.Response;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.*;

import static com.metreeca.link.handlers.Dispatcher.dispatcher;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.singleton;


/**
 * Bounded description handler.
 *
 * <table>
 *
 * <tr>
 * <th>HTTP Method</th>
 * <th>Action</th>
 * </tr>
 *
 * <tr>
 * <td>GET</td>
 * <td>Returns the symmetric concise bounded description of the request focus resource; the description is extended
 * with
 * {@code rdfs:label/comment} annotations for all referenced IRIs</td>
 * </tr>
 * </table>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Description implements Handler {

	public static Description description() { return new Description(); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);

	private final Handler dispatcher=dispatcher().get(this::get);


	private Description() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void handle(final Request request, final Response response) {
		dispatcher.handle(request, response);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void get(final Request request, final Response response) { // !!! optimize for SPARQL

		final IRI focus=request.focus();
		final Collection<Statement> cell=new LinkedHashModel();

		try (final RepositoryConnection connection=graph.connect()) {

			final Queue<Value> pending=new ArrayDeque<>(singleton(focus));
			final Collection<Value> visited=new HashSet<>();

			while ( !pending.isEmpty() ) {

				final Value value=pending.remove();

				if ( visited.add(value) ) {
					if ( value.equals(focus) || value instanceof BNode ) {

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								(Resource)value, null, null, true
						)) {
							while ( statements.hasNext() ) {

								final Statement statement=statements.next();

								cell.add(statement);
								pending.add(statement.getObject());
							}
						}

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								null, null, value, true
						)) {
							while ( statements.hasNext() ) {

								final Statement statement=statements.next();

								cell.add(statement);
								pending.add(statement.getSubject());
							}
						}

					} else if ( value instanceof IRI ) {

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								(Resource)value, RDFS.LABEL, null, true
						)) {
							while ( statements.hasNext() ) { cell.add(statements.next()); }
						}

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								(Resource)value, RDFS.COMMENT, null, true
						)) {
							while ( statements.hasNext() ) { cell.add(statements.next()); }
						}

					}
				}

			}
		}

		response.status(Response.OK)

				.header("Link",
						"<http://www.w3.org/ns/ldp#RDFSource>; rel=\"type\"",
						"<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""
				)

				.rdf(cell);

	}

}
