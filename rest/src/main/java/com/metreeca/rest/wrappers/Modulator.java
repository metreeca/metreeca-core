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

import com.metreeca.form.*;
import com.metreeca.form.probes.Extractor;
import com.metreeca.form.things.Sets;
import com.metreeca.form.things.Structures;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.*;

import java.util.*;

import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.Report.report;
import static com.metreeca.form.Shape.empty;
import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.things.Sets.intersection;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.rest.Handler.forbidden;
import static com.metreeca.rest.Handler.refused;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.formats.RDFFormat.rdf;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.toList;


/**
 * Shape-based content modulator.
 *
 * <p>Controls resource access and redacts message content according to </p>
 *
 * <ul>
 *
 * <li>redact request and response {@linkplain Message#shape() shape} according to task/view parameters {@linkplain
 * #action(IRI, IRI) provided} by the concrete implementation;</li>
 *
 * <li>enforces role-based access control; access to the managed resource action is public, unless explicitly
 * {@linkplain #role(Value...) limited} to specific user roles;</li>
 *
 * </ul>
 */
public final class Modulator implements Wrapper { // !!! tbd

	//	 * <p>If the request includes a {@linkplain Message#shape() shape}, the filtered model is trimmed to remove
	//	 * statements outside the allowed shape envelope.</p>

	//	 * <p>If the response includes a {@linkplain Message#shape() shape}, the filtered model is trimmed to remove
	//	 * statements outside the allowed shape envelope.</p>

	private Value task=Form.any;
	private Value view=Form.any;

	private Set<Value> roles=emptySet();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Modulator task(final Value task) { // !!! tbd

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		this.task=task;

		return this;
	}

	public Modulator view(final Value view) { // !!! tbd

		if ( view == null ) {
			throw new NullPointerException("null view");
		}

		this.view=view;

		return this;
	}


	/**
	 * Configures the permitted roles.
	 *
	 * @param roles the user {@linkplain Request#roles(Value...) roles} enabled to perform the resource action managed
	 *              by the wrapped handler; empty for public access; may be further restricted by role-based annotations
	 *              in the {@linkplain Request#shape() request shape}, if one is present
	 *
	 * @return this actor
	 *
	 * @throws NullPointerException if either {@code roles} is null or contains null values
	 */
	public Modulator role(final Value... roles) {
		return role(asList(roles));
	}

	/**
	 * Configures the permitted roles.
	 *
	 * @param roles the user {@linkplain Request#roles(Value...) roles} enabled to perform the resource action managed
	 *              by the wrapped handler; empty for public access; may be further restricted by role-based annotations
	 *              in the {@linkplain Request#shape() request shape}, if one is present
	 *
	 * @return this modulator
	 *
	 * @throws NullPointerException if either {@code roles} is null or contains null values
	 */
	public Modulator role(final Collection<? extends Value> roles) {

		if ( roles == null ) {
			throw new NullPointerException("null roles");
		}

		if ( roles.contains(null) ) {
			throw new NullPointerException("null role");
		}

		this.roles=new HashSet<>(roles);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return pre().wrap(post()).wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper pre() {
		return handler -> request -> {

			final Shape shape=request.shape();
			final Set<Value> roles=roles(request);

			if ( pass(shape) ) {

				if ( !this.roles.isEmpty() && roles.isEmpty() ) {

					return refused(request);

				} else {

					final Collection<Statement> model=request.body(rdf()).value().orElseGet(Sets::set);
					final Collection<Statement> envelope=Structures.network(request.item(), model);

					final Report report=report(model.stream()
							.filter(statement -> !envelope.contains(statement))
							.map(outlier -> issue(Issue.Level.Error, "statement outside cell envelope "+outlier, pass()))
							.collect(toList())
					);

					if ( report.assess(Issue.Level.Error) ) {

						return request.reply(new Failure()
								.status(Response.UnprocessableEntity)
								.error(Failure.DataInvalid)
								.trace(report)
						);

					} else {

						return handler.handle(request);

					}

				}

			} else { // !!! cache redacted shapes?

				final Shape redacted=shape
						.map(Shape.task(task))
						.map(Shape.view(view));

				final Shape authorized=redacted
						.map(Shape.role(roles));

				return empty(redacted) ? forbidden(request)
						: empty(authorized) ? refused(request)
						: handler.handle(request.shape(authorized));

			}

		};
	}

	private Wrapper post() { // !!! cache redacted shapes?
		return handler -> request -> handler.handle(request).map(response -> {

			final Shape shape=response.shape();

			if ( pass(shape) ) {

				return response
						.pipe(rdf(), rdf -> Value(Structures.network(request.item(), rdf)));

			} else {

				final Shape redacted=shape
						.map(Shape.task(task))
						.map(Shape.view(view))
						.map(Shape.role(roles(request)))
						.map(Shape.mode(Form.verify));

				return response
						.shape(redacted)
						.pipe(rdf(), model -> Value(empty(redacted)? set()
								: redacted.map(new Extractor(model, singleton(request.item()))).collect(toList())
						));

			}

		});
	}


	private Set<Value> roles(final Request request) {
		return roles.isEmpty() ? request.roles() : intersection(roles, request.roles());
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
