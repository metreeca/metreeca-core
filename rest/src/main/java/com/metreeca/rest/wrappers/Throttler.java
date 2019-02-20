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
import com.metreeca.form.Shape;
import com.metreeca.form.probes.*;
import com.metreeca.form.things.Structures;
import com.metreeca.rest.*;
import com.metreeca.rest.bodies.RDFBody;
import com.metreeca.form.things.Shapes;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;

import static com.metreeca.form.probes.Evaluator.empty;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.Memoizing.memoizable;
import static com.metreeca.form.things.Structures.envelope;
import static com.metreeca.form.things.Structures.network;
import static com.metreeca.rest.Handler.forbidden;
import static com.metreeca.rest.Handler.refused;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.RDFBody.rdf;

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
 * <li>redacts the request shape according to the provided task, view and area {@linkplain #Throttler(Value, Value,
 * Function) parameters} and the request user roles; {@code mode} redaction is left to final shape consumers;</li>
 *
 * <li>enforces shape-based access control according to the redacted request shape;</li>
 *
 * <li>extends the {@linkplain RDFBody RDF payload} with the statements inferred from the redacted shape;</li>
 *
 * <li>associates the redacted shape to the request forwarded to the wrapped handler.</li>
 *
 * </ul>
 *
 * <p>If the response includes a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>redacts the response shape according to the provided {@linkplain #Throttler(Value, Value) task/view
 * parameters} and the request user roles, hiding also filtering-only shapes;</li>
 *
 * <li>extends the {@linkplain RDFBody RDF payload} with the statements inferred from the redacted shape;</li>
 *
 * <li>trims {@linkplain RDFBody RDF payload} statements exceeding the allowed envelope of the redacted shape;</li>
 *
 * <li>associates the redacted shape to the response forwarded to the consumer.</li>
 *
 * </ul>
 *
 * <p>Otherwise, if the response doesn't include a non-empty shape:</p>
 *
 * <ul>
 *
 * <li>trims {@linkplain RDFBody RDF payload} statements exceeding the {@linkplain Structures#network(Resource,
 * Iterable)} connectivity network} of the response focus {@linkplain Message#item() item}.</li>
 *
 * </ul>
 */
public final class Throttler implements Wrapper {

	private static final Function<Shape, Shape> anyone=memoizable(s -> s
			.map(new Redactor(Form.role))
			.map(new Optimizer())
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Function<Shape, Shape> common;
	private final Function<Shape, Shape> convey;


	/**
	 * Creates a throttler
	 *
	 * @param task a IRI identifying the {@linkplain Form#task task} to be performed by the wrapped handler
	 * @param view a IRI identifying the {@linkplain Form#view view} level for the wrapped handler
	 *
	 * @throws NullPointerException if either {@code task} or {@code view} is null
	 */
	public Throttler(final Value task, final Value view) {
		this(task, view, Shapes::entity);
	}

	/**
	 * Creates a throttler
	 *
	 * @param task a IRI identifying the {@linkplain Form#task task} to be performed by the wrapped handler
	 * @param view a IRI identifying the {@linkplain Form#view view} level for the wrapped handler
	 * @param area an operator extracting a specific area of interest from the shape associated with incoming requests
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public Throttler(final Value task, final Value view, final Function<Shape, Shape> area) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( view == null ) {
			throw new NullPointerException("null view");
		}

		if ( area == null ) {
			throw new NullPointerException("null area");
		}

		this.common=memoizable(shape -> shape
				.map(area)
				.map(new Redactor(Form.task, task))
				.map(new Redactor(Form.view, view))
				.map(new Optimizer())
		);

		this.convey=memoizable(shape -> shape
				.map(common)
				.map(new Redactor(Form.mode, Form.convey))
				.map(new Optimizer())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return pre().wrap(post()).wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper pre() {
		return handler -> request -> {

			final IRI focus=request.item();
			final Shape shape=request.shape();
			final Set<Value> roles=request.roles();

			if ( pass(shape) ) {

				return handler.handle(request);

			} else {

				// remove annotations and filtering-only constraints for authorization checks

				final Shape general=shape
						.map(convey)
						.map(anyone);

				final Shape authorized=shape
						.map(convey)
						.map(new Redactor(Form.role, roles))
						.map(new Optimizer());

				final Shape redacted=shape
						.map(common)
						.map(new Redactor(Form.role, roles))
						.map(new Optimizer());

				return empty(general) ? forbidden(request)
						: empty(authorized) ? refused(request)
						: handler.handle(request.shape(redacted)
						.pipe(rdf(), rdf -> Value(expand(focus, authorized, rdf)))
				);

			}

		};
	}

	private Wrapper post() {
		return handler -> request -> handler.handle(request).map(response -> {

			final IRI item=request.item();
			final Shape shape=response.shape().map(convey);

			if ( pass(shape) ) {

				return response.shape(shape)
						.pipe(rdf(), rdf -> Value(network(item, rdf)));

			} else {

				final Shape redacted=shape

						.map(new Redactor(Form.role, request.roles()))
						.map(new Optimizer());

				return response.shape(redacted)
						.pipe(rdf(), rdf -> Value(envelope(item, redacted, rdf)))
						.pipe(rdf(), rdf -> Value(expand(item, redacted, rdf)));

			}

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <V extends Collection<Statement>> V expand(final IRI resource, final Shape shape, final V model) {

		model.addAll(shape // add implied statements
				.map(new Outliner(resource)) // shape already redacted for convey mode
				.collect(toList())
		);

		return model;
	}

}
