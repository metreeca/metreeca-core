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

package com.metreeca.next.handlers.shape;


import com.metreeca.link.*;
import com.metreeca.spec.*;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.function.BiFunction;

import static com.metreeca.link.Handler.error;
import static com.metreeca.link.Wrapper.wrapper;
import static com.metreeca.next.wrappers.Transactor.transactor;
import static com.metreeca.spec.Shape.task;
import static com.metreeca.spec.Shape.view;
import static com.metreeca.spec.sparql.SPARQLEngine.contains;
import static com.metreeca.spec.sparql.SPARQLEngine.update;
import static com.metreeca.tray.Tray.tool;


public final class Updater extends Shaper {

	public static Updater updater(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return new Updater(shape);
	}


	private final Shape shape;

	private Wrapper wrapper=wrapper();

	private BiFunction<Request, Model, Model> pipe;

	private final Graph graph=tool(Graph.Tool);


	private Updater(final Shape shape) {
		this.shape=shape
				.accept(task(Spec.update))
				.accept(view(Spec.detail));
	}


	public Updater pipe(final BiFunction<Request, Model, Model> pipe) {

		if ( pipe == null ) {
			throw new NullPointerException("null pipe");
		}

		this.pipe=chain(this.pipe, pipe);

		return this;
	}


	@Override public Updater wrap(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		this.wrapper=wrapper.wrap(this.wrapper);

		return this;
	}

	@Override public void handle(final Request request, final Response response) {
		authorize(request, response, shape, shape -> model(request, response, shape, model -> {
			try (final RepositoryConnection connection=graph.connect()) {

				final Collection<Statement> piped=(pipe == null) ?
						model : pipe.apply(request, new LinkedHashModel(model));

				transactor(connection)
						.wrap(wrapper)
						.wrap(process(connection, shape, piped))
						.handle(request, response);

			}
		}));
	}


	private Handler process(final RepositoryConnection connection, final Shape shape, final Collection<Statement> model) {
		return (_request, _response) -> {

			final IRI focus=_request.focus();

			// !!! remove system-generated properties (e.g. rdf:types inferred from Link header)

			if ( !contains(connection, focus) ) {

				// !!! 410 Gone if the resource is known to have existed (how to test?)

				_response.status(Response.NotFound).done();

			} else {

				final Report report=update(connection, focus, shape, trace(model));

				if ( report.assess(Issue.Level.Error) ) { // shape violations

					_response.status(Response.UnprocessableEntity)
							.json(error("data-invalid", report(report)));

				} else { // valid data

					_response.status(Response.NoContent).done();

				}

			}
		};
	}

}
