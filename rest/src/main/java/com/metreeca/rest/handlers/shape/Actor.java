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
import com.metreeca.form.things.Values;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.ShapeFormat;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.stream.Stream;

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
import static java.util.stream.Collectors.toList;


/**
 * Resource actor.
 *
 * <p>Handles a specific action on a linked data resource.</p>
 *
 * <p>Access to the managed resource action is public, unless explicitly  {@linkplain #roles(Value...) limited} to
 * specific user roles.</p>
 *
 * - vary header
 *
 * - link headers
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

	private final IRI task;
	private final IRI view;


	protected Actor(final IRI task, final IRI view) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( view == null ) {
			throw new NullPointerException("null view");
		}

		this.task=task;
		this.view=view;
	}


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

	@Override public Responder handle(final Request request) {
		return request.body(ShapeFormat.shape()).map(

				shape -> {  // !!! look for ldp:contains sub-shapes?

					final Shape redacted=shape
							.accept(task(task))
							.accept(view(view));

					final Shape authorized=redacted
							.accept(role(roles.isEmpty() ? request.roles() : intersection(roles, request.roles())));

					return empty(redacted) ? forbidden(request)
							: empty(authorized) ? refused(request)
							: shaped(request, authorized);

				},

				error -> {

					final boolean refused=!roles.isEmpty() && disjoint(roles, request.roles());

					return refused ? refused(request)
							: direct(request);

				}

		).map(response -> {

			if ( response.request().safe() && response.success() ) {
				response.headers("+Vary", "Accept", "Prefer");
			}

			return response.headers("Link",
					link(LDP.RDF_SOURCE, "type"),
					link(LDP.RESOURCE, "type")
			);

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected abstract Responder shaped(final Request request, final Shape shape);

	protected abstract Responder direct(final Request request);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected Collection<Statement> trace(final Collection<Statement> model) {
		try (final StringWriter writer=new StringWriter()) {

			Rio.write(model, new TurtleWriter(writer));

			trace.debug(this, "processing model\n"+indent(writer, true));

			return model;

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected JsonObject report(final Report trace) {

		final Map<Issue.Level, List<Issue>> levels=new EnumMap<>(Issue.Level.class);

		trace.getIssues().forEach(issue -> levels.compute(issue.getLevel(), (level, current) -> {

			final List<Issue> updated=(current != null) ? current : new ArrayList<>();

			updated.add(issue);

			return updated;

		}));

		final JsonObjectBuilder report=Json.createObjectBuilder();

		Optional.ofNullable(levels.get(Issue.Level.Error)).ifPresent(errors ->
				report.add("errors", report(errors.stream().map(this::report))));

		Optional.ofNullable(levels.get(Issue.Level.Warning)).ifPresent(warnings ->
				report.add("warnings", report(warnings.stream().map(this::report))));

		trace.getFrames().forEach(frame -> {

			final String property=format(frame.getValue());
			final JsonObject value=report(frame);

			if ( !value.isEmpty() ) {
				report.add(property, value);
			}
		});

		return report.build();
	}


	private JsonObject report(final Frame<Report> frame) {

		final JsonObjectBuilder report=Json.createObjectBuilder();

		for (final Map.Entry<Step, Report> slot : frame.getSlots().entrySet()) {

			final String property=slot.getKey().format();
			final JsonObject value=report(slot.getValue());

			if ( !value.isEmpty() ) {
				report.add(property, value);
			}
		}

		return report.build();
	}

	private JsonObject report(final Issue issue) {

		final JsonObjectBuilder report=Json.createObjectBuilder();

		report.add("cause", issue.getMessage());
		report.add("shape", issue.getShape().toString());

		final Set<Value> values=issue.getValues();

		if ( !values.isEmpty() ) {
			report.add("values", report(values.stream().map(Values::format)));
		}

		return report.build();
	}

	private JsonArray report(final Stream<?> stream) {
		return Json.createArrayBuilder(stream.collect(toList())).build();
	}

}
