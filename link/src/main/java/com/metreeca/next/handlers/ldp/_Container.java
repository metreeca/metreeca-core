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

package com.metreeca.next.handlers.ldp;

import com.metreeca.spec.Shape;
import com.metreeca.spec.shapes.*;
import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.regex.Pattern;

import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shifts.Step.step;

import static java.util.stream.Collectors.toList;


public final class _Container {

	protected static final Step Contains=step(LDP.CONTAINS);

	protected static final Shape.Probe<Shape> trimmer=new Shape.Probe<Shape>() { // prune ldp:contains trait // !!! review

		@Override protected Shape fallback(final Shape shape) { return shape; }

		@Override public Shape visit(final Trait trait) { return trait.getStep().equals(Contains) ? and() : trait; }

		@Override public Shape visit(final Virtual virtual) {
			return virtual.getTrait().getStep().equals(Contains) ? and() : virtual;
		}

		@Override public Shape visit(final And and) {
			return and(and.getShapes().stream().map(s -> s.accept(this)).collect(toList()));
		}

		@Override public Shape visit(final Or or) {
			return or(or.getShapes().stream().map(s -> s.accept(this)).collect(toList()));
		}

		@Override public Shape visit(final Test test) {
			return test(test.getTest().accept(this),
					test.getPass().accept(this), test.getFail().accept(this));
		}

	};

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


	//@Override public void handle(final Request request, final Response response) {
	//
	//	dispatcher.handle(tools, request, response, (_request, _response) -> {
	//
	//		if ( _response.getStatus() == Response.OK ) {
	//			_response
	//
	//					.addHeader("Link", "<"+Link.ShapedContainer.stringValue()+">; rel=\"type\"")
	//					.addHeader("Link", "<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"")
	//					.addHeader("Link", "<http://www.w3.org/ns/ldp#Container>; rel=\"type\"")
	//					.addHeader("Link", "<http://www.w3.org/ns/ldp#RDFResource>; rel=\"type\"")
	//					.addHeader("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"")
	//
	//					.addHeader("Link", String.format("<%s?specs>; rel=\"%s\"", request.getTarget(), LDP.CONSTRAINEDBY));
	//		}
	//
	//		sink.accept(_request, _response);
	//
	//	});
	//
	//}


	//// !!! Browser ///////////////////////////////////////////////////////////////////////////////////////////////////

	//final String representation=request.getHeaders("Prefer")
	//		.stream()
	//		.map(value -> {
	//
	//			final Matcher matcher=RepresentationPattern.matcher(value);
	//
	//			return matcher.matches() ? matcher.group("representation") : "";
	//
	//		})
	//		.filter(value -> !value.isEmpty())
	//		.findFirst()
	//		.orElse("");


	//// !!! Relater ///////////////////////////////////////////////////////////////////////////////////////////////////

	//
	//} else if ( query.equals("specs") ) { // !!! review / factor
	//
	//	// !!! specs query can't be processed as a regular query as it requires visibility on all tasks
	//	// !!! user redaction must be performed before task redaction (ie reversed wrt regular processing)
	//
	//	final IRI iri=iri(focus+"?"+query);
	//	final Collection<Statement> model=new ArrayList<>();
	//
	//	model.add(statement(focus, LDP.CONSTRAINEDBY, iri));
	//
	//	final Shape shape=this.shape
	//			.accept(role(request.getRoles()))
	//			.accept(Shape.mode(Spec.verify))
	//			.accept(new Inferencer())
	//			.accept(new Optimizer());
	//
	//	final ShapeCodec codec=new ShapeCodec();
	//
	//	final Shape relate=shape.accept(Shape.task(Spec.relate)); // container relation shape
	//
	//	if ( !empty(relate) ) {
	//		model.add(statement(iri, Spec.relate, codec.encode(relate, model)));
	//	}
	//
	//	final Shape create=traits(shape).getOrDefault(Contains, and()) // extract resource creation shape
	//			.accept(Shape.task(Spec.create));
	//
	//	if ( !empty(create) ) {
	//		model.add(statement(iri, Spec.create, codec.encode(create, model)));
	//	}
	//
	//	response.status(Response.OK)
	//			.rdf(model, and())  // !!! SpecsShape
	//		;


	//
	//} else if ( representation.equals("http://www.w3.org/ns/ldp#PreferMinimalContainer")
	//		|| representation.equals("http://www.w3.org/ns/ldp#PreferEmptyContainer") ) {
	//
	//	// !!! handle multiple uris in include parameter
	//	// !!! handle omit parameter (with multiple uris)
	//	// !!! handle other representation values in https://www.w3.org/TR/ldp/#prefer-parameters
	//
	//	// split container/resource shapes and augment them with system generated properties
	//
	//	final Shape container=and(all(focus), shape.accept(trimmer)); // !!! see Container
	//
	//	response.status(Response.OK)
	//			.rdf( // !!! re/factor
	//
	//					container.accept(Shape.mode(Spec.verify)).accept(new Outliner()), container
	//
	//			);

}
