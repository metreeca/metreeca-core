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

package com.metreeca.rdf4j;

import com.metreeca.sparql.services.Graph;

import org.eclipse.rdf4j.http.client.RDF4JProtocolSession;
import org.eclipse.rdf4j.repository.http.HTTPRepository;

import java.util.function.Consumer;


/**
 * RDF4J remote graph store.
 *
 * <p>Manages task execution on a remote RDF4J {@link HTTPRepository}.</p>
 */
public final class RDF4JRemote extends Graph {

	private Consumer<RDF4JProtocolSession> customizer=session -> {};


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

		repository(new HTTPRepository(url){

			@Override protected RDF4JProtocolSession createHTTPClient() {

				final RDF4JProtocolSession session=super.createHTTPClient();

				customizer.accept(session);

				return session;
			}

		});
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


	/**
	 * Configures the RDF4J session customizer.
	 *
	 * @param customizer the customizer for the RDF4J session; takes as argument the RDF4J session to be customized
	 *
	 * @return this graph store
	 *
	 * @throws NullPointerException if {@code customizer} is {@code null}
	 */
	public RDF4JRemote session(final Consumer<RDF4JProtocolSession> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		this.customizer=customizer;

		return this;
	}

}
