/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.engines;

import com.metreeca.form.Focus;
import com.metreeca.rest.Engine;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;


/**
 * Concise bounded description container engine.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded container descriptions.</p>
 */
 final class SimpleContainer implements Engine {


	private Graph graph;

	/**
	 * Creates a concise bounded description container engine.
	 *
	 * @param graph a connection to the repository where container description are stored
	 *
	 * @throws NullPointerException if {@code connection} is null
	 */
	public SimpleContainer(final Graph graph) {

		if ( graph == null ) {
			throw new NullPointerException("null connection");
		}

		this.graph=graph;
	}


	@Override public Optional<Collection<Statement>> relate(final IRI resource) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI slug, final Collection<Statement> model) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	/**
	 * {@inheritDoc} {Unsupported}
	 */
	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		throw new UnsupportedOperationException("simple container updating not supported");
	}

	/**
	 * {@inheritDoc} {Unsupported}
	 */
	@Override public Optional<IRI> delete(final IRI resource) {
		throw new UnsupportedOperationException("simple container deletion not supported");
	}

}
