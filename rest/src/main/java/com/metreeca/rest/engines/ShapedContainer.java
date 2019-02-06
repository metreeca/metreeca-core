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

import com.metreeca.form.*;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;

import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.rest.engines.Flock.flock;
import static com.metreeca.rest.wrappers.Throttler.resource;


/**
 * Shape-driven container engine.
 *
 * <p>Manages CRUD lifecycle operations on container resource descriptions defined by a shape.</p>
 */
final class ShapedContainer extends GraphEntity {

	private final Graph graph;
	private final Flock flock;

	private final Shape relate;
	private final Shape create;


	ShapedContainer(final Graph graph, final Shape shape) {

		final Shape resource=resource().apply(shape);

		this.graph=graph;
		this.flock=flock(metas(resource)).orElseGet(Flock.None::new);

		this.relate=redact(resource, Form.relate, Form.digest);
		this.create=redact(resource, Form.create, Form.detail);
	}


	@Override public Optional<Collection<Statement>> relate(final IRI resource) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {
		return graph.update(connection -> {

			return reserve(connection, related).map(reserved -> {

				flock.insert(connection, resource, related, model).add(model);

				// !!! validate before altering the db (snapshot isolation)

				final Focus focus=validate(connection, related, create, model);

				if ( focus.assess(Issue.Level.Error) ) {
					connection.rollback();
				} else {
					connection.commit();
				}

				return focus;

			});

		});
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		throw new UnsupportedOperationException("shaped container updating not supported");
	}

	@Override public Optional<IRI> delete(final IRI resource) {
		throw new UnsupportedOperationException("shaped container deletion not supported");
	}

}
