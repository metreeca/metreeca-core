/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca._repo;

import com.metreeca.form.*;
import com.metreeca.form.probes.Outliner;
import com.metreeca.rest.Engine;
import com.metreeca.rest.Request;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca._repo.GraphProcessor.convey;
import static com.metreeca._repo.GraphProcessor.filter;
import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Structures.description;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.rdf.Graph.graph;
import static com.metreeca.tray.sys.Trace.trace;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * LDP resource actor.
 *
 * <p>Handles  CRUD operations on linked data resources stored in the system {@linkplain Graph graph} and identified by
 * the request {@linkplain Request#item() focus item}.</p>
 */
public final class GraphEngine implements Engine {

	private final Graph graph=tool(graph());
	private final Trace trace=tool(trace());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <R> R reading(final Supplier<R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return graph.query(connection -> { return task.get(); });
	}

	@Override public <R> R writing(final Supplier<R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return graph.update(connection -> { return task.get(); });
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Collection<Statement> relate(final IRI resource, final Query query) {
		return graph.query(connection -> {
			return query.map(new GraphRetriever(trace, connection, resource, true));
		});
	}

	@Override public Optional<Focus> create(final IRI resource, final Shape shape, final Collection<Statement> model) {
		return graph.update(connection -> {

			return Optional.of(resource)

					.filter(reserved -> !(
							connection.hasStatement(reserved, null, null, true)
									|| connection.hasStatement(null, null, reserved, true)
					))

					.map(reserved -> {

						// validate before updating graph to support snapshot transactions

						final Focus focus=validate(connection, resource, shape, model);

						if ( !focus.assess(Issue.Level.Error) ) {

							connection.add(anchor(reserved, shape));
							connection.add(model);

						}

						return focus;

					});

		});
	}

	@Override public Optional<Focus> update(final IRI resource, final Shape shape, final Collection<Statement> model) {
		return graph.update(connection -> {
			return retrieve(connection, resource, shape).map(current -> {

				// validate against shape before updating graph to support snapshot transactions

				final Focus focus=validate(connection, resource, shape, model);

				if ( !focus.assess(Issue.Level.Error) ) {
					connection.remove(current);
					connection.add(model);
				}

				return focus;

			});
		});
	}

	@Override public Optional<Focus> delete(final IRI resource, final Shape shape) {
		return graph.update(connection -> {

			// !!! merge retrieve/remove operations into a single SPARQL update txn
			// !!! must check resource existence anyway and wouldn't work for CBD shapes

			return retrieve(connection, resource, shape).map(current -> {

				connection.remove(anchor(resource, shape));
				connection.remove(current);

				return focus(set(), set(frame(resource)));

			});

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Iterable<Statement> anchor(final Resource resource, final Shape shape) {
		return shape.map(filter).map(new Outliner(resource)).collect(toList());
	}

	private Optional<Collection<Statement>> retrieve(
			final RepositoryConnection connection, final IRI resource, final Shape shape
	) {
		return Optional.of(edges(shape))

				.map(query -> query.map(new GraphRetriever(trace, connection, resource, false)))

				.filter(current -> !current.isEmpty());
	}

	private Focus validate(final RepositoryConnection connection,
			final Resource resource, final Shape shape, final Collection<Statement> model
	) {

		final Shape target=shape.map(convey);

		final Focus focus=target // validate against shape
				.map(new GraphValidator(trace, connection, set(resource), model));

		final Collection<Statement> envelope=pass(target)
				? description(resource, false, model) // collect resource cbd
				: focus.outline().collect(toSet()); // collect shape envelope

		return focus( // extend validation report with errors for statements outside shape envelope

				Stream.concat(

						focus.getIssues().stream(),

						model.stream().filter(statement -> !envelope.contains(statement)).map(outlier ->
								issue(Issue.Level.Error, "statement outside shape envelope "+outlier)
						)

				).collect(toList()),

				focus.getFrames()

		);
	}

}
