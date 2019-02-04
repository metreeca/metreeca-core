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
import com.metreeca.form.Issue;
import com.metreeca.rest.Engine;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.rest.engines.Descriptions.description;

import static java.util.stream.Collectors.toList;


/**
 * Concise bounded resource description manager.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded resource descriptions.</p>
 */
public final class SimpleResource implements Engine {

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


	@Override public Optional<Collection<Statement>> relate(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null item");
		}

		return Optional.of(description(resource, true, connection)).filter(statements -> !statements.isEmpty());
	}

	/**
	 * {@inheritDoc} {Unsupported}
	 */
	@Override public Optional<Focus> create(final IRI resource, final IRI slug, final Collection<Statement> model) {
		throw new UnsupportedOperationException("simple related resource creation not supported");
	}

	/**
	 * @return {@inheritDoc}; includes {@linkplain Issue.Level#Error errors} if {@code model} contains statements
	 * outside the symmetric concise bounded description of {@code entity}
	 */
	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null item");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		final Collection<Statement> envelope=description(resource, false, model);

		if ( envelope.isEmpty() ) {

			return Optional.empty();

		} else {

			final Focus focus=focus(model.stream()
					.filter(statement -> !envelope.contains(statement))
					.map(outlier -> issue(Issue.Level.Error, "statement outside cell envelope "+outlier))
					.collect(toList())
			);

			if ( !focus.assess(Issue.Level.Error) ) {

				connection.remove(description(resource, false, connection));
				connection.add(model);

			}

			return Optional.of(focus);

		}
	}

	@Override public Optional<IRI> delete(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null item");
		}


		final Collection<Statement> description=description(resource, false, connection);

		if ( description.isEmpty() ) {

			return Optional.empty();

		} else {

			connection.remove(description);

			return Optional.of(resource);

		}

	}

}
