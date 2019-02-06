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
import com.metreeca.form.queries.Edges;
import com.metreeca.rest.*;
import com.metreeca.rest.engines._SPARQLEngine;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.queries.Items.ItemsShape;
import static com.metreeca.form.queries.Stats.StatsShape;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.wrappers.Throttler.entity;
import static com.metreeca.rest.wrappers.Throttler.resource;
import static com.metreeca.tray.Tray.tool;


/**
 * LDP resource relator.
 *
 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item() focus
 * item}.</p>
 *
 * <p>If the focus item is a {@linkplain Request#container() container} and the request includes an expected
 * {@linkplain Message#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>!!!</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the focus item is a {@linkplain Request#container() container}:</p>
 *
 * <ul>
 *
 * <li>!!!</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the request includes an expected {@linkplain Message#shape() resource shape}:</p>
 *
 * <ul>
 *
 * <li>the response includes the derived shape actually used in the retrieval process, redacted according to request
 * user {@linkplain Request#roles() roles}, {@link Form#relate} task, {@link Form#detail} view and {@link Form#verify}
 * mode.</li>
 *
 * <li>the response {@link RDFFormat RDF body} contains the RDF description of the request focus, as matched by the
 * redacted request shape.</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the response {@link RDFFormat RDF body} contains the symmetric concise bounded description of the request focus
 * item, extended with {@code rdfs:label/comment} annotations for all referenced IRIs.</li>
 *
 * </ul>
 *
 * <p>Regardless of the operating mode, RDF data is retrieved from the system {@linkplain Graph#Factory graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */


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

public final class Relator extends Actor {

	private final Graph graph=tool(Graph.Factory);


	public Relator() {
		delegate(relator().with(annotator()).with(throttler()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper annotator() {
		return handler -> request -> handler.handle(request).map(response ->
				response.headers("+Vary", "Accept", "Prefer")
		);
	}

	private Wrapper throttler() {
		return wrapper(Request::container,
				new Throttler(Form.relate, Form.digest, entity()),
				new Throttler(Form.relate, Form.detail, resource())
		);
	}

	private Handler relator() {
		return request -> request.reply(response -> {

			return engine(request.shape())

					.relate(request.item())

					.map(model -> response
							.status(Response.OK)
							.shape(request.shape())
							.body(rdf(), model)
					)

					.orElseGet(() -> response.
							status(Response.NotFound) // !!! 410 Gone if previously known
					);

			//request.query(request.shape()).fold(
			//
			//		value -> value.map(query -> query.map(new Query.Probe<Response>() {
			//
			//			@Override public Response probe(final Edges edges) {
			//				throw new UnsupportedOperationException("to be implemented"); // !!! tbi
			//			}
			//
			//			@Override public Response probe(final Stats stats) {
			//				throw new UnsupportedOperationException("to be implemented"); // !!! tbi
			//			}
			//
			//			@Override public Response probe(final Items items) {
			//				throw new UnsupportedOperationException("to be implemented"); // !!! tbi
			//			}
			//
			//		})).orElseGet(() -> {
			//
			//
			//		}),
			//
			//		response::map
			//
			//)

		});
	}


	//// !!! ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


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


			//return r
			//		.shape(shape // !!! cache returned shape
			//				.map(new Redactor(Form.mode, Form.verify)) // hide filtering constraints
			//				.map(new Optimizer())
			//		)
			//		.body(rdf(), model);

			return response.status(Response.OK)
					.body(rdf(), model);
		});
	}

	private Response stats(final Query stats, final Response response) {

		//return r.shape(StatsShape)
		//		.body(rdf(), rewrite(_SPARQLEngine.meta, item, model));

		return graph.query(connection -> {
			return response.status(Response.OK)
					.shape(StatsShape)
					.body(rdf(), new _SPARQLEngine(connection).browse(stats, response.item()));

		});
	}

	private Response items(final Query items, final Response response) {

		//return r.shape(ItemsShape)
		//		.body(rdf(), rewrite(_SPARQLEngine.meta, item, model));

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
