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

import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import java.io.File;


/**
 * RDF4J native graph store.
 *
 * <p>Manages task execution on an RDF4J {@link SailRepository} backed by a {@link NativeStore}.</p>
 */
public final class RDF4JNative extends Graph {

	// ;(rdf4j) SERIALIZABLE isolation level prevents transaction commitments and leaks memory like a sieve…
	// https://github.com/eclipse/rdf4j/issues/1031


	/**
	 * Creates an RDF4J native graph.
	 *
	 * @param storage the storage folder where the graph is to be persisted
	 *
	 * @throws NullPointerException     if {@code storage} is null
	 * @throws IllegalArgumentException if {@code storage} is not a folder
	 */
	public RDF4JNative(final File storage) {

		if ( storage == null ) {
			throw new NullPointerException("null storage");
		}

		if ( storage.exists() && !storage.isDirectory() ) {
			throw new IllegalArgumentException("plain file at storage folder path ["+storage+"]");
		}

		repository(new SailRepository(new NativeStore(storage)));
	}

}
