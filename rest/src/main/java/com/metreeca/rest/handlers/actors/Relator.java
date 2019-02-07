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
import com.metreeca.form.queries.Edges;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.handlers.Actor;
import com.metreeca.rest.wrappers.Throttler;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.Collection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.Wrapper.wrapper;
import static com.metreeca.rest.formats.RDFFormat.rdf;
import static com.metreeca.rest.wrappers.Throttler.entity;
import static com.metreeca.rest.wrappers.Throttler.resource;

import static java.util.function.Function.identity;


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
 *
 * ---
 *
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
 * @see <a href="https://www.w3.org/TR/ldp/#ldpbc">Linked Data Platform 1.0 - §5.3 Basic</a>
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

			final IRI item=request.item();

			final boolean resource=!request.container();
			final boolean minimal=include(request, LDP.PREFER_MINIMAL_CONTAINER);
			final boolean total=request.query().isEmpty();

			final Engine engine=engine(request.shape());

			if ( resource || minimal ) {

				// !!! 410 Gone if previously known

				return engine

						.relate(item, request::query, (shape, model) -> response

								.status(resource && total && model.isEmpty() ? NotFound : OK )

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

										.relate(item, shape -> Value(new Edges(shape)), (cshape, cmodel) -> response
												.status(OK)
												.shape(and(cshape, field(LDP.CONTAINS, rshape)))
												.body(rdf(), union(cmodel, rmodel))
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
		return "return=representation; include=\"" +include+ "\"";
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


	private <V> Collection<V> union(final Collection<V> x, final Collection<V> y) {

		x.addAll(y);

		return x;
	}

}
