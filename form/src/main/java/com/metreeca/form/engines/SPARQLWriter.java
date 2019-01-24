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
import com.metreeca.form.shifts.Count;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.shifts.Table;
import com.metreeca.form.things.Lists;
import com.metreeca.form.things.Sets;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.query.AbstractTupleQueryResultHandler;
import org.eclipse.rdf4j.query.BindingSet;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.*;
import java.util.function.Predicate;
import java.util.logging.Level;
import java.util.logging.Logger;

import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.Report.report;
import static com.metreeca.form.Shape.mode;
import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.form.things.Values.compare;
import static com.metreeca.form.things.Values.is;
import static com.metreeca.form.things.Values.text;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
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

	public Report process(final Shape shape, final Iterable<Statement> model, final Value... focus) {

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
				.map(new TracesProbe(new LinkedHashSet<>(asList(focus))));
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
	 * Validate constraints on a focus value set.
	 */
	private final class TracesProbe implements Shape.Probe<Report> {

		private final Collection<Value> focus;


		private TracesProbe(final Collection<Value> focus) {
			this.focus=focus;
		}


		@Override public Report probe(final Meta meta) { return report(); }

		@Override public Report probe(final When when) { return report(); }


		@Override public Report probe(final Datatype datatype) {
			return report(focus.stream()
					.filter(value -> !is(value, datatype.getIRI()))
					.map(value -> Issue.issue(Issue.Level.Error, "invalid datatype", datatype, value))
					.collect(toList()));
		}

		@Override public Report probe(final Clazz clazz) {
			if ( focus.isEmpty() ) { return report(); } else {

				final Collection<Issue> issues=new ArrayList<>();

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

						issues.add(issue(Issue.Level.Error,
								"not an instance of target class", clazz, bindings.getValue("value")));

					}
				});

				return report(issues, focus.stream()
						.map(value -> frame(value, Frame.slot(Step.step(RDF.TYPE), report(emptySet(), frame(clazz.getIRI())))))
						.collect(toList()));
			}
		}

		@Override public Report probe(final MinExclusive minExclusive) {
			return report(focus.stream()
					.filter(value -> !(compare(value, minExclusive.getValue()) > 0))
					.map(value -> Issue.issue(Issue.Level.Error, "invalid value", minExclusive, value))
					.collect(toList()));
		}

		@Override public Report probe(final MaxExclusive maxExclusive) {
			return report(focus.stream()
					.filter(value -> !(compare(value, maxExclusive.getValue()) < 0))
					.map(value -> Issue.issue(Issue.Level.Error, "invalid value", maxExclusive, value))
					.collect(toList()));
		}

		@Override public Report probe(final MinInclusive minInclusive) {
			return report(focus.stream()
					.filter(value -> !(compare(value, minInclusive.getValue()) >= 0))
					.map(value -> Issue.issue(Issue.Level.Error, "invalid value", minInclusive, value))
					.collect(toList()));
		}

		@Override public Report probe(final MaxInclusive maxInclusive) {
			return report(focus.stream()
					.filter(value -> !(compare(value, maxInclusive.getValue()) <= 0))
					.map(value -> Issue.issue(Issue.Level.Error, "invalid value", maxInclusive, value))
					.collect(toList()));
		}

		@Override public Report probe(final Pattern pattern) {

			final String expression=pattern.getText();
			final String flags=pattern.getFlags();

			final java.util.regex.Pattern compiled=java.util.regex.Pattern
					.compile(flags.isEmpty() ? expression : "(?"+flags+":"+expression+")");

			// match the whole string: don't use compiled.asPredicate() (implemented using .find())

			return report(focus.stream()
					.filter(value -> !compiled.matcher(text(value)).matches())
					.map(value -> Issue.issue(Issue.Level.Error, "invalid lexical value", pattern, value))
					.collect(toList()));
		}

		@Override public Report probe(final Like like) {

			final String expression=like.toExpression();

			final Predicate<String> predicate=java.util.regex.Pattern
					.compile(expression)
					.asPredicate();

			return report(focus.stream()
					.filter(value -> !predicate.test(text(value)))
					.map(value -> Issue.issue(Issue.Level.Error, "invalid lexical value", like, value))
					.collect(toList()));
		}

		@Override public Report probe(final MaxLength maxLength) {

			final int limit=maxLength.getLimit();

			return report(focus.stream()
					.filter(value -> !(text(value).length() <= limit))
					.map(value -> Issue.issue(Issue.Level.Error, "invalid lexical value", maxLength, value))
					.collect(toList()));
		}

		@Override public Report probe(final MinLength minLength) {

			final int limit=minLength.getLimit();

			return report(focus.stream()
					.filter(value -> !(text(value).length() >= limit))
					.map(value -> Issue.issue(Issue.Level.Error, "invalid lexical value", minLength, value))
					.collect(toList()));
		}


		@Override public Report probe(final MinCount minCount) {
			return focus.size() >= minCount.getLimit() ? report() : report(issue(
					Issue.Level.Error, "invalid item count", minCount, focus
			));
		}

		@Override public Report probe(final MaxCount maxCount) {
			return focus.size() <= maxCount.getLimit() ? report() : report(issue(
					Issue.Level.Error, "invalid item count", maxCount, focus
			));
		}

		@Override public Report probe(final In in) {

			final Set<Value> values=in.getValues();

			return report(focus.stream()
					.filter(value -> !values.contains(value))
					.map(value -> issue(Issue.Level.Error, "out of range value", in, value))
					.collect(toList()));
		}

		@Override public Report probe(final All all) {
			return report(all.getValues().stream()
					.filter(value -> !focus.contains(value))
					.map(value -> issue(Issue.Level.Error, "missing required value", all, value))
					.collect(toList()));
		}

		@Override public Report probe(final Any any) {
			return any.getValues().stream()
					.filter(focus::contains)
					.findAny()
					.map(values -> report())
					.orElseGet(() -> report(issue(Issue.Level.Error, "missing alternative value", any, focus)));
		}


		@Override public Report probe(final Trait trait) {

			final Step step=trait.getStep();
			final Shape shape=trait.getShape();

			return report(Sets.set(), focus.stream().map(value -> { // for each focus value

				// compute the new focus set expanding the trait shift from the focus value

				final Set<Value> focus=step.map(new FocusProbe(value));

				// validate the trait shape on the new focus set

				final Report report=shape.map(new TracesProbe(focus));

				// identifies the values in the new focus set referenced in report frames

				final Set<Issue> issues=report.getIssues();
				final Set<Frame<Report>> frames=report.getFrames();

				final Set<Value> referenced=frames.stream()
						.map(Frame::getValue)
						.collect(toSet());

				// create an empty frame for each unreferenced value to support statement outlining

				final List<Frame<Report>> placeholders=Sets.complement(focus, referenced).stream()
						.map(v -> Frame.<Report>frame(v))
						.collect(toList());

				// return trait validation results

				return frame(value, Frame.slot(step, report(issues, Lists.concat(frames, placeholders))));

			}).collect(toList()));

		}


		@Override public Report probe(final And and) {
			return and.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(report(), Report::merge);
		}

		@Override public Report probe(final Or or) {
			return or.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(report(), Report::merge);
		}

		@Override public Report probe(final Option option) {

			final boolean pass=!option.getTest().map(this).assess(Issue.Level.Error);

			return pass
					? option.getPass().map(this)
					: option.getFail().map(this);

		}

	}


	/**
	 * Expands a source value into a focus value set applying a shift operator.
	 */
	private final class FocusProbe implements Shift.Probe<Set<Value>> {

		private final Value source;


		private FocusProbe(final Value source) {
			this.source=source;
		}



		@Override public Set<Value> probe(final Step step) {

			final Set<Value> values=new HashSet<>();

			final IRI iri=step.getIRI();

			if ( step.isInverse() ) {

				try (final RepositoryResult<Statement> statements=connection.getStatements(null, iri, source)) {
					while ( statements.hasNext() ) { values.add(statements.next().getSubject()); }
				}

			} else if ( source instanceof Resource ) {

				try (final RepositoryResult<Statement> statements=connection.getStatements((Resource)source, iri, null)) {
					while ( statements.hasNext() ) { values.add(statements.next().getObject()); }
				}

			}

			return values;
		}

		@Override public Set<Value> probe(final Table table) { return probe((Shift)table); }

		@Override public Set<Value> probe(final Count count) { return probe((Shift)count); }


		private Set<Value> probe(final Shift shift) {
			throw new UnsupportedOperationException("unsupported shift ["+shift.getClass().getName()+"]");
		}

	}

}
