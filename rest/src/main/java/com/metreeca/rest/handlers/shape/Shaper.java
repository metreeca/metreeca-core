/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.handlers.shape;

import com.metreeca.form.*;
import com.metreeca.form.codecs.QueryParser;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Values;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.sys.Trace;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.Rio;
import org.eclipse.rdf4j.rio.turtle.TurtleWriter;

import java.io.IOException;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;

import static com.metreeca.form.Shape.empty;
import static com.metreeca.form.Shape.role;
import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.rest.Handler.error;
import static com.metreeca.rest.Handler.forbidden;
import static com.metreeca.rest.Handler.refused;
import static com.metreeca.tray._Tray.tool;

import static java.util.stream.Collectors.toList;


public abstract class Shaper implements Handler {

	private final Trace trace=tool(Trace.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected BiFunction<Request, Model, Model> chain(
			final BiFunction<Request, Model, Model> head, final BiFunction<Request, Model, Model> tail
	) {
		return (head == null) ? tail : (request, statements) -> tail.apply(request, head.apply(request, statements));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	protected void authorize(
			final Request request, final Response response,
			final Shape shape, final Consumer<Shape> delegate
	) {

		final Shape authorized=shape.accept(role(request.roles())); // !!! look for ldp:contains sub-shape

		if ( empty(shape) ) {

			forbidden(request, response);

		} else if ( empty(authorized) ) {

			refused(request, response);

		} else {

			delegate.accept(authorized);

		}
	}

	/*
	 * construct and process configured query, merging constraints from the query string
	 */
	protected void query(
			final Request request, final Response response,
			final Shape shape, final Consumer<Query> delegate
	) { // !!! refactor

		Query query=null;

		try {

			query=new QueryParser(shape).parse(request.query());

		} catch ( final RuntimeException e ) {

			response.status(Response.BadRequest).json(error("query-malformed", e));

		}

		if ( query != null ) {
			delegate.accept(query);
		}

	}

	protected void model(
			final Request request, final Response response,
			final Shape shape, final Consumer<Collection<Statement>> delegate
	) { // !!! refactor

		final IRI focus=request.focus();

		Collection<Statement> model=null; // user-submitted statements

		try {

			model=request.rdf(shape, focus);

		} catch ( final RDFParseException e ) {

			response.status(Response.BadRequest).json(error("data-malformed", e));

		}

		// @@@ already handled by RDFFormat.get()
		//if ( model != null ) {
		//
		//	model.addAll(shape.accept(mode(Form.verify)).accept(new Outliner(focus))); // shape-implied statements
		//
		//	delegate.accept(model);
		//}

	}


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

	protected Map<String, Object> report(final Report trace) {

		final Map<Issue.Level, List<Issue>> levels=new EnumMap<>(Issue.Level.class);

		trace.getIssues().forEach(issue -> levels.compute(issue.getLevel(), (level, current) -> {

			final List<Issue> updated=(current != null) ? current : new ArrayList<>();

			updated.add(issue);

			return updated;

		}));

		final Map<String, Object> map=new LinkedHashMap<>();

		Optional.ofNullable(levels.get(Issue.Level.Error)).ifPresent(errors ->
				map.put("errors", errors.stream().map(this::report).collect(toList())));

		Optional.ofNullable(levels.get(Issue.Level.Warning)).ifPresent(warnings ->
				map.put("warnings", warnings.stream().map(this::report).collect(toList())));

		trace.getFrames().forEach(frame -> {

			final String property=Values.format(frame.getValue());
			final Map<Object, Object> report=report(frame);

			if ( !report.isEmpty() ) {
				map.put(property, report);
			}
		});

		return map;
	}


	private Map<Object, Object> report(final Frame<Report> frame) {

		final Map<Object, Object> map=new LinkedHashMap<>();

		for (final Map.Entry<Step, Report> slot : frame.getSlots().entrySet()) {

			final String property=slot.getKey().format();
			final Map<String, Object> report=report(slot.getValue());

			if ( !report.isEmpty() ) {
				map.put(property, report);
			}
		}

		return map;
	}

	private Map<String, Object> report(final Issue issue) {

		final Map<String, Object> map=new LinkedHashMap<>();

		map.put("cause", issue.getMessage());
		map.put("shape", issue.getShape());

		final Set<Value> values=issue.getValues();

		if ( !values.isEmpty() ) {
			map.put("values", values.stream().map(Values::format).collect(toList()));
		}

		return map;
	}

}
