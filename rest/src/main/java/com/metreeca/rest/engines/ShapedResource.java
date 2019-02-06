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


/**
 * Shape-driven resource engine.
 *
 * <p>Manages CRUD lifecycle operations on resource descriptions defined by a shape.</p>
 */
final class ShapedResource extends GraphEntity {

	private final Graph graph;
	private final Flock flock;

	private final Shape relate;
	private final Shape update;
	private final Shape delete;


	ShapedResource(final Graph graph, final Shape shape) {

		this.graph=graph;
		this.flock=flock(metas(shape)).orElseGet(Flock.None::new);

		this.relate=redact(shape, Form.relate, Form.detail);
		this.update=redact(shape, Form.update, Form.detail);
		this.delete=redact(shape, Form.delete, Form.detail);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Collection<Statement>> relate(final IRI resource) {
		return graph.query(connection -> { return retrieve(connection, resource, relate); });
	}

	@Override public Optional<Focus> create(final IRI resource, final IRI related, final Collection<Statement> model) {
		throw new UnsupportedOperationException("shaped related resource creation not supported");
	}

	@Override public Optional<Focus> update(final IRI resource, final Collection<Statement> model) {
		return graph.update(connection -> {

			return retrieve(connection, resource, update).map(current -> { // identify updatable description

				connection.remove(current);
				connection.add(model);

				// !!! validate before altering the db (snapshot isolation)

				final Focus focus=validate(connection, resource, update, model);

				if ( focus.assess(Issue.Level.Error) ) {
					connection.rollback();
				} else {
					connection.commit();
				}

				return focus;

			});

		});
	}

	@Override public Optional<IRI> delete(final IRI resource) {
		return graph.update(connection -> {

			// !!! merge retrieve/remove operations into a single SPARQL update txn

			return retrieve(connection, resource, delete).map(current -> { // identify deletable description

				flock.remove(connection, resource, current).remove(current);

				return resource;

			});

		});
	}

}
