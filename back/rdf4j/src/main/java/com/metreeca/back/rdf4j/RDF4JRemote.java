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

package com.metreeca.back.rdf4j;

import com.metreeca.back.sparql.Graph;

import org.eclipse.rdf4j.repository.http.HTTPRepository;


/**
 * RDF4J remote graph store.
 *
 * <p>Manages task execution on a remote RDF4J {@link HTTPRepository}.</p>
 */
public final class RDF4JRemote extends Graph {

	/**
	 * Creates an RDF4J remote graph.
	 *
	 * @param url the URL of a remote RDF repository supporting the <a href="http://docs.rdf4j.org/rest-api/">RDF4J
	 *            Server REST API</a>
	 *
	 * @throws NullPointerException if {@code url} is null
	 */
	public RDF4JRemote(final String url) {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		repository(new HTTPRepository(url));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the credentials for accessing the remote RDF repository.
	 *
	 * @param usr the username of the account on the remote RDF repository
	 * @param pwd the password of the account on the remote RDF repository
	 *
	 * @return this graph store
	 *
	 * @throws NullPointerException  if either {@code usr} or {@code pwd} is {@code null}
	 * @throws IllegalStateException if the backing remote repository was already initialized
	 */
	public RDF4JRemote credentials(final String usr, final String pwd) {

		if ( usr == null ) {
			throw new NullPointerException("null usr");
		}

		if ( pwd == null ) {
			throw new NullPointerException("null pwd");
		}

		final HTTPRepository repository=(HTTPRepository)repository();

		if ( repository.isInitialized() ) {
			throw new IllegalStateException("active repository");
		}

		if ( !usr.isEmpty() || !pwd.isEmpty() ) {
			repository.setUsernameAndPassword(usr, pwd);
		}

		return this;
	}

}
