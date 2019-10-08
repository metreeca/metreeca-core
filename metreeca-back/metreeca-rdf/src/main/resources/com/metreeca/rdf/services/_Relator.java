/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rdf.services;

import com.metreeca.tree.Query;
import com.metreeca.tree.Shape;
import com.metreeca.tree.queries.Edges;
import com.metreeca.tree.queries.Items;
import com.metreeca.tree.queries.Stats;
import com.metreeca.tree.things.Shapes;
import com.metreeca.rest.*;
import com.metreeca.rest.bodies.RDFBody;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.services.Engine;
import com.metreeca.rest.wrappers.Throttler;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.tree.queries.Edges.edges;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.things.Lists.concat;
import static com.metreeca.tree.things.Shapes.container;
import static com.metreeca.tree.things.Shapes.resource;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Engine.engine;
import static com.metreeca.rest.Message.link;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.bodies.RDFBody.rdf;


 final class _Relator extends Delegator {

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Engine engine=service(engine());


	public _Relator() {
		delegate(relator()
				.with(annotator())
				.with(throttler())
				.with(connector())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper annotator() {
		return handler -> request -> handler.handle(request).map(response -> response
				.headers("+Vary", "Accept", "Prefer")
				.headers("+Link",
						link(LDP.RESOURCE, "type"),
						link(LDP.RDF_SOURCE, "type"),
						request.container() ? link(LDP.CONTAINER, "type") : ""
				)
		);
	}

	private Wrapper throttler() {
		return wrapper(Request::container,
				new Throttler(Shape.Relate, Shape.Digest, Shapes::entity),
				new Throttler(Shape.Relate, Shape.Detail, Shapes::resource)
		);
	}

	private Wrapper connector() {
		return handler -> request -> engine.exec(() -> handler.handle(request));
	}

	private Handler relator() {
		return request -> request.reply(response -> {

			final IRI item=request.item();
			final Shape shape=request.shape();

			final boolean resource=!request.container();
			final boolean minimal=include(request, LDP.PREFER_MINIMAL_CONTAINER);
			final boolean filtered=!request.query().isEmpty();

			if ( resource || minimal ) {

				return filtered ? response.map(new Failure()

						.status(Response.NotImplemented)
						.cause("resource filtered retrieval not supported")

				) : request.query(resource(item, shape)).fold(

						query -> {

							final Collection<Statement> model=engine.relate(item, query);

							return response

									.status(resource && model.isEmpty() ? NotFound : OK) // !!! 410 Gone if previously known

									.header("+Preference-Applied",
											minimal ? include(LDP.PREFER_MINIMAL_CONTAINER) : ""
									)

									.shape(query.map(new Query.Probe<Shape>() {

										@Override public Shape probe(final Edges edges) {
											return edges.getShape(); // !!! add ldp:contains if edges.path is not empty
										}

										@Override public Shape probe(final Stats stats) {
											return Stats.Shape;
										}

										@Override public Shape probe(final Items items) {
											return Items.Shape;
										}

									}))

									.body(rdf(), model);

						},

						response::map

				);

			} else {

				// containers are currently virtual and respond always with 200 OK even if not described in the graph

				return request.query(container(item, resource(shape))).fold(

						query -> {

							final Collection<Statement> matches=engine.relate(item, query);

							if ( filtered ) { // matches only

								return response
										.status(OK)
										.shape(query.map(new Query.Probe<Shape>() {

											@Override public Shape probe(final Edges edges) {
												return field(LDP.CONTAINS, edges.getShape());
											}

											@Override public Shape probe(final Stats stats) {
												return Stats.Shape;
											}

											@Override public Shape probe(final Items items) {
												return Items.Shape;
											}

										}))
										.body(rdf(), matches);

							} else { // include container description

								// !!! 404 NotFound or 410 Gone if previously known for non-virtual containers

								return response
										.status(OK)
										.shape(shape)
										.body(rdf(), concat(
												matches,
												engine.relate(item, edges(resource(item, container(shape))))
										));

							}

						},

						response::map

				);

			}

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String include(final IRI include) {
		return "return=representation; include=\""+include+"\"";
	}

	private boolean include(final Request request, final IRI include) {

		// !!! handle multiple uris in include parameter
		// !!! handle omit parameter (with multiple uris)
		// !!! handle other representation values in https://www.w3.org/TR/ldp/#prefer-parameters

		final String representation=request
				.headers("Prefer")
				.stream()
				.map(value -> {

					final Matcher matcher=RepresentationPattern.matcher(value);

					return matcher.matches() ? matcher.group("representation") : "";

				})
				.filter(value -> !value.isEmpty())
				.findFirst()
				.orElse("");

		return representation.equals(include.stringValue());

	}

}
