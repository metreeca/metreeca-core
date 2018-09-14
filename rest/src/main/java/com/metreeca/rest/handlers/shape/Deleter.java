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
import com.metreeca.rest.*;
import com.metreeca.form.Shape;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.wrappers._Transactor.transactor;
import static com.metreeca.form.Shape.empty;
import static com.metreeca.form.Shape.task;
import static com.metreeca.form.Shape.view;
import static com.metreeca.form.sparql.SPARQLEngine.contains;
import static com.metreeca.form.sparql.SPARQLEngine.delete;
import static com.metreeca.tray._Tray.tool;


public final class Deleter extends Shaper {

	public static Deleter deleter(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return new Deleter(shape);
	}


	private final Shape shape;

	private Wrapper wrapper=wrapper();

	private final Graph graph=tool(Graph.Factory);


	private Deleter(final Shape shape) {
		this.shape=shape
				.accept(task(Form.delete))
				.accept(view(Form.detail));
	}


	public boolean active() {
		return !empty(shape);
	}


	@Override public Deleter wrap(final Wrapper wrapper) {

		if ( wrapper == null ) {
			throw new NullPointerException("null wrapper");
		}

		this.wrapper=wrapper.wrap(this.wrapper);

		return this;
	}

	@Override public void handle(final Request request, final Response response) {
		authorize(request, response, shape, shape -> {
			try (final RepositoryConnection connection=graph.connect()) {

				transactor(connection)
						.wrap(wrapper)
						.wrap(process(connection, shape))
						.handle(request, response);

			}
		});
	}


	private Handler process(final RepositoryConnection connection, final Shape shape) {
		return (request, response) -> {

			final IRI focus=request.focus();

			if ( !contains(connection, focus) ) {

				// !!! 410 Gone if the resource is known to have existed (how to test?)

				response.status(Response.NotFound).done();

			} else {

				delete(connection, focus, shape);

				response.status(Response.NoContent).done();

			}

		};
	}

}
