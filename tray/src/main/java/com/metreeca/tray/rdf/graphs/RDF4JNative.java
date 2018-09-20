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

package com.metreeca.tray.rdf.graphs;

import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys._Setup;

import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.nativerdf.NativeStore;

import java.io.File;
import java.util.function.Supplier;

import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.IsolationLevels.SERIALIZABLE;


public final class RDF4JNative extends Graph {

	public static final Supplier<Graph> Factory=() -> {

		final _Setup setup=tool(_Setup.Factory);

		final File storage=setup.get("graph.native.storage", storage(setup));

		return new RDF4JNative(storage);
	};


	public RDF4JNative(final File storage) {
		super(IsolationLevels.SNAPSHOT, () -> { // !!! SERIALIZABLE breaks persistence

			if ( storage == null ) {
				throw new NullPointerException("null storage");
			}

			if ( storage.exists() && storage.isFile() ) {
				throw new IllegalArgumentException("plain file at storage folder path ["+storage+"]");
			}

			return new SailRepository(new NativeStore(storage));

		});
	}


	@Override public RepositoryConnection connect(final IsolationLevel isolation) {

		if ( isolation == null ) {
			throw new NullPointerException("null isolation");
		}

		final boolean serializable=isolation.isCompatibleWith(SERIALIZABLE);

		// !!! ;(rdf4j) fall back to default isolation if trying to activate SERIALIZABLE (leaks memory like a sieve…)
		// see https://github.com/eclipse/rdf4j/issues/1031

		return serializable ? connect() : super.connect(isolation);
	}

}
