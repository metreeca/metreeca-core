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

package com.metreeca.rest.flavors;

import com.metreeca.form.Focus;
import com.metreeca.form.Issue;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;

import static java.util.stream.Collectors.toList;


/**
 * Concise bounded resource description manager.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded resource descriptions.</p>
 */
public final class SimpleResource extends SimpleEntity {

	private final RepositoryConnection connection;


	/**
	 * Creates a concise bounded resource description manager
	 *
	 * @param connection a connection to the repository where resource description are stored.
	 *
	 * @throws NullPointerException if {@code connection} is null
	 */
	public SimpleResource(final RepositoryConnection connection) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		this.connection=connection;
	}


	@Override public Collection<Statement> relate(final IRI entity) {

		if ( entity == null ) {
			throw new NullPointerException("null item");
		}

		return description(entity, true, connection);
	}

	/**
	 * {@inheritDoc} {Unsupported}
	 */
	@Override public Focus create(final IRI entity, final Collection<Statement> model) {
		throw new UnsupportedOperationException("simple connected resource creation not supported");
	}

	/**
	 * @return {@inheritDoc}; includes {@linkplain Issue.Level#Error errors} if {@code model} contains statements
	 * outside the symmetric concise bounded description of {@code entity}
	 */
	@Override public Focus update(final IRI entity, final Collection<Statement> model) {

		if ( entity == null ) {
			throw new NullPointerException("null item");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		final Model envelope=description(entity, false, model);

		final Focus focus=focus(model.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> issue(Issue.Level.Error, "statement outside cell envelope "+outlier))
				.collect(toList())
		);

		if ( !focus.assess(Issue.Level.Error) ) {

			connection.remove(description(entity, false, connection));
			connection.add(model);

		}

		return focus;
	}

	@Override public boolean delete(final IRI entity) {

		if ( entity == null ) {
			throw new NullPointerException("null item");
		}

		if ( !connection.hasStatement(entity, null, null, true)
				&& !connection.hasStatement(null, null, entity, true) ) {

			return false;

		} else {

			connection.remove(description(entity, false, connection));

			return true;

		}

	}

}
