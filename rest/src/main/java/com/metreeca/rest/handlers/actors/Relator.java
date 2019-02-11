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
import com.metreeca.form.Shape;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.rest.Message.link;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.wrappers.Throttler.entity;
import static com.metreeca.rest.wrappers.Throttler.resource;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;


/**
 * LDP resource relator.
 *
 * <p>Handles retrieval requests on the linked data resource identified by the request {@linkplain Request#item() focus
 * item}.</p>
 *
 * <p>If the focus item is a {@linkplain Request#container() container} and the request includes an expected
 * {@linkplain Request#shape() shape}:</p>
 *
 * <ul>
 *
 * <li>the response includes the derived shape actually used in the {@linkplain Engine#browse(IRI) browsing} process,
 * redacted according to request user {@linkplain Request#roles() roles}, {@link Form#relate} task, {@link Form#convey}
 * mode and {@link Form#digest} view;</li>
 *
 * <li>the response {@linkplain RDFFormat RDF body} includes the RDF description of the container as matched by the
 * {@linkplain Throttler#container() container section} of redacted shape, linked using the {@code ldp:contains}
 * property to the RDF description of the container items matched by the {@linkplain Throttler#resource() resource
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
 * <li>the response {@linkplain RDFFormat RDF body} includes the symmetric concise bounded description of the
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
 * <li>the response includes the derived shape actually used in the {@linkplain Engine#relate(IRI) retrieval} process,
 * redacted according to request user {@linkplain Request#roles() roles}, {@link Form#relate} task, {@link Form#detail}
 * view and {@link Form#convey} mode;</li>
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
 * item, extended with {@code rdf:type} and {@code rdfs:label/comment} annotations for all referenced IRIs;</li>
 *
 * </ul>
 *
 * <p>Regardless of the operating mode, RDF data is retrieved from the system {@linkplain Graph#Factory graph}
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
				new Throttler(Form.relate, Form.digest, entity()),
				new Throttler(Form.relate, Form.detail, resource())
		);
	}

	private Handler relator() {
		return request -> request.reply(response -> {

			final IRI item=request.item();

			final boolean resource=!request.container();
			final boolean minimal=include(request, LDP.PREFER_MINIMAL_CONTAINER);
			final boolean total=request.query().isEmpty();

			final Engine engine=engine(request.shape());

			if ( resource || minimal ) {

				// !!! 410 Gone if previously known

				return engine

						.relate(item, request::query, (shape, model) -> response

								.status(resource && total && model.isEmpty() ? NotFound : OK)

								.header("+Preference-Applied",
										minimal ? include(LDP.PREFER_MINIMAL_CONTAINER) : ""
								)

								.shape(shape)
								.body(rdf(), model)

						)

						.fold(
								identity(),
								response::map
						);

			} else {

				// containers are currently virtual and respond always with 200 OK even if not described in the graph

				// !!! 404 NotFound or 410 Gone if previously known for non-virtual containers

				return engine

						.browse(item, request::query, (rshape, rmodel) -> {

							if ( total ) { // retrieve container description

								return engine

										.relate(item, shape -> Value(edges(shape)), (cshape, cmodel) -> response
												.status(OK)
												.shape(and(cshape, rshape))
												.body(rdf(), Stream
														.concat(cmodel.stream(), rmodel.stream())
														.collect(toList())
												)
										)

										.fold(identity(), unexpected -> response);

							} else {

								return response
										.status(OK)
										.shape(rshape)
										.body(rdf(), rmodel);

							}

						}).fold(
								identity(),
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
