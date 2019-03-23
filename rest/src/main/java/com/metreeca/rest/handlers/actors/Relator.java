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

package com.metreeca.rest.handlers.actors;

import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.things.Shapes;
import com.metreeca.rest.*;
import com.metreeca.rest.bodies.RDFBody;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.rest.Message.link;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.bodies.RDFBody.rdf;
import static com.metreeca.form.things.Shapes.container;
import static com.metreeca.form.things.Shapes.resource;


/**
 * LDP resource relator.
 *
 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item() focus
 * item}, according to the following operating modes.</p>
 *
 * <p>If the focus item is a {@linkplain Request#container() container} and the request includes an expected
 * {@linkplain Request#shape() shape}:</p>
 *
 * <ul>
 *
 * <li>the response includes the derived shape actually used in the retrieval process, redacted according to request
 * user {@linkplain Request#roles() roles}, {@link Form#relate} task, {@link Form#convey} mode and {@link Form#digest}
 * view;</li>
 *
 * <li>the response {@linkplain RDFBody RDF body} includes the RDF description of the container as matched by the
 * {@linkplain Shapes#container(Shape) container section} of redacted shape, linked using the {@code ldp:contains}
 * property to the RDF description of the container items matched by the {@linkplain Shapes#resource(Shape) resource
 * section} of redacted shape;</li>
 *
 * <li>contained items are selected as required by the LDP container profile {@linkplain
 * com.metreeca.rest.handlers.actors identified} by {@code rdf:type} and LDP properties in the request shape;</li>
 *
 * <li>if the request contains a filtering {@linkplain Request#query(Shape) query}, only matching container item
 * descriptions are included.</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the focus item is a {@linkplain Request#container() container}:</p>
 *
 * <ul>
 *
 * <li>the response {@linkplain RDFBody RDF body} includes the symmetric concise bounded description of the
 * container, linked using the {@code ldp:contains} property to the symmetric concise bounded description of the
 * container items, extended with {@code rdf:type} and {@code rdfs:label/comment} annotations for all referenced
 * IRIs;</li>
 *
 * <li>contained items are selected handling the target resource as an LDP Basic Container, that is on the basis of the
 * {@code ldp:contains} property.</li>
 *
 * </ul>
 *
 * <p><strong>Warning</strong> / Filtered browsing isn't yet supported on shape-less container.</p>
 *
 * <p>In both cases:</p>
 *
 * <ul>
 *
 * <li>if the request contains a {@code Prefer} header requesting the {@code ldp:preferMinimalContainer}
 * representation, item descriptions are omitted.</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the request includes an expected {@linkplain Request#shape() shape}:</p>
 *
 * <ul>
 *
 * <li>the response includes the derived shape actually used in the retrieval process, redacted according to request
 * user {@linkplain Request#roles() roles}, {@link Form#relate} task, {@link Form#detail} view and {@link Form#convey}
 * mode;</li>
 *
 * <li>the response {@link RDFBody RDF body} contains the RDF description of the request focus, as matched by the
 * redacted request shape.</li>
 *
 * </ul>
 *
 * <p>Otherwise:</p>
 *
 * <ul>
 *
 * <li>the response {@link RDFBody RDF body} contains the symmetric concise bounded description of the request focus
 * item, extended with {@code rdf:type} and {@code rdfs:label/comment} annotations for all referenced IRIs;</li>
 *
 * </ul>
 *
 * <p>Regardless of the operating mode, RDF data is retrieved from the system {@linkplain Graph#graph() graph}
 * database.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Relator extends Actor {

	private static final Pattern RepresentationPattern=Pattern
			.compile("\\s*return\\s*=\\s*representation\\s*;\\s*include\\s*=\\s*\"(?<representation>[^\"]*)\"\\s*");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Relator() {
		delegate(relator().with(annotator()).with(throttler()));
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
				new Throttler(Form.relate, Form.digest, Shapes::entity),
				new Throttler(Form.relate, Form.detail, Shapes::resource)
		);
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

							final Collection<Statement> model=relate(item, query);

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

							final Collection<Statement> matches=relate(item, query);

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
												relate(item, edges(resource(item, container(shape))))
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
