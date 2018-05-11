/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.rdf.graphs;

import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;

import java.io.File;


public final class RDF4JMemory extends Graph {

	public static final Tool<Graph> Tool=tools -> {

		final Setup setup=tools.get(Setup.Tool);

		final boolean persistent=setup.get("graph.memory.persistent", false);
		final File storage=setup.get("graph.memory.storage", storage(setup));

		return persistent ? new RDF4JMemory(storage) : new RDF4JMemory();
	};


	public RDF4JMemory() {
		super("RDF4J Memory Store (Transient)", IsolationLevels.SERIALIZABLE, () ->
				new SailRepository(new MemoryStore()));
	}

	public RDF4JMemory(final File storage) {
		super("RDF4J Memory Store (Persistent)", IsolationLevels.SERIALIZABLE, () -> {

			if ( storage == null ) {
				throw new NullPointerException("null storage");
			}

			if ( storage.exists() && storage.isFile() ) {
				throw new IllegalArgumentException("plain file at storage folder path ["+storage+"]");
			}

			return new SailRepository(new MemoryStore(storage));
		});
	}

}
