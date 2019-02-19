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
import com.metreeca.form.probes.Outliner;
import com.metreeca.form.probes.Redactor;
import com.metreeca.rest.handlers.actors._Engine;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.Memoizing.memoizable;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Structures.description;
import static com.metreeca.tray.Tray.tool;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Graph-based engine.
 *
 * <p>Manages CRUD operations on linked data resources stored in the system {@linkplain Graph graph}.</p>
 */
public final class GraphEngine implements _Engine {

	private static final Function<Shape, Shape> convey=memoizable(s -> s
			.map(new Redactor(Form.mode, Form.convey))
			.map(new Optimizer())
	);

	private static final Function<Shape, Shape> filter=memoizable(s -> s
			.map(new Redactor(Form.mode, Form.filter))
			.map(new Optimizer())
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);


	@Override public Collection<Statement> relate(final IRI resource, final Query query) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return graph.query(connection -> {
			return query.map(new GraphRetriever(connection, resource, true));
		});
	}


	@Override public Optional<Focus> create(final IRI resource, final Shape shape, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

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

							connection.add(shape.map(filter).map(new Outliner(reserved)).collect(toList()));
							connection.add(model);

						}

						return focus;

					});

		});

	}

	@Override public Optional<Focus> update(final IRI resource, final Shape shape, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

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

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}


		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return graph.update(connection -> {
		//
		//	// !!! merge retrieve/remove operations into a single SPARQL update txn
		//	// !!! must check resource existence anyway and wouldn't work for CBD shapes
		//
		//	return retrieve(resource, shape.map(current -> { // identify deletable description
		//
		//		shape.map(flock).remove(connection, resource, current).remove(current);
		//
		//		return resource;
		//
		//	});
		//
		//});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Collection<Statement>> retrieve(
			final RepositoryConnection connection, final IRI resource, final Shape shape
	) {
		return Optional.of(edges(shape))

				.map(query -> query.map(new GraphRetriever(connection, resource, false)))

				.filter(current -> !current.isEmpty());
	}

	private Focus validate(final RepositoryConnection connection,
			final Resource resource, final Shape shape, final Collection<Statement> model
	) {

		final Shape xxx=shape.map(convey);

		// validate against shape

		final Focus focus=xxx
				.map(new GraphValidator(connection, set(resource), model));

		final Collection<Statement> envelope=pass(xxx)
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
