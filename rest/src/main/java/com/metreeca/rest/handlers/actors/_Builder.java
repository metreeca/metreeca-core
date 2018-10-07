/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.query.GraphQuery;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.util.Collection;
import java.util.Collections;
import java.util.Set;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.things.Values.time;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.ShapeFormat.shape;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


public final class _Builder extends Actor<_Builder> {

	public _Builder query(final String query) {

		if ( query == null ) {
			throw new NullPointerException("null query");
		}

		return pre(new SPARQLBuilder(query));
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {
		return request.query().isEmpty() ? handler(Form.relate, Form.detail, shape -> request.reply(response -> {

			final Collection<Statement> model=request.body(rdf()).get().orElseGet(Collections::emptySet);

			return model.isEmpty()

					? response.status(Response.NotFound)

					: response.status(Response.OK)

					.body(shape()).set(shape.accept(mode(Form.verify)))// hide filtering constraints
					.body(rdf()).set(shape.accept(new Restrictor(model, singleton(response.item()))).collect(toList()));

		})).handle(

				request.body(rdf()).set(emptySet()) // initial empty model to activate pre-processing

		) : request.reply(new Failure<>().status(Response.BadRequest).cause("unexpected query parameters"));
	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class SPARQLBuilder implements BiFunction<Request, Model, Model> {

		private final String sparql;

		private final Graph graph=tool(Graph.Factory);


		private SPARQLBuilder(final String sparql) { this.sparql=sparql; }


		@Override public Model apply(final Request request, final Model model) {
			return graph.query(connection -> {

				final GraphQuery query=connection.prepareGraphQuery(QueryLanguage.SPARQL, sparql, request.base());

				query.setBinding("this", request.item());
				query.setBinding("user", request.user());
				query.setBinding("time", time(true));

				query.evaluate(new AbstractRDFHandler() {
					@Override public void handleStatement(final Statement statement) { model.add(statement); }
				});

				return model;

			});
		}

	}

	/**
	 * Model restrictor.
	 *
	 * <p>Recursively extracts from a model and an initial collection of source values all the statements compatible
	 * with a shape.</p>
	 */
	private static final class Restrictor extends Shape.Probe<Stream<Statement>> {

		private final Collection<Statement> model;
		private final Collection<Value> sources;


		private Restrictor(final Collection<Statement> model, final Collection<Value> sources) {
			this.model=model;
			this.sources=sources;
		}


		@Override protected Stream<Statement> fallback(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<Statement> visit(final Trait trait) {

			final Step step=trait.getStep();

			final IRI iri=step.getIRI();
			final boolean inverse=step.isInverse();

			final Function<Statement, Value> source=inverse
					? Statement::getObject
					: Statement::getSubject;

			final Function<Statement, Value> target=inverse
					? Statement::getSubject
					: Statement::getObject;

			final Collection<Statement> restricted=model.stream()
					.filter(s -> sources.contains(source.apply(s)) && iri.equals(s.getPredicate()))
					.collect(toList());

			final Set<Value> focus=restricted.stream()
					.map(target)
					.collect(toSet());

			return Stream.concat(restricted.stream(), trait.getShape().accept(new Restrictor(model, focus)));
		}

		@Override public Stream<Statement> visit(final And and) {
			return and.getShapes().stream().flatMap(shape -> shape.accept(this));
		}

		@Override public Stream<Statement> visit(final Or or) {
			return or.getShapes().stream().flatMap(shape -> shape.accept(this));
		}

		@Override public Stream<Statement> visit(final Test test) {
			return Stream.concat(test.getPass().accept(this), test.getFail().accept(this));
		}

		@Override public Stream<Statement> visit(final Group group) {
			return group.getShape().accept(this);
		}

	}

}
