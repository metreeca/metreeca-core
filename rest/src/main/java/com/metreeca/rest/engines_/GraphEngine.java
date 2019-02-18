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

package com.metreeca.rest.engines_;

import com.metreeca.form.*;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.Memo.memoizable;
import static com.metreeca.form.things.Sets.set;
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

	@Override public Optional<Focus> create(final IRI resource, final Shape shape, final IRI related, final Collection<Statement> model) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( related == null ) {
			throw new NullPointerException("null slug");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return graph.update(connection -> {
		//
		//	return reserve(connection, related).map(reserved -> {
		//
		//		// validate before updating graph to support snapshot transactions
		//
		//		final Focus focus=new GraphValidator().validate(related, shape, model);
		//
		//		if ( !focus.assess(Issue.Level.Error) ) {
		//
		//			shape.map(flock).insert(connection, resource, reserved, model).add(model);
		//
		//		}
		//
		//		return focus;
		//
		//	});
		//
		//});

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
			return Optional.of(edges(shape))

					.map(query -> query.map(new GraphRetriever(connection, resource, false)))
					.filter(current -> !current.isEmpty())

					.map(current -> {

						// validate against shape before updating graph to support snapshot transactions

						final Shape xxx=shape.map(convey);

						final Focus focus=xxx  // validate against shape
								.map(new GraphValidator(connection, set(resource), model));


						final Collection<Statement> envelope=pass(xxx)
								? set() // !!! description(resource, false, model)
								: focus.outline().collect(toSet()); // collect shape envelope

						final Focus extended=focus( // extend validation report with errors for statements outside shape envelope

								Stream.concat(

										focus.getIssues().stream(),

										model.stream().filter(statement -> !envelope.contains(statement)).map(outlier ->
												issue(Issue.Level.Error, "statement outside shape envelope "+outlier)
										)

								).collect(toList()),

								focus.getFrames()

						);


						if ( !extended.assess(Issue.Level.Error) ) {
							connection.remove(current);
							connection.add(model);
						}

						return extended;

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

}
