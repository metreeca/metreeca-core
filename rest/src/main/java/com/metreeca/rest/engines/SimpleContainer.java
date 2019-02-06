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
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.metreeca.rest.engines.Flock.flock;


/**
 * Concise bounded description container engine.
 *
 * <p>Manages CRUD lifecycle operations on (labelled) symmetric concise bounded container descriptions.</p>
 */
final class SimpleContainer extends GraphEntity {

	private final Graph graph;
	private final Flock flock;

	private final Engine delegate;


	SimpleContainer(final Graph graph, final Map<IRI, Value> metadata) {

		this.graph=graph;
		this.flock=flock(metadata).orElseGet(Flock.Basic::new);

		this.delegate=new SimpleResource(graph, metadata);
	}


	@Override public Optional<Collection<Statement>> relate(final IRI resource) {
		return delegate.relate(resource);
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {
		return graph.update(connection -> {

			return reserve(connection, related).map(reserved -> {

				final Focus focus=validate(related, model);

				if ( !focus.assess(Issue.Level.Error) ) {
					flock.insert(connection, resource, reserved, model).add(model);
				}

				return focus;

			});

		});
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		throw new UnsupportedOperationException("simple container updating not supported");
	}

	@Override public Optional<IRI> delete(final IRI resource) {
		throw new UnsupportedOperationException("simple container deletion not supported");
	}

}
