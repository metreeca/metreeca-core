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

package com.metreeca.stardog;

import com.metreeca.rdf.services.Graph;

import com.complexible.stardog.api.ConnectionConfiguration;
import com.complexible.stardog.rdf4j.StardogRepository;
import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;

import static com.metreeca.tree.things.Values.iri;

import static org.eclipse.rdf4j.IsolationLevels.READ_COMMITTED;


/**
 * Stardog graph store.
 *
 * <p>Manages task execution on an <a href="https://www.stardog.com/docs/">Stardog</a> repository.</p>
 *
 * <p><strong>Warning</strong> / the {@linkplain #isolation(IsolationLevel) isolation level} is set to {@link
 * IsolationLevels#READ_COMMITTED}, unless explicitly overridden during tray configuration, like:</p>
 *
 * <pre>{@code
 * tray.set(Graph.Factory, () -> new Stardog(…).isolation(IsolationLevels.NONE);
 * }</pre>
 *
 * <p>Note that this limitation seems to be specific of the RDF4J interface, as at the repository configuration level
 * available transaction options are SNAPSHOT and SERIALIZABLE…</p>
 */
public final class Stardog extends Graph {

	/**
	 * The IRI of the default context.
	 */
	public static final IRI Default=iri("tag:stardog:api:context:default");


	{ isolation(READ_COMMITTED); } // ;( limited transaction support through RDF4J


	/**
	 * Creates a Stardog graph.
	 *
	 * @param url the <a href="https://www.stardog.com/docs/#_how_to_make_a_connection_string">connection string</a> for
	 *            a remote Stardog server (e.g. {@code http://localhost:5820/<database>})
	 *
	 * @throws NullPointerException if {@code url} is null
	 */
	public Stardog(final String url) {
		this(ConnectionConfiguration.from(url));
	}

	/**
	 * Creates a Stardog graph.
	 *
	 * @param configuration the <a href="https://www.stardog.com/docs/#_creating_a_connection_string">connection
	 *                      configuration</a> for a remote Stardog server
	 *
	 * @throws NullPointerException if {@code configuration} is {@code null}
	 * @see ConnectionConfiguration#from(String)
	 */
	public Stardog(final ConnectionConfiguration configuration) {

		if ( configuration == null ) {
			throw new NullPointerException("null configuration");
		}

		repository(new StardogRepository(configuration));
	}

}
