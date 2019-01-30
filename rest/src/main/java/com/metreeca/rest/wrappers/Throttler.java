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

package com.metreeca.rest.wrappers;

import com.metreeca.form.Form;
import com.metreeca.form.Issue;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Extractor;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.things.Structures;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Shape.empty;
import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Structures.description;
import static com.metreeca.form.things.Structures.network;
import static com.metreeca.rest.Handler.forbidden;
import static com.metreeca.rest.Handler.refused;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.RDFFormat.rdf;

import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;


/**
 * Content throttler.
 *
 * <p>Controls resource access and throttles content visibility according to user {@linkplain Request#roles() roles}
 * and message {@linkplain Message#shape() shapes}.</p>
 *
 * <p>If the request includes a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>redacts the request shape according to the provided {@linkplain #Throttler(Value, Value) task/view
 * parameters} and the request user roles ({@code mode} redaction is left to final shape consumers);</li>
 *
 * <li>enforces shape-based access control according to the redacted request shape;</li>
 *
 * <li>ensures that the request {@linkplain RDFFormat RDF payload} doesn't contains statements outside the allowed
 * envelope of the redacted shape;</li>
 *
 * <li>associates the redacted shape to the request forwarded to the wrapped handler.</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the request doesn't include a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>ensures that the request {@linkplain RDFFormat RDF payload} doesn't contains statements exceeding the
 * {@linkplain Structures#description(Resource, boolean, Iterable) symmetric concise bounded description} of the request
 * focus {@linkplain Message#item() item}.</li>
 * </ul>
 *
 * <p>If the response includes a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>redacts the response shape according to the provided {@linkplain #Throttler(Value, Value) task/view
 * parameters} and the request user roles, hiding also filtering-only shapes;</li>
 *
 * <li>trims {@linkplain RDFFormat RDF payload} statements exceeding the allowed envelope of the redacted shape;</li>
 *
 * <li>associates the redacted shape to the response forwarded to the consumer.</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the response doesn't include a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>trims {@linkplain RDFFormat RDF payload} statements exceeding the {@linkplain Structures#network(IRI, Iterable)
 * connectivity network} of the response focus {@linkplain Message#item() item}.</li>
 *
 * </ul>
 */
public final class Throttler implements Wrapper {

	private final Value task;
	private final Value view;

	private final Map<Map<IRI, Set<? extends Value>>, Shape> cache=new HashMap<>();


	/**
	 * Creates a throttler
	 *
	 * @param task a IRI identifying the {@linkplain Form#task task} to be performed by the wrapped handler
	 * @param view a IRI identifying the {@linkplain Form#view view} level for the wrapped handler
	 *
	 * @throws NullPointerException if either {@code task} or {@code view} is null
	 */
	public Throttler(final Value task, final Value view) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( view == null ) {
			throw new NullPointerException("null view");
		}

		this.task=task;
		this.view=view;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return pre().wrap(post()).wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper pre() {
		return handler -> request -> {

			final Shape shape=request.shape();
			final Set<Value> roles=request.roles();

			if ( pass(shape) ) {

				return request.body(rdf()).fold(

						value -> outliers(value, description(request.item(), false, value))
								.map(request::reply)
								.orElseGet(() -> handler.handle(request)),

						request::reply

				);

			} else {

				final Shape general=shape(shape, Form.verify, set(Form.any));
				final Shape authorized=shape(shape, Form.verify, roles);

				if ( empty(general) ) {

					return forbidden(request);

				} else if ( empty(authorized) ) {

					return refused(request);

				} else {

					final Shape redacted=shape(shape, null, roles);

					return request.body(rdf()).fold(

							value -> outliers(value, envelope(request.item(), authorized, value))
									.map(request::reply)
									.orElseGet(() -> handler.handle(request.shape(redacted))),

							request::reply

					);

				}

			}

		};
	}

	private Wrapper post() {
		return handler -> request -> handler.handle(request).map(response -> {

			final IRI focus=request.item();
			final Shape shape=response.shape();

			if ( pass(shape) ) {

				return response
						.pipe(rdf(), rdf -> Value(network(focus, rdf)));

			} else {

				final Shape redacted=shape(shape, Form.verify, request.roles());

				return response
						.shape(redacted)
						.pipe(rdf(), model -> Value(empty(redacted) ? set() : envelope(focus, redacted, model)));

			}

		});
	}


	private Shape shape(final Shape shape, final IRI mode, final Set<Value> roles) {
		return cache.computeIfAbsent(

				mode == null ? map(
						entry(Form.task, set(task)),
						entry(Form.view, set(view)),
						entry(Form.role, roles)
				) : map(
						entry(Form.task, set(task)),
						entry(Form.view, set(view)),
						entry(Form.mode, set(mode)),
						entry(Form.role, roles)
				),

				variables -> shape.map(new Redactor(variables)).map(new Optimizer())

		);
	}

	private Set<Statement> envelope(final Value focus, final Shape shape, final Collection<Statement> model) {
		return shape.map(new Extractor(model, singleton(focus)))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private Optional<Failure> outliers(final Collection<Statement> value, final Collection<Statement> envelope) {

		final List<Issue> outliers=value.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> Issue.issue(Issue.Level.Error, "statement outside allowed envelope "+outlier))
				.collect(toList());

		return outliers.isEmpty() ? Optional.empty() : Optional.of(new Failure()
				.status(Response.UnprocessableEntity)
				.error(Failure.DataInvalid)
				.trace(focus(outliers))
		);
	}

}
