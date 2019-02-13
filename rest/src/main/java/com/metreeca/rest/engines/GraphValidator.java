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

package com.metreeca.rest.engines;

import com.metreeca.form.*;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Values;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Snippets.source;
import static com.metreeca.form.things.Values.*;
import static com.metreeca.rest.engines.GraphProcessor.list;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.common.iteration.Iterations.stream;

import static java.util.Collections.disjoint;
import static java.util.Collections.emptySet;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.*;


final class GraphValidator {

	private final Graph graph=tool(Graph.Factory);


	Focus validate(final IRI resource, final Shape shape, final Collection<Statement> model) {

		final Focus focus=graph.query(connection -> { // validate against shape
			return shape
					.map(new Redactor(Form.mode, Form.convey)) // remove internal filtering shapes
					.map(new Optimizer())
					.map(new ReportProbe(connection, set(resource), model));
		});

		final Collection<Statement> envelope=focus.outline().collect(toSet()); // collect shape envelope

		return focus( // extend validation report with errors for statements outside shape envelope

				Stream.concat(

						focus.getIssues().stream(),

						model.stream().filter(statement -> !envelope.contains(statement)).map(outlier ->
								issue(Issue.Level.Error, "statement outside shape envelope "+outlier)
						)

				).collect(toList()),

				focus.getFrames()

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Validate constraints on a focus value set.
	 */
	private static final class ReportProbe implements Shape.Probe<Focus> {

		private final RepositoryConnection connection;

		private final Collection<Value> focus;
		private final Collection<Statement> model;


		private ReportProbe(
				final RepositoryConnection connection,
				final Collection<Value> focus,
				final Collection<Statement> model
		) {

			this.connection=connection;

			this.focus=focus;
			this.model=model;
		}


		@Override public Focus probe(final Meta meta) { return focus(); }

		@Override public Focus probe(final Guard guard) {
			throw new UnsupportedOperationException("partially redacted shape");
		}


		@Override public Focus probe(final Datatype datatype) {
			return focus(set(), focus.stream()
					.filter(value -> !is(value, datatype.getIRI()))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid datatype", datatype))))
					.collect(toList())
			);
		}

		@Override public Focus probe(final Clazz clazz) {
			if ( focus.isEmpty() ) { return focus(); } else {

				// retrieve the class hierarchy rooted in the expected class

				final Set<Value> hierarchy=stream(connection.prepareTupleQuery(source(

						"# clazz hierarchy\n"
								+"\n"
								+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
								+"\n"
								+"select distinct ?class { ?class rdfs:subClassOf* {root} }",

						format(clazz.getIRI())

				)).evaluate())

						.map(bindings -> bindings.getValue("class"))
						.collect(toSet());


				// retrieve type info for focus nodes

				final Map<Value, Set<Value>> types=Stream.concat(

						// retrieve type info from the validated model

						focus.stream().flatMap(value -> model.stream()
								.filter(pattern(value, RDF.TYPE, null))
								.map(Statement::getObject)
								.map(type1 -> entry(value, type1))
						),

						// retrieve type info from graph

						stream(connection.prepareTupleQuery(source(

								"# type info\n"
										+"\n"
										+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
										+"\n"
										+"select ?value ?type {\n"
										+"\n"
										+"\tvalues ?value {\n"
										+"\t\t{values}\n"
										+"\t}\n"
										+"\n"
										+"\t?value a ?type\n"
										+"\n"
										+"}",

								list(focus.stream().map(Values::format), "\n")

						)).evaluate()).map(bindings -> entry(
								bindings.getValue("value"),
								bindings.getValue("type")
						))

				).collect(groupingBy(Map.Entry::getKey, mapping(Map.Entry::getValue, toSet())));


				final Map<IRI, Focus> type=singletonMap(RDF.TYPE, focus(set(), set(frame(clazz.getIRI()))));

				return focus(emptySet(), Stream.concat(

						focus.stream() // generate the validation report
								.filter(value -> disjoint(types.getOrDefault(value, emptySet()), hierarchy))
								.map(value -> frame(value,
										set(issue(Issue.Level.Error, "not an instance of target class", clazz))
								)),

						focus.stream() // add rdf:type frames to support outlining
								.map(value -> frame(value, set(), type))

				).collect(toList()));

			}
		}


		@Override public Focus probe(final MinExclusive minExclusive) {
			return focus(set(), focus.stream()
					.filter(value -> !(compare(value, minExclusive.getValue()) > 0))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid value", minExclusive))))
					.collect(toList())
			);
		}

		@Override public Focus probe(final MaxExclusive maxExclusive) {
			return focus(set(), focus.stream()
					.filter(value -> !(compare(value, maxExclusive.getValue()) < 0))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid value", maxExclusive))))
					.collect(toList())
			);
		}

		@Override public Focus probe(final MinInclusive minInclusive) {
			return focus(set(), focus.stream()
					.filter(value -> !(compare(value, minInclusive.getValue()) >= 0))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid value", minInclusive))))
					.collect(toList())
			);
		}

		@Override public Focus probe(final MaxInclusive maxInclusive) {
			return focus(set(), focus.stream()
					.filter(value -> !(compare(value, maxInclusive.getValue()) <= 0))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid value", maxInclusive))))
					.collect(toList())
			);
		}


		@Override public Focus probe(final Pattern pattern) {

			final String expression=pattern.getText();
			final String flags=pattern.getFlags();

			final java.util.regex.Pattern compiled=java.util.regex.Pattern
					.compile(flags.isEmpty() ? expression : "(?"+flags+":"+expression+")");

			// match the whole string: don't use compiled.asPredicate() (implemented using .find())

			return focus(set(), focus.stream()
					.filter(value -> !compiled.matcher(text(value)).matches())
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid lexical value", pattern))))
					.collect(toList())
			);
		}

		@Override public Focus probe(final Like like) {

			final String expression=like.toExpression();

			final Predicate<String> predicate=java.util.regex.Pattern.compile(expression).asPredicate();

			return focus(set(), focus.stream()
					.filter(value -> !predicate.test(text(value)))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid lexical value", like))))
					.collect(toList())
			);
		}

		@Override public Focus probe(final MaxLength maxLength) {

			final int limit=maxLength.getLimit();

			return focus(set(), focus.stream()
					.filter(value -> !(text(value).length() <= limit))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid lexical value", maxLength))))
					.collect(toList())
			);
		}

		@Override public Focus probe(final MinLength minLength) {

			final int limit=minLength.getLimit();

			return focus(set(), focus.stream()
					.filter(value -> !(text(value).length() >= limit))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid lexical value", minLength))))
					.collect(toList())
			);
		}


		@Override public Focus probe(final MinCount minCount) {
			return focus.size() >= minCount.getLimit() ? focus() : focus(set(
					issue(Issue.Level.Error, "invalid item count", minCount)
			));
		}

		@Override public Focus probe(final MaxCount maxCount) {
			return focus.size() <= maxCount.getLimit() ? focus() : focus(set(
					issue(Issue.Level.Error, "invalid item count", maxCount)
			));
		}


		@Override public Focus probe(final In in) {

			final Set<Value> values=in.getValues();

			return focus(focus.stream()
					.filter(value -> !values.contains(value))
					.map(value -> issue(Issue.Level.Error, "out of range value {"+value+"}", in))
					.collect(toList())
			);
		}

		@Override public Focus probe(final All all) {
			return focus(all.getValues().stream()
					.filter(value -> !focus.contains(value))
					.map(value -> issue(Issue.Level.Error, "missing required value {"+value+"}", all))
					.collect(toList())
			);
		}

		@Override public Focus probe(final Any any) {
			return any.getValues().stream()
					.filter(focus::contains)
					.findAny()
					.map(values -> focus())
					.orElseGet(() -> focus(set(issue(Issue.Level.Error, "missing alternative value", any))));
		}


		@Override public Focus probe(final Field field) {

			final IRI iri=field.getIRI();
			final Shape shape=field.getShape();

			return focus(set(), focus.stream().map(value -> { // for each focus value

				// compute the new focus set

				final Set<Value> focus=(direct(iri)

						? model.stream().filter(pattern(value, iri, null)).map(Statement::getObject)
						: model.stream().filter(pattern(null, inverse(iri), value)).map(Statement::getSubject)

				).collect(toSet());

				// validate the field shape on the new focus set

				final Focus report=shape.map(new ReportProbe(connection, focus, model));

				// identifies the values in the new focus set referenced in report frames

				final Set<Issue> issues=report.getIssues();
				final Set<Frame> frames=report.getFrames();

				final Set<Value> referenced=frames.stream()
						.map(Frame::getValue)
						.collect(toSet());

				// create an empty frame for each unreferenced value to support statement outlining

				final List<Frame> placeholders=focus.stream()
						.filter(v -> !referenced.contains(v))
						.map(Frame::frame)
						.collect(toList());

				// return field validation results

				return frame(value, set(), map(entry(iri, focus(issues, concat(frames, placeholders)))));

			}).collect(toList()));

		}


		@Override public Focus probe(final And and) {
			return and.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(focus(), (focus1, focus12) -> focus1.merge(focus12));
		}

		@Override public Focus probe(final Or or) {
			return or.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(focus(), Focus::merge);
		}

		@Override public Focus probe(final When when) {

			final boolean pass=!when.getTest().map(this).assess(Issue.Level.Error);

			return pass
					? when.getPass().map(this)
					: when.getFail().map(this);

		}

	}

}
