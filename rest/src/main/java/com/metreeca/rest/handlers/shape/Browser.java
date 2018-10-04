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


import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.sparql.SPARQLEngine;
import com.metreeca.rest.*;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.Shape.empty;
import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.queries.Items.ItemsShape;
import static com.metreeca.form.queries.Stats.StatsShape;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Sets.union;
import static com.metreeca.form.things.Values.rewrite;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.ShapeFormat.shape;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;


/**
 * Basic container browser.
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldpbc">Linked Data Platform 1.0 - §5.3 Basic</a>
 */
public final class Browser extends Actor<Browser> {


	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");

	private void get(final Request request, final Shape shape) {

		final String representation=request.headers("Prefer")
				.stream()
				.map(value -> {

					final Matcher matcher=RepresentationPattern.matcher(value);

					return matcher.matches() ? matcher.group("representation") : "";

				})
				.filter(value -> !value.isEmpty())
				.findFirst()
				.orElse("");

		final IRI target=request.item();
		final String query=request.query();

		if ( representation.equals("http://www.w3.org/ns/ldp#PreferMinimalContainer")
				|| representation.equals("http://www.w3.org/ns/ldp#PreferEmptyContainer") ) {

			// !!! handle multiple uris in include parameter
			// !!! handle omit parameter (with multiple uris)
			// !!! handle other representation values in https://www.w3.org/TR/ldp/#prefer-parameters

			// !!! include Preference-Applied response header
			// !!! include Vary response header?


		} else {



		}

	}

	
	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {
		return handler(Form.relate, Form.digest, shape ->

				empty(shape) ? direct(request) : driven(request, shape)

		).handle(request)

				.map(response -> response.headers("+Link",

						link(LDP.CONTAINER, "type"),
						link(LDP.BASIC_CONTAINER, "type")

				))

				.map(response -> response.success() ?

						response.headers("Vary", "Accept", "Prefer") : response

				);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder direct(final Request request) {
		return request.reply(response -> response.map(new Failure<>()
				.status(Response.NotImplemented)
				.cause("shapeless container browsing not supported"))
		);
	}

	private Responder driven(final Request request, final Shape shape) {
		return request.reply(response -> request.query(shape).map(

				query -> graph.query(connection -> {

					final IRI focus=request.item();

					// retrieve filtered content from repository

					final Map<Value, Collection<Statement>> matches=new SPARQLEngine(connection).browse(query);

					// !!! @@@ add implied statements

					final Collection<Statement> model=matches.values().stream().reduce(emptyList(), (x, y) -> concat(x, y)); // !!! !!! review


					return response.status(Response.OK).map(r -> query.accept(new Query.Probe<Response>() { // !!! factor

						@Override public Response visit(final Edges edges) {
							return r.body(shape()).set(trait(LDP.CONTAINS, shape.accept(mode(Form.verify)))) // hide filtering constraints
									.body(rdf()).set(union(model, matches.keySet().stream()
											.map(item -> statement(focus, LDP.CONTAINS, item))
											.collect(toList())
									));
						}

						@Override public Response visit(final Stats stats) {
							return r.body(shape()).set(StatsShape)
									.body(rdf()).set(rewrite(model, Form.meta, focus));
						}

						@Override public Response visit(final Items items) {
							return r.body(shape()).set(ItemsShape)
									.body(rdf()).set(rewrite(model, Form.meta, focus));
						}

					}));

				}),

				response::map

		));
	}

}
