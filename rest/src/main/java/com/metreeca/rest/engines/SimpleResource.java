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
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.rest.engines.Descriptions.description;
import static com.metreeca.rest.engines.Flock.flock;

import static java.util.stream.Collectors.toList;


/**
 * Concise bounded description resource engine.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded resource descriptions.</p>
 */
 final class SimpleResource extends GraphEntity {

	private final Graph graph;
	private final Flock flock;


	SimpleResource(final Graph graph, final Map<IRI, Value> metadata) {
		this.graph=graph;
		this.flock=flock(metadata).orElseGet(Flock.None::new);
	}


	@Override public Optional<Collection<Statement>> relate(final IRI resource) {
		return graph.query(connection -> { return retrieve(connection, resource, true); });
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {
		throw new UnsupportedOperationException("simple related resource creation not supported");
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		return graph.update(connection -> {

			return retrieve(connection, resource, false).map(current -> {

				final Focus focus=validate(resource, model);

				if ( !focus.assess(Issue.Level.Error) ) {

					connection.remove(current);
					connection.add(model);

				}

				return focus;

			});

		});
	}

	@Override public Optional<IRI> delete(final IRI resource) {

		if ( resource == null ) {
			throw new NullPointerException("null item");
		}

		return graph.update(connection -> {

			return retrieve(connection, resource, false).map(current -> {

				flock.remove(connection, resource, current).remove(current);

				return resource;

			});

		});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Collection<Statement>> retrieve(
			final RepositoryConnection connection, final Resource resource, final boolean labelled
	) {
		return Optional.of(description(resource, labelled, connection)).filter(statements -> !statements.isEmpty());
	}

	private Focus validate(final Resource resource, final Collection<Statement> model) {

		final Collection<Statement> envelope=description(resource, false, model);

		return focus(model.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> issue(Issue.Level.Error, "statement outside description envelope "+outlier))
				.collect(toList())
		);
	}

}
