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

package com.metreeca.rest.handlers.work.wrappers;

import com.metreeca.form.Form;
import com.metreeca.form.Issue;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Extractor;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.things.Sets;
import com.metreeca.form.things.Structures;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.stream.Collectors;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.Shape.empty;
import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Structures.description;
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
 * <li>ensures that the request {@linkplain RDFFormat RDF payload} doesn't contains statements exceeding the {@linkplain
 * Structures#description(Resource, boolean, Iterable) symmetric concise bounded description} of the request focus
 * {@linkplain Message#item() item}.</li>
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

			if ( pass(shape) ) {

				final Collection<Statement> model=request.body(rdf()).value().orElseGet(Sets::set);
				final Collection<Statement> envelope=description(request.item(), false, model);

				final List<Issue> outliers=outliers(model, envelope);

				return outliers.isEmpty() ? handler.handle(request) : request.reply(new Failure()
						.status(Response.UnprocessableEntity)
						.error(Failure.DataInvalid)
						.trace(focus(outliers))
				);

			} else {

				final Shape general=shape.map(new Redactor(map(
						entry(Form.task, set(task)),
						entry(Form.view, set(view)),
						entry(Form.mode, set(Form.verify)),
						entry(Form.role, set(Form.any))
				))).map(new Optimizer());

				final Shape authorized=shape.map(new Redactor(map(
						entry(Form.task, set(task)),
						entry(Form.view, set(view)),
						entry(Form.mode, set(Form.verify)),
						entry(Form.role, request.roles())
				))).map(new Optimizer());


				if ( empty(general) ) {

					return forbidden(request);

				} else if ( empty(authorized) ) {

					return refused(request);

				} else {

					final Shape redacted=shape.map(new Redactor(map(
							entry(Form.task, set(task)),
							entry(Form.view, set(view)),
							entry(Form.role, request.roles())
					))).map(new Optimizer());


					final Collection<Statement> model=request.body(rdf()).value().orElseGet(Sets::set);
					final Collection<Statement> envelope=envelope(model, authorized, request.item());

					final List<Issue> outliers=outliers(model, envelope);

					return outliers.isEmpty() ? handler.handle(request.shape(redacted)) : request.reply(new Failure()
							.status(Response.UnprocessableEntity)
							.error(Failure.DataInvalid)
							.trace(focus(outliers))
					);

				}

			}

		};
	}

	private Wrapper post() {
		return handler -> request -> handler.handle(request).map(response -> {

			final Shape shape=response.shape();

			if ( pass(shape) ) {

				return response
						.pipe(rdf(), rdf -> Value(Structures.network(request.item(), rdf)));

			} else {

				final Shape redacted=shape
						.map(Shape.task(task))
						.map(Shape.view(view))
						.map(Shape.role(request.roles()))
						.map(Shape.mode(Form.verify));

				return response
						.shape(redacted)
						.pipe(rdf(), model -> Value(empty(redacted) ? set() : envelope(model, redacted, request.item())));

			}

		});
	}


	private Set<Statement> envelope(final Collection<Statement> model, final Shape shape, final Value focus) {
		return shape.map(new Extractor(model, singleton(focus)))
				.collect(Collectors.toCollection(LinkedHashSet::new));
	}

	private List<Issue> outliers(final Collection<Statement> model, final Collection<Statement> envelope) {
		return model.stream()
				.filter(statement -> !envelope.contains(statement))
				.map(outlier -> issue(Issue.Level.Error, "statement outside allowed envelope "+outlier, pass()))
				.collect(toList());
	}

}
