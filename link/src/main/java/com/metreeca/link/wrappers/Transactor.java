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

package com.metreeca.link.wrappers;

import com.metreeca.link.Handler;
import com.metreeca.link.Wrapper;
import com.metreeca.spec.Spec;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.repository.RepositoryConnection;

import static com.metreeca.link.Handler.refused;
import static com.metreeca.spec.sparql.SPARQLEngine.transactional;


/**
 * SPARQL transaction manager
 *
 * <p>Executes the wrapped handler inside a SPARQL transaction on the connection provided by the shared {@linkplain
 * Graph#Tool graph} tool.</p>
 */
public final class Transactor implements Wrapper {

	public static Transactor transactor(final RepositoryConnection connection) {
		return transactor(connection, false);
	}

	public static Transactor transactor(final RepositoryConnection connection, final boolean conditional) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		return new Transactor(connection, conditional);
	}


	private final RepositoryConnection connection;

	private final boolean conditional;


	private Transactor(final RepositoryConnection connection, final boolean conditional) {
		this.connection=connection;
		this.conditional=conditional;
	}


	@Override public Handler wrap(final Handler handler) {
		return (request, response) -> {
			if ( !transactional(connection) && !request.role(Spec.root) ) { refused(request, response); } else {

				if ( !connection.isActive() ) {

					handler.handle(

							writer -> {

								connection.begin();

								writer.copy(request).done();

							},

							reader -> {

								if ( reader.success() ) {
									connection.commit();
								} else {
									connection.rollback();
								}

								response.copy(reader).done();

							}

					);

				} else if ( conditional ) {

					handler.handle(request, response);

				} else {

					throw new IllegalStateException("transaction already active on connection");

				}

			}
		};
	}
}
