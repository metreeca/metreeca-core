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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Optional;


/**
 * Concise bounded container description manager.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded container descriptions.</p>
 */
public final class SimpleContainer implements Engine {

	private final RepositoryConnection connection;


	/**
	 * Creates a concise bounded container description manager
	 *
	 * @param connection a connection to the repository where container description are stored.
	 *
	 * @throws NullPointerException if {@code connection} is null
	 */

	public SimpleContainer(final RepositoryConnection connection) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		this.connection=connection;
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
