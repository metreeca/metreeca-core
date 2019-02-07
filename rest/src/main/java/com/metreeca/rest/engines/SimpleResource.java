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
import com.metreeca.form.things.Sets;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.metreeca.rest.engines.Flock.flock;


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


	@Override public Collection<Statement> relate(final IRI resource) {
		return graph.query(connection -> { return retrieve(connection, resource, true).orElseGet(Sets::set); });
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

}
