/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.handlers._virtual;


import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.engines.SPARQLEngine;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.Shape.optional;
import static com.metreeca.form.Shape.verify;
import static com.metreeca.form.Shape.wild;
import static com.metreeca.form.queries.Items.ItemsShape;
import static com.metreeca.form.queries.Stats.StatsShape;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;


/**
 * Virtual basic container browser.
 *
 * <p>Handles retrieval requests on the virtual linked data basic resource container identified by the request
 * {@linkplain Request#item() focus item}.</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldpbc">Linked Data Platform 1.0 - §5.3 Basic</a>
 * @deprecated work in progress
 */
@Deprecated public final class VBrowser extends Actor<VBrowser> {

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);


	public VBrowser() {
		delegate(action(Form.relate, Form.digest).wrap((Request request) -> (

				wild(request.shape()) ? direct(request) : driven(request))

				.map(response -> response.headers("+Link",

						link(LDP.CONTAINER, "type"),
						link(LDP.BASIC_CONTAINER, "type")

				))

				.map(response -> response.success() ?

						response.headers("Vary", "Accept", "Prefer") : response

				)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public VBrowser post(final BiFunction<Response, Model, Model> filter) { return super.post(filter); }

	@Override public VBrowser sync(final String script) { return super.sync(script); }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder direct(final Request request) {
		return request.reply(new Failure()
				.status(Response.NotImplemented)
				.cause("shapeless container browsing not supported")
		);
	}

	private Responder driven(final Request request) {
		return request.reply(response -> request.query(request.shape()).fold(

				query -> query.map(new Query.Probe<Response>() {

					@Override public Response probe(final Edges edges) {
						return edges(edges, response);
					}

					@Override public Response probe(final Stats stats) { return stats(stats, response); }

					@Override public Response probe(final Items items) { return items(items, response); }

				}),

				response::map

		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Response edges(final Edges edges, final Response response) {
		return graph.query(connection -> {

			final Request request=response.request();
			final IRI item=response.item();

			final Collection<Statement> model=new ArrayList<>();

			model.add(statement(item, RDF.TYPE, LDP.BASIC_CONTAINER));

			if ( include(request, LDP.PREFER_EMPTY_CONTAINER) ) {

				response.header("Preference-Applied", String.format(
						"return=representation; include=\"%s\"", LDP.PREFER_EMPTY_CONTAINER
				));

			} else {

				new SPARQLEngine(connection).browse(edges).forEach((focus, statements) -> {
					model.add(statement(item, LDP.CONTAINS, focus));
					model.addAll(statements);
				});

			}

			return response.status(Response.OK)
					.shape(and( // !!! provisional support for container metadata (replace with wildcard trait)
							trait(RDFS.LABEL, verify(optional(), datatype(XMLSchema.STRING))),
							trait(RDFS.COMMENT, verify(optional(), datatype(XMLSchema.STRING))),
							trait(LDP.CONTAINS, edges.getShape())
					))
					.body(rdf(), model);
		});
	}

	private Response stats(final Query stats, final Response response) {
		return graph.query(connection -> {
			return response.status(Response.OK)
					.shape(StatsShape)
					.body(rdf(), new SPARQLEngine(connection).browse(stats, response.item()));

		});
	}

	private Response items(final Query items, final Response response) {
		return graph.query(connection -> {
			return response.status(Response.OK)
					.shape(ItemsShape)
					.body(rdf(), new SPARQLEngine(connection).browse(items, response.item()));

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean include(final Request request, final Value include) {

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
