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

package com.metreeca.form.engines;

import com.metreeca.form.*;
import com.metreeca.form.shapes.*;
import com.metreeca.form.things.Lists;
import com.metreeca.form.things.Sets;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.form.things.Values.*;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


final class SPARQLWriter {

	private static final Logger logger=Logger.getLogger(SPARQLWriter.class.getName()); // !!! migrate logging to Trace?


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final RepositoryConnection connection;


	public SPARQLWriter(final RepositoryConnection connection) {

		if ( connection == null ) {
			throw new NullPointerException("null connection");
		}

		this.connection=connection;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Focus process(final Shape shape, final Iterable<Statement> model, final Value... focus) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		connection.add(model);

		return shape
				.map(mode(Form.verify))
				.map(new FocusProbe(new LinkedHashSet<>(asList(focus))));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String compile(final SPARQL sparql) {

		final String query=sparql.compile();

		if ( logger.isLoggable(Level.FINE) ) {
			logger.fine("evaluating SPARQL query "+indent(query, true)+(query.endsWith("\n") ? "" : "\n"));
		}

		return query;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Expands a source value into a focus value set following a path step.
	 */
	private Set<Value> shift(final Value source, final IRI iri) {

		final Set<Value> values=new HashSet<>();

		if ( !direct(iri) ) {

			try (final RepositoryResult<Statement> statements=connection.getStatements(null, inverse(iri), source)) {
				while ( statements.hasNext() ) { values.add(statements.next().getSubject()); }
			}

		} else if ( source instanceof Resource ) {

			try (final RepositoryResult<Statement> statements=connection.getStatements((Resource)source, iri, null)) {
				while ( statements.hasNext() ) { values.add(statements.next().getObject()); }
			}

		}

		return values;
	}


	/**
	 * Validate constraints on a focus value set.
	 */
	private final class FocusProbe implements Shape.Probe<Focus> {

		private final Collection<Value> focus;


		private FocusProbe(final Collection<Value> focus) {
			this.focus=focus;
		}


		@Override public Focus probe(final Meta meta) { return focus(); }

		@Override public Focus probe(final Guard guard) { return focus(); }


		@Override public Focus probe(final Datatype datatype) {
			return focus(set(), focus.stream()
					.filter(value -> !is(value, datatype.getIRI()))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid datatype", datatype))))
					.collect(toList()));
		}

		@Override public Focus probe(final Clazz clazz) {
			if ( focus.isEmpty() ) { return focus(); } else {

				final Collection<Frame> frames=new ArrayList<>();

				connection.prepareTupleQuery(compile(new SPARQL() {

					@Override public Object code() {
						return Lists.list(

								"# clazz constraint\f",

								prefixes(),

								"select ?value where {\f",

								"values ?value {\n",
								items(focus.stream().map(this::term), "\n"),
								"\n}\f",

								"filter not exists {\n?value a/rdfs:subClassOf* ", term(clazz.getIRI()), "\n}",

								"\f}"

						);
					}

				})).evaluate(new AbstractTupleQueryResultHandler() {
					@Override public void handleSolution(final BindingSet bindings) {

						frames.add(frame(
								bindings.getValue("value"),
								set(issue(Issue.Level.Error, "not an instance of target class", clazz))
						));

					}
				});

				final Map<IRI, Focus> type=singletonMap(RDF.TYPE, focus(set(), set(frame(clazz.getIRI()))));

				focus.stream() // add rdf:type outlining frames
						.map(value -> frame(value, set(), type))
						.forEach(frames::add);

				return focus(set(), frames);
			}
		}

		@Override public Focus probe(final MinExclusive minExclusive) {
			return focus(set(), focus.stream()
					.filter(value -> !(compare(value, minExclusive.getValue()) > 0))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid value", minExclusive))))
					.collect(toList()));
		}

		@Override public Focus probe(final MaxExclusive maxExclusive) {
			return focus(set(), focus.stream()
					.filter(value -> !(compare(value, maxExclusive.getValue()) < 0))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid value", maxExclusive))))
					.collect(toList()));
		}

		@Override public Focus probe(final MinInclusive minInclusive) {
			return focus(set(), focus.stream()
					.filter(value -> !(compare(value, minInclusive.getValue()) >= 0))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid value", minInclusive))))
					.collect(toList()));
		}

		@Override public Focus probe(final MaxInclusive maxInclusive) {
			return focus(set(), focus.stream()
					.filter(value -> !(compare(value, maxInclusive.getValue()) <= 0))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid value", maxInclusive))))
					.collect(toList()));
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
					.collect(toList()));
		}

		@Override public Focus probe(final Like like) {

			final String expression=like.toExpression();

			return focus(set(), focus.stream()
					.filter(value -> !java.util.regex.Pattern
							.compile(expression)
							.asPredicate().test(text(value)))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid lexical value", like))))
					.collect(toList()));
		}

		@Override public Focus probe(final MaxLength maxLength) {

			final int limit=maxLength.getLimit();

			return focus(set(), focus.stream()
					.filter(value -> !(text(value).length() <= limit))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid lexical value", maxLength))))
					.collect(toList()));
		}

		@Override public Focus probe(final MinLength minLength) {

			final int limit=minLength.getLimit();

			return focus(set(), focus.stream()
					.filter(value -> !(text(value).length() >= limit))
					.map(value -> frame(value, set(issue(Issue.Level.Error, "invalid lexical value", minLength))))
					.collect(toList()));
		}


		@Override public Focus probe(final MinCount minCount) {
			return focus.size() >= minCount.getLimit() ? focus() : focus(set(new Issue[] {
					issue(
							Issue.Level.Error, "invalid item count", minCount
					)
			}));
		}

		@Override public Focus probe(final MaxCount maxCount) {
			return focus.size() <= maxCount.getLimit() ? focus() : focus(set(new Issue[] {
					issue(
							Issue.Level.Error, "invalid item count", maxCount
					)
			}));
		}

		@Override public Focus probe(final In in) {

			final Set<Value> values=in.getValues();

			return focus(focus.stream()
					.filter(value -> !values.contains(value))
					.map(value -> issue(Issue.Level.Error, "out of range value {"+value+"}", in))
					.collect(toList()));
		}

		@Override public Focus probe(final All all) {
			return focus(all.getValues().stream()
					.filter(value -> !focus.contains(value))
					.map(value -> issue(Issue.Level.Error, "missing required value {"+value+"}", all))
					.collect(toList()));
		}

		@Override public Focus probe(final Any any) {
			return any.getValues().stream()
					.filter(focus::contains)
					.findAny()
					.map(values -> focus())
					.orElseGet(() -> focus(set(new Issue[] {issue(Issue.Level.Error, "missing alternative value", any)})));
		}


		@Override public Focus probe(final Field field) {

			final IRI iri=field.getIRI();
			final Shape shape=field.getShape();

			return focus(set(), focus.stream().map(value -> { // for each focus value

				// compute the new focus set expanding the field shift from the focus value

				final Set<Value> focus=shift(value, iri);

				// validate the field shape on the new focus set

				final Focus report=shape.map(new FocusProbe(focus));

				// identifies the values in the new focus set referenced in report frames

				final Set<Issue> issues=report.getIssues();
				final Set<Frame> frames=report.getFrames();

				final Set<Value> referenced=frames.stream()
						.map(Frame::getValue)
						.collect(toSet());

				// create an empty frame for each unreferenced value to support statement outlining

				final List<Frame> placeholders=Sets.complement(focus, referenced).stream()
						.map(Frame::frame)
						.collect(toList());

				// return field validation results

				return frame(value, set(), map(entry(iri, focus(issues, concat(frames, placeholders)))));

			}).collect(toList()));

		}


		@Override public Focus probe(final And and) {
			return and.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(focus(), Focus::merge);
		}

		@Override public Focus probe(final Or or) {
			return or.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(focus(), Focus::merge);
		}

		@Override public Focus probe(final Option option) {

			final boolean pass=!option.getTest().map(this).assess(Issue.Level.Error);

			return pass
					? option.getPass().map(this)
					: option.getFail().map(this);

		}

	}

}
