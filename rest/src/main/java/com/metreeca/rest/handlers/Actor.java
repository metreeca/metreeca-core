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

package com.metreeca.rest.handlers;

import com.metreeca.form.*;
import com.metreeca.form.probes.Outliner;
import com.metreeca.rest.Request;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Structures.description;
import static com.metreeca.rest.handlers.ActorProcessor.convey;
import static com.metreeca.rest.handlers.ActorProcessor.filter;
import static com.metreeca.tray.Tray.tool;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * LDP resource actor.
 *
 * <p>Handles  CRUD operations on linked data resources stored in the system {@linkplain Graph graph} and identified by
 * the request {@linkplain Request#item() focus item}.</p>
 */
public abstract class Actor extends Delegator {

	private final Graph graph=tool(Graph.graph());
	private final Trace trace=tool(Trace.trace());


	//// CRUD Engine (methods preserved to possibly support extraction of Engine interface) ////////////////////////////

	/**
	 * Relates a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be retrieved
	 * @param query    the query defining the details of {@code resource} to be retrieved;
	 *
	 * @return the description of {@code resource} matched by {@code query}; empty if a matching description for {@code
	 * resource} was not found;  related resources matched by {@code query} are linked to {@code resource} with the
	 * {@code ldp:contains} property
	 *
	 * @throws NullPointerException          if either {@code resource} or {@code query} is {@code null}
	 * @throws UnsupportedOperationException if resource retrieval is not supported by this engine
	 */
	protected Collection<Statement> relate(final IRI resource, final Query query) {
		return graph.query(connection -> {
			return query.map(new ActorRetriever(trace, connection, resource, true));
		});
	}

	/**
	 * Creates a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be created
	 * @param shape    the validation shape for the description of the resource;
	 * @param model    the description for {@code resource} to be created
	 *
	 * @return an optional validation report for the operation; empty a description for {@code resource} is already
	 * present
	 *
	 * @throws NullPointerException          if any argument is null or if {@code model} contains null values
	 * @throws UnsupportedOperationException if resource creation is not supported by this engine
	 */
	protected Optional<Focus> create(final IRI resource, final Shape shape, final Collection<Statement> model) {
		return graph.update(connection -> {

			return Optional.of(resource)

					.filter(reserved -> !(
							connection.hasStatement(reserved, null, null, true)
									|| connection.hasStatement(null, null, reserved, true)
					))

					.map(reserved -> { // validate before updating graph to support snapshot transactions

						final Focus focus=validate(connection, resource, shape, model);

						if ( !focus.assess(Issue.Level.Error) ) {

							connection.add(anchor(reserved, shape));
							connection.add(model);

						}

						return focus;

					});

		});
	}

	/**
	 * Updates a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be updated
	 * @param shape    the validation shape for the description of the resource
	 * @param model    the updated description for {@code resource}
	 *
	 * @return an optional validation report for the operation; empty if a description for {@code resource} was not
	 * found
	 *
	 * @throws NullPointerException          if any argument is null or if {@code model} contains null values
	 * @throws UnsupportedOperationException if resource updating is not supported by this engine
	 */
	protected Optional<Focus> update(final IRI resource, final Shape shape, final Collection<Statement> model) {
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

	/**
	 * Deletes a resource.
	 *
	 * @param resource the IRI identifying the resource whose description is to be deleted
	 * @param shape    the validation shape for the description of the resource
	 *
	 * @return an optional IRI identifying the deleted resource; empty if a description for {@code resource} was not
	 * found
	 *
	 * @throws NullPointerException          if either {@code resource} or {@code shape} is {@code null}
	 * @throws UnsupportedOperationException if resource deletion is not supported by this engine
	 */
	protected Optional<Focus> delete(final IRI resource, final Shape shape) {
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

				.map(query -> query.map(new ActorRetriever(trace, connection, resource, false)))

				.filter(current -> !current.isEmpty());
	}

	private Focus validate(final RepositoryConnection connection,
			final Resource resource, final Shape shape, final Collection<Statement> model
	) {

		final Shape target=shape.map(convey);

		final Focus focus=target // validate against shape
				.map(new ActorValidator(trace, connection, set(resource), model));

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
