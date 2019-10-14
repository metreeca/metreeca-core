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

import com.metreeca.rdf.formats.RDFFormat;
import com.metreeca.rest.*;
import com.metreeca.tree.Query;
import com.metreeca.tree.Shape;
import com.metreeca.tree.queries.Items;
import com.metreeca.tree.queries.Stats;
import com.metreeca.tree.queries.Terms;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.rdf.Values.iri;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf.services.Graph.graph;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.tree.queries.Items.items;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;


final class GraphRelator extends GraphProcessor {

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=service(graph());

	private final RDFFormat rdf=rdf();


	Future<Response> handle(final Request request) {
		return request.reply(response -> {

			final boolean resource=!request.collection();
			final boolean minimal=include(request, LDP.PREFER_MINIMAL_CONTAINER);
			final boolean filtered=!request.query().isEmpty();

			if ( resource || minimal ) {

				final IRI item=iri(request.item());
				final Shape shape=resource ? detail(request.shape()) : holder(request.shape());

				return filtered ? response.map(new Failure()

						.status(Response.NotImplemented)
						.cause("resource filtered retrieval not supported")

				) : request.query(rdf, and(all(item), shape)).fold(

						query -> graph.exec(connection -> {

							final Collection<Statement> model=fetch(connection, item, query);

							return response

									.status(resource && model.isEmpty() ? NotFound : OK) // !!! 410 Gone if previously known

									.header("+Preference-Applied",
											minimal ? include(LDP.PREFER_MINIMAL_CONTAINER) : ""
									)

									.shape(query.map(new Query.Probe<Shape>() {

										@Override public Shape probe(final Items items) {
											return items.getShape(); // !!! add ldp:contains if items.path is not empty
										}

										@Override public Shape probe(final Stats stats) {
											return GraphEngine.StatsShape;
										}

										@Override public Shape probe(final Terms terms) {
											return GraphEngine.TermsShape;
										}

									}))

									.body(rdf, model);

						}),

						response::map

				);

			} else {

				final IRI item=iri(request.item());

				final Shape holder=holder(request.shape());
				final Shape digest=digest(request.shape());

				// containers are currently virtual and respond always with 200 OK even if not described in the graph

				return request.query(rdf, digest).fold(

						query -> graph.exec(connection -> {

							final Collection<Statement> matches=fetch(connection, item, query);

							if ( filtered ) { // matches only

								return response
										.status(OK)
										.shape(query.map(new Query.Probe<Shape>() {

											@Override public Shape probe(final Items items) {
												return field(LDP.CONTAINS, items.getShape());
											}

											@Override public Shape probe(final Stats stats) {

												return GraphEngine.StatsShape;
											}

											@Override public Shape probe(final Terms terms) {
												return GraphEngine.TermsShape;
											}

										}))
										.body(rdf, matches);

							} else { // include container description

								// !!! 404 NotFound or 410 Gone if previously known for non-virtual containers

								matches.addAll(fetch(connection, item, items(holder)));

								return response
										.status(OK)
										.shape(and(holder, field(LDP.CONTAINS, digest)))
										.body(rdf, matches);

							}

						}),

						response::map

				);

			}

		}).map(response -> response.success() ? response
				.headers("+Vary", "Accept", "Prefer")
				.headers("+Link",
						"<"+LDP.RESOURCE+">; rel=\"type\"",
						"<"+LDP.RDF_SOURCE+">; rel=\"type\"",
						request.collection() ? "<"+LDP.CONTAINER+">; rel=\"type\"" : ""
				) : response
		);
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
