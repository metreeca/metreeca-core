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

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.regex.Pattern;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Test.test;
import static com.metreeca.form.shifts.Step.step;

import static java.util.stream.Collectors.toList;


/**
 * Model-driven LDP Basic Container handler.
 *
 * <p>Manages read/write operations for LDP basic containers under the control of a linked data {@linkplain Shape
 * shape}.</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldpbc">Linked Data Platform 1.0 - §5.3 Basic</a>
 */
public final class Browser_LDP implements Handler {

	private static final Step Contains=step(LDP.CONTAINS);

	private static final Shape.Probe<Shape> trimmer=new Shape.Probe<Shape>() { // prune ldp:contains trait // !!! review

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//private Handler dispatcher=dispatcher();


	/**
	 * Configures the linked data shape for this handler.
	 *
	 * @param shape the shape driving read/write operations for the LDPbasic container managed by this handler
	 *
	 * @return this handler
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public Browser_LDP shape(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		//final Browser browser=browser(shape);
		//final Creator creator=creator(shape);
		//
		//final Dispatcher dispatcher=dispatcher();
		//
		//if ( browser.active() ) { dispatcher.get(browser); }
		//if ( creator.active() ) { dispatcher.post(creator); }
		//
		//this.dispatcher=dispatcher
		//
		//		.wrap(inspector().shape(shape));

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void handle(final Request request, final Response response) {
		//dispatcher.handle(
		//
		//		writer -> writer.copy(request)
		//
		//				.done(),
		//
		//		reader -> response.copy(reader)
		//
		//				.header("Link",
		//
		//						format(Link.ShapedContainer)+"; rel=\"type\"",
		//						"<http://www.w3.org/ns/ldp#BasicContainer>; rel=\"type\"",
		//						"<http://www.w3.org/ns/ldp#Container>; rel=\"type\"",
		//						"<http://www.w3.org/ns/ldp#RDFSource>; rel=\"type\"",
		//						"<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"")
		//
		//				.done()
		//
		//);
	}


	//// !!! migrate to Browser ////////////////////////////////////////////////////////////////////////////////////////

	//private void get() {
	//
	//	final String representation=request.getHeaders("Prefer")
	//			.stream()
	//			.map(value -> {
	//
	//				final Matcher matcher=RepresentationPattern.matcher(value);
	//
	//				return matcher.matches() ? matcher.group("representation") : "";
	//
	//			})
	//			.filter(value -> !value.isEmpty())
	//			.findFirst()
	//			.orElse("");
	//
	//	final IRI target=iri(request.getTarget());
	//	final String query=request.getQuery();
	//
	//	final Shape relating=shape.accept(Shape.task(Form.relate));
	//	final Shape authorized=relating.accept(Shape.role(request.getRoles()));
	//
	// if ( representation.equals("http://www.w3.org/ns/ldp#PreferMinimalContainer")
	//			|| representation.equals("http://www.w3.org/ns/ldp#PreferEmptyContainer") ) {
	//
	//		// !!! handle multiple uris in include parameter
	//		// !!! handle omit parameter (with multiple uris)
	//		// !!! handle other representation values in https://www.w3.org/TR/ldp/#prefer-parameters
	//
	//		// !!! include Preference-Applied response header
	//		// !!! include Vary response header?
	//
	//		// split container/resource shapes and augment them with system generated properties
	//
	//		final Shape container=and(all(target), authorized.accept(trimmer));
	//
	//		response.setStatus(_Response.OK);
	//
	//		new _Transfer(request, response).model( // !!! re/factor
	//
	//				container.accept(Shape.mode(Form.verify)).accept(new Outliner()), container
	//
	//		);
	//
	//		sink.accept(request, response);
	//
	//	} else {
	//
	//		// split container/resource shapes and augment them with system generated properties
	//
	//		final Shape container=and(all(target), authorized.accept(trimmer));
	//		final Shape resource=traits(authorized).getOrDefault(Contains, and());
	//
	//		// construct and process configured query, merging constraints from the query string
	//
	//		final Query filter;
	//
	//		try {
	//			filter=new QueryParser(resource).parse(Transputs.decode(query));
	//		} catch ( final RuntimeException e ) {
	//			throw new _LinkException(_Response.BadRequest, "malformed query: "+e.getMessage(), e);
	//		}
	//
	//		// retrieve filtered content from repository
	//
	//		final _Cell cell=request.map(graph).get(filter);
	//
	//		if ( filter instanceof com.metreeca.form.queries.Edges ) {
	//			cell.reverse(LDP.CONTAINS).insert(target);
	//		}
	//
	//		// signal successful retrieval of the filtered container
	//
	//		response.setStatus(_Response.OK);
	//
	//		new _Transfer(request, response).model( // !!! re/factor
	//
	//				query.isEmpty()
	//
	//						// base container: convert its shape to RDF and merge into results
	//
	//						? Sets.union(cell.model(), container.accept(Shape.mode(Form.verify)).accept(new Outliner()))
	//
	//						// filtered container: return selected data
	//
	//						: filter instanceof com.metreeca.form.queries.Edges ? cell.model()
	//
	//						// introspection query: rewrite query results to the target IRI
	//
	//						: rewrite(cell.model(), Form.meta, target),
	//
	//				// merge all possible shape elements to properly drive response formatting
	//
	//				or(container, trait(LDP.CONTAINS, resource), StatsShape, ItemsShape)
	//
	//		);
	//
	//		sink.accept(request, response);
	//
	//	}
	//
	//}

}
