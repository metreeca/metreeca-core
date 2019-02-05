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
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.queries.Edges;
import com.metreeca.rest.Engine;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.IsolationLevels;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Shape-driven resource engine.
 *
 * <p>Manages CRUD lifecycle operations on resource descriptions defined by a shape.</p>
 */
final class ShapedResource implements Engine {

	private final Graph graph;

	private final Shape relate;
	private final Shape update;
	private final Shape delete;


	ShapedResource(final Graph graph, final Shape shape) {

		this.graph=graph;

		this.relate=redact(shape, Form.relate);
		this.update=redact(shape, Form.update);
		this.delete=redact(shape, Form.delete);

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
				// !!! make sure the validator use update state in 'model' rather than current state in 'current'

				final Focus focus=validate(connection, resource, model);

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

				connection.remove(current);

				return resource;

			});

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Shape redact(final Shape shape, final IRI task) {
		return shape.map(new Redactor(map(
				entry(Form.task, set(task)),
				entry(Form.view, set(Form.detail)),
				entry(Form.role, set(Form.any))
		))).map(new Optimizer());
	}


	private Optional<Collection<Statement>> retrieve(final RepositoryConnection connection, final IRI resource, final Shape task) {
		return new SPARQLRetriever(connection)
				.process(new Edges(and(all(resource), task)))
				.entrySet()
				.stream()
				.findFirst()
				.map(Map.Entry::getValue);
	}

	private Focus validate(final RepositoryConnection connection, final IRI resource, final Collection<Statement> model) {

		// validate against shape (disable if not transactional) // !!! just downgrade

		final boolean unsafe=connection.getIsolationLevel().equals(IsolationLevels.NONE);

		final Focus focus=new SPARQLValidator(connection).process(unsafe ? and() : update, resource);

		// validate shape envelope

		final Collection<Statement> envelope=focus.outline().collect(toSet());

		final Collection<Statement> outliers=unsafe ? emptySet() : model.stream()
				.filter(statement -> !envelope.contains(statement))
				.collect(toList());

		// extend validation report with statements outside shape envelope

		return outliers.isEmpty() ? focus : focus(concat(focus.getIssues(), outliers.stream()
				.map(outlier -> issue(Issue.Level.Error, "statement outside shape envelope "+outlier))
				.collect(toList())
		), focus.getFrames());

	}

}
