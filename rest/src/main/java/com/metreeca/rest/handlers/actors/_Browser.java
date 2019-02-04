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

package com.metreeca.rest.handlers.actors;


import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.rest.engines._SPARQLEngine;
import com.metreeca.form.queries.Edges;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Delegator;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.Shape.optional;
import static com.metreeca.form.Shape.verify;
import static com.metreeca.form.queries.Items.ItemsShape;
import static com.metreeca.form.queries.Stats.StatsShape;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.tray.Tray.tool;


/**
 * Basic container browser.
 *
 * <p>Handles retrieval requests on linked data basic container.</p>
 *
 * <dl>
 *
 * <dt>Response shape-driven {@link RDFFormat} body</dt>
 *
 * <dd>If the request includes {@linkplain Message#shape() shape}, the response includes the {@linkplain RDFFormat RDF
 * description} of the request {@linkplain Request#item() focus item}, {@linkplain LDP#CONTAINS containing} the RDF
 * descriptions of the virtual container items matched by the redacted linked data {@linkplain Shape shape}.</dd>
 *
 * <dd>If the request contains a {@code Prefer} header requesting the {@link LDP#PREFER_EMPTY_CONTAINER}
 * representation, virtual item descriptions are omitted.</dd>
 *
 * <dd>If the request contains a filtering {@linkplain Request#query(Shape) query}, only matching virtual container
 * item descriptionss are included.</dd>
 *
 * <dt>Response shapeless {@link RDFFormat} body</dt>
 *
 * <dd><strong>Warning</strong> / Shapeless container retrieval is not yet supported and is reported with a {@linkplain
 * Response#NotImplemented} HTTP status code.</dd>
 *
 * </dl>
 *
 * <p>If the request includes a shape, the response includes the derived shape actually used in the container retrieval
 * process, redacted according to request user {@linkplain Request#roles() roles}, {@link Form#relate} task, {@link
 * Form#verify} mode and {@link Form#digest} view.</p>
 *
 * <p>Regardless of the operating mode, RDF data is retrieved from the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldpbc">Linked Data Platform 1.0 - §5.3 Basic</a>
 */
public final class _Browser extends Delegator {

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);


	public _Browser() {
		delegate(Handler.handler(request -> !request.shaped(), direct(), driven())

						.with(new Throttler(Form.relate, Form.digest))

				//.with(response -> response.headers("+Link",
				//
				//		link(LDP.CONTAINER, "type"),
				//		link(LDP.BASIC_CONTAINER, "type")
				//
				//))
				//
				//.with(response -> response.success() ?
				//
				//		response.headers("Vary", "Accept", "Prefer") : response
				//
				//)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Handler direct() {
		return request -> request.reply(response -> response.map(new Failure()
				.status(Response.NotImplemented)
				.cause("shapeless container browsing not supported"))
		);
	}

	private Handler driven() {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		//return request -> request.reply(response -> request.query(request.shape()).map(
		//
		//		query -> query.map(new Query.Probe<Response>() {
		//
		//			@Override public Response visit(final Edges edges) {
		//				return edges(edges, response);
		//			}
		//
		//			@Override public Response visit(final Stats stats) { return stats(stats, response); }
		//
		//			@Override public Response visit(final Items items) { return items(items, response); }
		//
		//		}),
		//
		//		response::map
		//
		//));
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

				new _SPARQLEngine(connection).browse(edges).forEach((focus, statements) -> {
					model.add(statement(item, LDP.CONTAINS, focus));
					model.addAll(statements);
				});

			}

			return response.status(Response.OK)
					.shape(and( // !!! provisional support for container metadata (replace with wildcard trait)
							field(RDFS.LABEL, verify().then(optional(), datatype(XMLSchema.STRING))),
							field(RDFS.COMMENT, verify().then(optional(), datatype(XMLSchema.STRING))),
							field(LDP.CONTAINS, edges.getShape())
					))
					.body(rdf(), model);
		});
	}

	private Response stats(final Query stats, final Response response) {
		return graph.query(connection -> {
			return response.status(Response.OK)
					.shape(StatsShape)
					.body(rdf(), new _SPARQLEngine(connection).browse(stats, response.item()));

		});
	}

	private Response items(final Query items, final Response response) {
		return graph.query(connection -> {
			return response.status(Response.OK)
					.shape(ItemsShape)
					.body(rdf(), new _SPARQLEngine(connection).browse(items, response.item()));

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
