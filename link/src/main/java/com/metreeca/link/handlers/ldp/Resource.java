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

package com.metreeca.link.handlers.ldp;

public final class Resource {

	//@Override public void handle(final Tool.Loader tools,
	//		final Request request, final Response response, final BiConsumer<Request, Response> sink) {
	//
	//	dispatcher.handle(tools, request, response, (_request, _response) -> {
	//
	//		if ( _response.getStatus() == Response.OK ) {
	//			_response.addHeader("Link", format(Link.ShapedResource)+"; rel=\"type\"")
	//					.addHeader("Link", "<http://www.w3.org/ns/ldp#RDFResource>; rel=\"type\"")
	//					.addHeader("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"")
	//					.addHeader("Link", String.format("<%s?specs>; rel=\"%s\"", _request.getTarget(), LDP.CONSTRAINEDBY));
	//		}
	//
	//		sink.accept(_request, _response);
	//
	//	});
	//
	//}

	//// !!! Relate ////////////////////////////////////////////////////////////////////////////////////////////////////

	//} else if ( query.equals("specs") ) { // !!! review / factor
	//
	//	// !!! specs query can't be processed as a regular query as it requires visibility on all tasks
	//	// !!! user redaction must be performed before task redaction (ie reversed wrt regular processing)
	//
	//	final IRI iri=iri(target+"?"+query);
	//	final Collection<Statement> model=new ArrayList<>();
	//
	//	model.add(statement(target, LDP.CONSTRAINEDBY, iri));
	//
	//	final Shape shape=this.shape
	//			.accept(role(request.roles()))
	//			.accept(mode(Spec.verify))
	//			.accept(new Inferencer())
	//			.accept(new Optimizer());
	//
	//	final ShapeCodec codec=new ShapeCodec();
	//
	//	for (final IRI task : list(Spec.relate, Spec.update, Spec.delete)) {
	//
	//		final Shape spec=shape.accept(task(task));
	//
	//		if ( !empty(spec) ) {
	//			model.add(statement(iri, task, codec.encode(spec, model)));
	//		}
	//
	//	}
	//
	//	response.status(Response.OK)
	//			.rdf(model, and() /* !!! SpecsShape*/);

}
