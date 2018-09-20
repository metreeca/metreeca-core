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


public final class _Creator {

	//
	///*
	// * Shared lock for serializing slug operations (concurrent graph txns may produce conflicting results).
	// */
	//private static final Supplier<Object> LockFactory=Object::new; // !!! ;( breaks in distributed containers
	//
	//
	//public static _Creator creator(final Shape shape) {
	//
	//	if ( shape == null ) {
	//		throw new NullPointerException("null shape");
	//	}
	//
	//	return new _Creator(shape);
	//}
	//
	//
	//private final Shape shape;
	//
	//private Wrapper wrapper=wrapper();
	//
	//private BiFunction<Request, Model, Model> pipe;
	//private BiFunction<Request, Collection<Statement>, String> slug;
	//
	//private final Graph graph=tool(Graph.Factory);
	//private final Object lock=tool(LockFactory);
	//
	//
	//private _Creator(final Shape shape) {
	//
	//	this.shape=shape
	//			.accept(task(Form.create))
	//			.accept(view(Form.detail));
	//
	//	this.slug=uuid();
	//}
	//
	//
	//public boolean active() {
	//	return !empty(shape);
	//}
	//
	//public _Creator pipe(final BiFunction<Request, Model, Model> pipe) {
	//
	//	if ( pipe == null ) {
	//		throw new NullPointerException("null pipe");
	//	}
	//
	//	this.pipe=chain(this.pipe, pipe);
	//
	//	return this;
	//}
	//
	//public _Creator slug(final BiFunction<Request, Collection<Statement>, String> slug) {
	//
	//	if ( slug == null ) {
	//		throw new NullPointerException("null slug");
	//	}
	//
	//	this.slug=slug;
	//
	//	return this;
	//}
	//
	//
	//@Override public _Creator wrap(final Wrapper wrapper) {
	//
	//	if ( wrapper == null ) {
	//		throw new NullPointerException("null wrapper");
	//	}
	//
	//	synchronized ( lock ) {
	//		this.wrapper=wrapper.wrap(this.wrapper);
	//	}
	//
	//	return this;
	//}
	//
	//@Override public void handle(final Request request, final Response response) {
	//	authorize(request, response, shape, shape -> model(request, response, shape, model -> {
	//
	//		synchronized ( lock ) { // serialize auto-id generation from multiple txns
	//			try (final RepositoryConnection connection=graph.connect(SERIALIZABLE)) { // attempt to isolate slug operations
	//
	//				final Collection<Statement> piped=(pipe == null) ?
	//						model : pipe.apply(request, new LinkedHashModel(model));
	//
	//				transactor(connection)
	//						.wrap(wrapper)
	//						.wrap(process(connection, shape, piped))
	//						.handle(request, response);
	//
	//			}
	//		}
	//
	//	}));
	//}
	//
	//
	//private Handler process(final RepositoryConnection connection, final Shape shape, final Collection<Statement> model) {
	//	return (request, response) -> {
	//
	//		final String name=slug.apply(request, model);
	//
	//		final IRI iri=iri(request.stem(), name); // assign an IRI to the resource to be created
	//		final Collection<Statement> rewritten=rewrite(model, request.focus(), iri); // rewrite to IRI
	//
	//		final Report report=create(connection, iri, shape, trace(rewritten));
	//
	//		if ( report.assess(Issue.Level.Error) ) { // shape violations
	//
	//			// !!! rewrite report value references to original target iri
	//			// !!! rewrite references to external base IRI
	//
	//			response.status(Response.UnprocessableEntity)
	//					.json(error("data-invalid", report(report)));
	//
	//		} else { // valid data
	//
	//			response.status(Response.Created)
	//					.header("Location", iri.stringValue())
	//					.done();
	//
	//		}
	//
	//	};
	//}
	//
	//
	////// Slug Functions ////////////////////////////////////////////////////////////////////////////////////////////////
	//
	//public static BiFunction<Request, Collection<Statement>, String> uuid() {
	//	return (request, model) -> randomUUID().toString();
	//}
	//
	//public static BiFunction<Request, Collection<Statement>, String> auto(final Shape shape) {
	//
	//	if ( shape == null ) {
	//		throw new NullPointerException("null shape");
	//	}
	//
	//	final Shape matcher=shape
	//			.accept(task(Form.relate))
	//			.accept(view(Form.digest))
	//			.accept(role(Form.any));
	//
	//	final Graph graph=tool(Graph.Factory);
	//
	//	return (request, model) -> {
	//		try (final RepositoryConnection connection=graph.connect()) {
	//
	//			// !!! custom iri stem/pattern
	//			// !!! client naming hints (http://www.w3.org/TR/ldp/ §5.2.3.10 -> https://tools.ietf.org/html/rfc5023#section-9.7)
	//			// !!! normalize slug (https://tools.ietf.org/html/rfc5023#section-9.7)
	//			// !!! support UUID hint
	//			// !!! 409 Conflict https://tools.ietf.org/html/rfc7231#section-6.5.8 for clashing slug?
	//
	//			final String stem=request.stem();
	//			final Collection<Statement> edges=SPARQLEngine.browse(connection, matcher)
	//					.values()
	//					.stream()
	//					.findFirst()
	//					.orElseGet(Collections::emptySet);
	//
	//			long count=edges.size();
	//			IRI iri;
	//
	//			do {
	//
	//				iri=iri(stem, String.valueOf(++count));
	//
	//			} while ( connection.hasStatement(iri, null, null, true)
	//					|| connection.hasStatement(null, null, iri, true) );
	//
	//			return String.valueOf(count);
	//
	//		}
	//	};
	//}

}
