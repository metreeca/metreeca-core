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

import com.metreeca.rdf.services.Graph;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.http.client.SPARQLProtocolSession;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sparql.SPARQLRepository;

import java.util.function.Consumer;

import static org.eclipse.rdf4j.IsolationLevels.NONE;


/**
 * RDF4J SPARQL graph store.
 *
 * <p>Manages task execution on an RDF4J {@link SPARQLRepository}.</p>
 *
 * <p><strong>Warning</strong> / the {@linkplain Graph#isolation(IsolationLevel) isolation level} is set to {@code
 * IsolationLevels.NONE}; setting the isolation level to anything different will cause a runtime exception on update
 * transactions.</p>
 */
public final class RDF4JSPARQL extends Graph { // ;( namespace ops silently ignored

	private Consumer<SPARQLProtocolSession> customizer=session -> {};


	/**
	 * Creates an RDF4J SPARQL graph.
	 *
	 * @param url the URL of a remote SPARQL endpoint supporting <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL
	 *            1.1 Protocol</a> query/update operations
	 *
	 * @throws NullPointerException if {@code url} is null
	 */
	public RDF4JSPARQL(final String url) {
		this(url, url);
	}

	/**
	 * Creates an RDF4J SPARQL graph.
	 *
	 * @param query  the URL of a remote SPARQL endpoint supporting <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL
	 *               1.1 Protocol</a> query operations
	 * @param update the URL of a remote SPARQL endpoint supporting <a href="https://www.w3.org/TR/sparql11-protocol/">SPARQL
	 *               1.1 Protocol</a> update operations
	 *
	 * @throws NullPointerException if either {@code query} or {@code update} is null
	 */
	public RDF4JSPARQL(final String query, final String update) {

		if ( query == null ) {
			throw new NullPointerException("null query endpoint URL");
		}

		if ( update == null ) {
			throw new NullPointerException("null update endpoint URL");
		}

		repository(new SPARQLRepository(query, update) {

			@Override protected SPARQLProtocolSession createHTTPClient() {

				final SPARQLProtocolSession session=super.createHTTPClient();

				customizer.accept(session);

				return session;
			}

			@Override public RepositoryConnection getConnection() throws RepositoryException {
				return new RDF4JSPARAQLConnection(super.getConnection());
			}

		}).isolation(NONE); // ;( no transaction support
	}


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
	public RDF4JSPARQL credentials(final String usr, final String pwd) {

		if ( usr == null ) {
			throw new NullPointerException("null usr");
		}

		if ( pwd == null ) {
			throw new NullPointerException("null pwd");
		}

		final SPARQLRepository repository=(SPARQLRepository)repository();

		if ( repository.isInitialized() ) {
			throw new IllegalStateException("active repository");
		}

		if ( !usr.isEmpty() || !pwd.isEmpty() ) {
			repository.setUsernameAndPassword(usr, pwd);
		}

		return this;
	}


	/**
	 * Configures the SPARQL session customizer.
	 *
	 * @param customizer the customizer for the SPARQL session; takes as argument the SPARQL session to be customized
	 *
	 * @return this graph store
	 *
	 * @throws NullPointerException if {@code customizer} is {@code null}
	 */
	public RDF4JSPARQL session(final Consumer<SPARQLProtocolSession> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		this.customizer=customizer;

		return this;
	}

}
