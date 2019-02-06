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

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.rest.engines.Descriptions.description;
import static com.metreeca.rest.engines.Flock.flock;

import static java.util.stream.Collectors.toList;


/**
 * Concise bounded description container engine.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded container descriptions.</p>
 */
final class SimpleContainer extends GraphEntity {

	private final Graph graph;
	private final Flock flock;


	SimpleContainer(final Graph graph, final Map<IRI, Value> metadata) {
		this.graph=graph;
		this.flock=flock(metadata).orElseGet(Flock.Basic::new);
	}


	@Override public Optional<Collection<Statement>> relate(final IRI resource) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {
		return graph.update(connection -> {

			if ( lookup(connection, related) ) {

				return Optional.empty();

			} else {

				final Focus focus=validate(related, model);

				if ( !focus.assess(Issue.Level.Error) ) {
					flock.insert(connection, resource, related, model).add(model);
				}

				return Optional.of(focus);

			}

		});
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		throw new UnsupportedOperationException("simple container updating not supported");
	}

	@Override public Optional<IRI> delete(final IRI resource) {
		throw new UnsupportedOperationException("simple container deletion not supported");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Focus validate(final Resource resource, final Collection<Statement> model) {

		final Collection<Statement> envelope=description(resource, false, model);

		return focus(model.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> issue(Issue.Level.Error, "statement outside description envelope "+outlier))
				.collect(toList())
		);
	}

}
