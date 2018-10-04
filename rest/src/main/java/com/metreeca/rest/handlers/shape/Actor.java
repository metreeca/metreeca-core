/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers.shape;

import com.metreeca.form.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;
import com.metreeca.rest.formats.ShapeFormat;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.*;
import java.util.function.Function;

import javax.json.*;

import static com.metreeca.form.Shape.*;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Sets.intersection;
import static com.metreeca.form.things.Values.format;
import static com.metreeca.rest.Handler.forbidden;
import static com.metreeca.rest.Handler.refused;
import static com.metreeca.tray.Tray.tool;

import static java.util.Arrays.asList;
import static java.util.Collections.disjoint;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.groupingBy;


/**
 * Resource actor.
 *
 * <p>Handles a specific action on a linked data resource.</p>
 *
 * <p>The abstract base:</p>
 *
 * <ul>
 *
 * <li>looks for a {@linkplain ShapeFormat shape} body in incoming requests and redact it according to task/view
 * parameters {@linkplain #handler(IRI, IRI, Function) provided} by the concrete implementation;</li>
 *
 * <li>enforces role-based access control; Access to the managed resource action is public, unless explicitly
 * {@linkplain #roles(Value...) limited} to
 * * specific user roles;</li>
 *
 * <li>adds default LDP {@code Link} response headers for RDF sources.</li>
 *
 * </ul>
 *
 * <dl>
 *
 * <dt>Request {@link ShapeFormat} body {optional}</dt>
 * <dd>An optional linked data shape driving action processing.</dd>
 *
 * </dl>
 *
 * @param <T> the self-bounded message type supporting fluent setters
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldprs">Linked Data Platform 1.0 - §4.3 RDF Source</a>
 */
public abstract class Actor<T extends Actor<T>> implements Handler {

	private final Trace trace=tool(Trace.Factory);


	/**
	 * Creates a {@code Link} header value.
	 *
	 * @param resource the target resource to be linked through the header
	 * @param relation the relation with the target {@code resource}
	 *
	 * @return the header value linking the target {@code resource} with the given {@code relation}
	 *
	 * @throws NullPointerException if either {@code resource} or {@code relation} is null
	 */
	public static String link(final IRI resource, final String relation) {

		if ( resource == null ) {
			throw new NullPointerException("null resource");
		}

		if ( relation == null ) {
			throw new NullPointerException("null relation");
		}

		return String.format("<%s>; rel=\"%s\"", resource, relation);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Set<Value> roles=emptySet();


	@SuppressWarnings("unchecked") private T self() { return (T)this; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the permitted roles.
	 *
	 * @param roles the user {@linkplain Request#roles(Value...) roles} enabled to perform the resource action managed
	 *              by this actor; empty for public access
	 *
	 * @return this actor
	 *
	 * @throws NullPointerException if either {@code roles} is null or contains null values
	 */
	public T roles(final Value... roles) {
		return roles(asList(roles));
	}

	/**
	 * Configures the permitted roles.
	 *
	 * @param roles the user {@linkplain Request#roles(Value...) roles} enabled to perform the resource action managed
	 *              by this actor; empty for public access
	 *
	 * @return this actor
	 *
	 * @throws NullPointerException if either {@code roles} is null or contains null values
	 */
	public T roles(final Collection<? extends Value> roles) {

		if ( roles == null ) {
			throw new NullPointerException("null roles");
		}

		if ( roles.contains(null) ) {
			throw new NullPointerException("null role");
		}

		this.roles=new HashSet<>(roles);

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a shape-driven action handler.
	 *
	 * @param task   a IRI identifying the {@linkplain Form#task task} to be performed by the generated handler
	 * @param view   a IRI identifying the {@linkplain Form#view view} level for the generated handler
	 * @param action a function mapping a resource shape to a resource responder; the possibly empty input shape is
	 *               retrieved from the request {@linkplain ShapeFormat shape} body, if one is available, and redacted
	 *               according to {@code task} and {@code view} parameters and the roles of the user performing the
	 *               request; {@code mode} redaction is left to final shape consumers
	 *
	 * @return an handler for the specified shape driven action
	 *
	 * @throws NullPointerException if any argument is null
	 */
	protected Handler handler(final IRI task, final IRI view, final Function<Shape, Responder> action) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( view == null ) {
			throw new NullPointerException("null view");
		}

		if ( action == null ) {
			throw new NullPointerException("null action");
		}

		return request -> request.body(ShapeFormat.shape()).map(

				shape -> {

					final Shape redacted=shape
							.accept(task(task))
							.accept(view(view));

					final Shape authorized=redacted
							.accept(role(roles.isEmpty() ? request.roles() : intersection(roles, request.roles())));

					return empty(redacted) ? forbidden(request)
							: empty(authorized) ? refused(request)
							: action.apply(authorized);

				},

				error -> {

					final boolean refused=!roles.isEmpty() && disjoint(roles, request.roles());

					return refused ? refused(request)
							: action.apply(and());

				}

		).map(response -> response.headers("+Link",
				link(LDP.RESOURCE, "type"),
				link(LDP.RDF_SOURCE, "type")
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Collection<Statement> trace(final Collection<Statement> model) {

		return model; // !!! enable once tracing with message suppliers is implemented

		//try (final StringWriter writer=new StringWriter()) {
		//
		//	Rio.write(model, new TurtleWriter(writer));
		//
		//	trace.debug(this, "processing model\n"+indent(writer, true));
		//
		//	return model;
		//
		//} catch ( final IOException e ) {
		//	throw new UncheckedIOException(e);
		//}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected JsonObject report(final Report report) {

		final Map<Issue.Level, List<Issue>> issues=report.getIssues().stream().collect(groupingBy(Issue::getLevel));

		final JsonObjectBuilder json=Json.createObjectBuilder();

		Optional.ofNullable(issues.get(Issue.Level.Error)).ifPresent(errors ->
				json.add("errors", report(errors, item -> report(item)))
		);

		Optional.ofNullable(issues.get(Issue.Level.Warning)).ifPresent(warnings ->
				json.add("warnings", report(warnings, item -> report(item)))
		);

		report.getFrames().forEach(frame -> {

			final String property=format(frame.getValue());
			final JsonObject value=report(frame);

			if ( !value.isEmpty() ) {
				json.add(property, value);
			}
		});

		return json.build();
	}


	private JsonObject report(final Frame<Report> frame) {

		final JsonObjectBuilder json=Json.createObjectBuilder();

		for (final Map.Entry<Step, Report> slot : frame.getSlots().entrySet()) {

			final String property=slot.getKey().format();
			final JsonObject value=report(slot.getValue());

			if ( !value.isEmpty() ) {
				json.add(property, value);
			}
		}

		return json.build();
	}

	private JsonObject report(final Issue issue) {

		final JsonObjectBuilder json=Json.createObjectBuilder();

		json.add("cause", issue.getMessage());
		json.add("shape", issue.getShape().toString());

		final Set<Value> values=issue.getValues();

		if ( !values.isEmpty() ) {
			json.add("values", report(values, v -> Json.createValue(format(v))));
		}

		return json.build();
	}

	private <V> JsonArray report(final Iterable<V> errors, final Function<V, JsonValue> reporter) {

		final JsonArrayBuilder json=Json.createArrayBuilder();

		errors.forEach(item -> json.add(reporter.apply(item)));

		return json.build();
	}

}
