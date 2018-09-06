/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.handlers.shape;


import com.metreeca.form.Form;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.query.QueryLanguage;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.rio.helpers.AbstractRDFHandler;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.Shape.task;
import static com.metreeca.form.Shape.view;
import static com.metreeca.form.things.Bindings.bindings;
import static com.metreeca.form.things.Values.time;
import static com.metreeca.tray._Tray.tool;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


public final class Builder extends Shaper {

	public static Builder builder(final Shape shape, final String sparql) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( sparql == null ) {
			throw new NullPointerException("null sparql");
		}

		return builder(shape, new SPARQLBuilder(sparql));
	}

	public static Builder builder(final Shape shape, final Function<Request, Collection<Statement>> builder) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( builder == null ) {
			throw new NullPointerException("null builder");
		}

		return new Builder(shape, builder);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Shape shape;

	private final Function<Request, Collection<Statement>> builder;


	private Builder(final Shape shape, final Function<Request, Collection<Statement>> builder) {

		this.shape=shape
				.accept(task(Form.relate))
				.accept(view(Form.detail))
				.accept(mode(Form.verify));

		this.builder=builder;

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void handle(final Request request, final Response response) {
		authorize(request, response, shape, shape -> {

			final Collection<Statement> model=builder.apply(request);

			if ( model.isEmpty() ) {

				response.status(Response.NotFound).done();

			} else {

				response.status(Response.OK).rdf(shape
						.accept(new Restrictor(model, singleton(request.focus())))
						.collect(toList()), shape);

			}

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class SPARQLBuilder implements Function<Request, Collection<Statement>> {

		private final String sparql;

		private final Graph graph=tool(Graph.Factory);


		private SPARQLBuilder(final String sparql) { this.sparql=sparql; }


		@Override public Collection<Statement> apply(final Request request) {

			final Collection<Statement> model=new ArrayList<>();

			try (final RepositoryConnection connection=graph.connect()) {
				bindings()

						.set("this", request.focus())
						.set("user", request.user())
						.set("time", time(true))

						.bind(connection.prepareGraphQuery(QueryLanguage.SPARQL, sparql, request.base()))

						.evaluate(new AbstractRDFHandler() {
							@Override public void handleStatement(final Statement statement) { model.add(statement); }
						});
			}

			return model;
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
