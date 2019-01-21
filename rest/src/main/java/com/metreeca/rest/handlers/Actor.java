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

package com.metreeca.rest.handlers;

import com.metreeca.form.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;
import com.metreeca.rest.wrappers.Processor;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import javax.json.*;

import static com.metreeca.form.Shape.*;
import static com.metreeca.form.things.Sets.intersection;
import static com.metreeca.form.things.Strings.indent;
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
 * <p>Handles actions on linked data resources.</p>
 *
 * <p>The abstract base:</p>
 *
 * <ul>
 *
 * <li>redact request and response {@linkplain Message#shape() shape} according to task/view parameters {@linkplain
 * #action(IRI, IRI) provided} by the concrete implementation;</li>
 *
 * <li>enforces role-based access control; access to the managed resource action is public, unless explicitly
 * {@linkplain #roles(Value...) limited} to specific user roles;</li>
 *
 * <li>adds default LDP {@code Link} response headers for RDF sources.</li>
 *
 * </ul>
 *
 * @param <T> the self-bounded message type supporting fluent setters
 *
 * @see <a href="https://www.w3.org/TR/ldp/#ldprs">Linked Data Platform 1.0 - §4.3 RDF Source</a>
 */
public abstract class Actor<T extends Actor<T>> extends Delegator {

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

	private final Processor processor=new Processor();


	@SuppressWarnings("unchecked") private T self() { return (T)this; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the permitted roles.
	 *
	 * @param roles the user {@linkplain Request#roles(Value...) roles} enabled to perform the resource action managed
	 *              by this actor; empty for public access; may be further restricted by role-based annotations in the
	 *              {@linkplain Request#shape() request shape}, if one is present
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
	 *              by this actor; empty for public access; may be further restricted by role-based annotations in the
	 *              {@linkplain Request#shape() request shape}, if one is present
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
	 * Inserts a request RDF pre-processing filter.
	 *
	 * @param filter the request RDF request pre-processing filter to be inserted; takes as argument an incoming request
	 *               and its {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model; if the
	 *               request includes a {@linkplain Message#shape() shape}, the filtered model is trimmed to remove
	 *               statements outside the allowed envelope after shape redaction
	 *
	 * @return this actor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 * @see Processor#pre(BiFunction)
	 */
	protected T pre(final BiFunction<Request, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		processor.pre(filter);

		return self();
	}

	/**
	 * Inserts a response post-processing RDF filter.
	 *
	 * @param filter the response RDF post-processing filter to be inserted; takes as argument a successful outgoing
	 *               response and its {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model;
	 *               if the response includes a {@linkplain Message#shape() shape}, the filtered model is trimmed to
	 *               remove statements outside the allowed envelope after shape redaction
	 *
	 * @return this actor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 * @see Processor#post(BiFunction)
	 */
	protected T post(final BiFunction<Response, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		processor.post(filter);

		return self();
	}

	/**
	 * Inserts a SPARQL Update housekeeping script.
	 *
	 * @param script the SPARQL Update housekeeping script to be executed by this processor on successful request
	 *               processing; empty scripts are ignored
	 *
	 * @return this actor
	 *
	 * @throws NullPointerException if {@code script} is null
	 * @see Processor#sync(String)
	 */
	protected T sync(final String script) {

		if ( script == null ) {
			throw new NullPointerException("null script");
		}

		processor.sync(script);

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a shape-driven action wrapper.
	 *
	 * <p>Redacts request and response {@linkplain Message#shape() shapes} according to {@code task} and {@code view}
	 * parameters and the {@linkplain Request#roles() roles} of the user performing the request; response shape is also
	 * redacted according to the {@link Shape#verify(Shape...)} mode; request {@code mode} redaction is left to final
	 * shape consumers.</p>
	 *
	 * @param task a IRI identifying the {@linkplain Form#task task} to be performed by the wrapped handler
	 * @param view a IRI identifying the {@linkplain Form#view view} level for the wrapped handler
	 *
	 * @return an handler for the specified shape driven action
	 *
	 * @throws NullPointerException if either {@code task} or {@code view} is null
	 */
	protected Wrapper action(final IRI task, final IRI view) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( view == null ) {
			throw new NullPointerException("null view");
		}

		return query().wrap(headers())

				.wrap(pre(task, view)) // redact request shape before pre-processor shape-driven RDF payload trimming
				.wrap(processor)
				.wrap(post(task, view)); // redact response shape before post-processor shape-driven RDF payload trimming
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper query() {
		return handler -> request -> request.safe() || request.query().isEmpty()
				? handler.handle(request)
				: request.reply(new Failure().status(Response.BadRequest).cause("unexpected query parameters"));
	}

	private Wrapper headers() {
		return handler -> request -> handler.handle(request).map(response -> response.headers("+Link",
				link(LDP.RESOURCE, "type"),
				link(LDP.RDF_SOURCE, "type")
		));
	}


	private Wrapper pre(final IRI task, final IRI view) {
		return handler -> request -> {

			final Shape shape=request.shape();

			if ( wild(shape) ) {

				return !roles.isEmpty() && disjoint(roles, request.roles()) ?
						refused(request) : handler.handle(request);

			} else { // !!! cache redacted shapes?

				final Shape redacted=shape
						.accept(task(task))
						.accept(view(view));

				final Shape authorized=redacted
						.accept(role(roles(request)));

				return wild(redacted) || empty(redacted) ? forbidden(request)
						: wild(authorized) || empty(authorized) ? refused(request)
						: handler.handle(request.shape(authorized));

			}

		};
	}

	private Wrapper post(final IRI task, final IRI view) { // !!! cache redacted shapes?
		return handler -> request -> handler.handle(request).map(response -> response.shape(response.shape()
				.accept(task(task))
				.accept(view(view))
				.accept(mode(Form.verify))
				.accept(role(roles(request)))
		));
	}


	private Set<Value> roles(final Request request) {
		return roles.isEmpty() ? request.roles() : intersection(roles, request.roles());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Collection<Statement> trace(final Collection<Statement> model) {

		trace.entry(Trace.Level.Debug, this, () -> {

			try (final StringWriter writer=new StringWriter()) {

				Rio.write(model, new TurtleWriter(writer));

				return "processing model\n"+indent(writer, true);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		}, null);

		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected JsonObject report(final Report report) {

		final Map<Issue.Level, List<Issue>> issues=report.getIssues().stream().collect(groupingBy(Issue::getLevel));

		final JsonObjectBuilder json=Json.createObjectBuilder();

		Optional.ofNullable(issues.get(Issue.Level.Error)).ifPresent(errors ->
				json.add("errors", report(errors, this::report))
		);

		Optional.ofNullable(issues.get(Issue.Level.Warning)).ifPresent(warnings ->
				json.add("warnings", report(warnings, this::report))
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
