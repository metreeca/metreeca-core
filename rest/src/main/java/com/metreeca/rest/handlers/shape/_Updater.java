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

package com.metreeca.rest.handlers.shape;


public final class _Updater {


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//private final Shape shape;
	//
	//private Wrapper wrapper=wrapper();
	//
	//private BiFunction<Request, Model, Model> pipe;
	//
	//private final Graph graph=tool(Graph.Factory);
	//
	//
	//private _Updater(final Shape shape) {
	//	this.shape=shape
	//			.accept(task(Form.update))
	//			.accept(view(Form.detail));
	//}
	//
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//@Override public _Updater wrap(final Wrapper wrapper) {
	//
	//	if ( wrapper == null ) {
	//		throw new NullPointerException("null wrapper");
	//	}
	//
	//	this.wrapper=wrapper.wrap(this.wrapper);
	//
	//	return this;
	//}
	//
	//@Override public void handle(final Request request, final Response response) {
	//	authorize(request, response, shape, shape -> model(request, response, shape, model -> {
	//		try (final RepositoryConnection connection=graph.connect()) {
	//
	//			final Collection<Statement> piped=(pipe == null) ?
	//					model : pipe.apply(request, new LinkedHashModel(model));
	//
	//			transactor(connection)
	//					.wrap(wrapper)
	//					.wrap(process(connection, shape, piped))
	//					.handle(request, response);
	//
	//		}
	//	}));
	//}
	//
	//
	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//private Handler process(final RepositoryConnection connection, final Shape shape, final Collection<Statement> model) {
	//	return (request, response) -> {
	//
	//		final IRI focus=request.focus();
	//
	//		// !!! remove system-generated properties (e.g. rdf:types inferred from Link header)
	//
	//		if ( !contains(connection, focus) ) {
	//
	//			// !!! 410 Gone if the resource is known to have existed (how to test?)
	//
	//			response.status(Response.NotFound).done();
	//
	//		} else {
	//
	//			final Report report=update(connection, focus, shape, trace(model));
	//
	//			if ( report.assess(Issue.Level.Error) ) { // shape violations
	//
	//				response.status(Response.UnprocessableEntity)
	//						.json(error("data-invalid", report(report)));
	//
	//			} else { // valid data
	//
	//				response.status(Response.NoContent).done();
	//
	//			}
	//
	//		}
	//	};
	//}

}
