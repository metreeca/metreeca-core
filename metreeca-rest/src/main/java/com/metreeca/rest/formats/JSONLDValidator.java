/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.formats;

import com.metreeca.json.*;
import com.metreeca.json.shapes.*;
import com.metreeca.rest.Either;

import org.eclipse.rdf4j.model.*;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.json.Trace.trace;
import static com.metreeca.json.Values.*;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.formats.JSONLDCodec.driver;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;


final class JSONLDValidator {

	private final IRI focus;
	private final Shape shape;
	private final Map<String, String> keywords;

	JSONLDValidator(final IRI focus, final Shape shape, final Map<String, String> keywords) {

		this.focus=focus;
		this.shape=driver(shape);

		this.keywords=keywords;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	Either<Trace, Collection<Statement>> validate(final Collection<Statement> model) {

		final Collection<Statement> envelope=new HashSet<>();

		final Trace trace=shape.map(new ValidatorProbe(
				focus, singleton(focus), model, envelope
		));

		final Map<String, Collection<Value>> issues=model.stream()

				.filter(statement -> !envelope.contains(statement))

				.collect(toMap(

						statement -> statement.getSubject().equals(focus) ?
								"unexpected property {"+format(statement.getPredicate())+"}"
								: statement.getObject().equals(focus) ?
								"unexpected property {^"+format(statement.getPredicate())+"}"
								: "statement outside shape envelope",

						statement -> singleton(statement.getObject()),

						(x, y) -> Stream.of(x, y).flatMap(Collection::stream).collect(toSet())

				));


		final Trace merged=trace(trace(issues), trace);

		return merged.empty() ? Right(model) : Left(merged);

	}


	//////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ValidatorProbe extends Shape.Probe<Trace> {

		private final IRI resource;
		private final Collection<Value> focus;

		private final Collection<Statement> source;
		private final Collection<Statement> target;


		private ValidatorProbe(
				final IRI resource, final Collection<Value> focus,
				final Collection<Statement> source, final Collection<Statement> target
		) {

			this.resource=resource;
			this.focus=focus;

			this.source=source;
			this.target=target;

		}


		private Value value(final Value value) {
			return value instanceof Focus
					? ((Focus)value).resolve(resource)
					: value;
		}

		private Set<Value> values(final Collection<Value> values) {
			return values.stream()
					.map(this::value)
					.collect(toSet());
		}


		private <T> Predicate<T> negate(final Predicate<T> predicate) {
			return predicate.negate();
		}

		private String issue(final Shape shape) {
			return shape.toString().replaceAll("\\s+", " ");
		}


		@Override public Trace probe(final Guard guard) {
			throw new UnsupportedOperationException(guard.toString());
		}


		@Override public Trace probe(final Datatype datatype) {

			final IRI iri=datatype.id();

			return trace(focus.stream()
					.filter(negate(value -> is(value, iri)))
					.collect(toMap(v -> issue(datatype), Collections::singleton))
			);
		}

		@Override public Trace probe(final Range range) {

			final Set<Value> values=values(range.values());

			final Collection<Value> unexpected=focus
					.stream()
					.filter(negate(values::contains))
					.collect(toList());

			return unexpected.isEmpty() ? trace() : trace(issue(range), unexpected);
		}


		@Override public Trace probe(final MinExclusive minExclusive) {

			final Value limit=value(minExclusive.limit());

			return trace(focus.stream()
					.filter(negate(value -> compare(value, limit) > 0))
					.collect(toMap(v -> issue(minExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxExclusive maxExclusive) {

			final Value limit=value(maxExclusive.limit());

			return trace(focus.stream()
					.filter(negate(value -> compare(value, limit) < 0))
					.collect(toMap(v -> issue(maxExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MinInclusive minInclusive) {

			final Value limit=value(minInclusive.limit());

			return trace(focus.stream()
					.filter(negate(value -> compare(value, limit) >= 0))
					.collect(toMap(v -> issue(minInclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxInclusive maxInclusive) {

			final Value limit=value(maxInclusive.value());

			return trace(focus.stream()
					.filter(negate(value -> compare(value, limit) <= 0))
					.collect(toMap(v -> issue(maxInclusive), Collections::singleton))
			);
		}


		@Override public Trace probe(final MinLength minLength) {

			final int limit=minLength.limit();

			return trace(focus.stream()
					.filter(negate(value -> text(value).length() >= limit))
					.collect(toMap(value -> issue(minLength), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxLength maxLength) {

			final int limit=maxLength.limit();

			return trace(focus.stream()
					.filter(negate(value -> text(value).length() <= limit))
					.collect(toMap(value -> issue(maxLength), Collections::singleton))
			);
		}

		@Override public Trace probe(final Pattern pattern) {

			final String expression=pattern.text();
			final String flags=pattern.flags();

			final java.util.regex.Pattern compiled=java.util.regex.Pattern
					.compile(flags.isEmpty() ? expression : "(?"+flags+":"+expression+")");

			// match the whole string: don't use compiled.asPredicate() (implemented using .find())

			return trace(focus.stream()
					.filter(negate(value -> compiled.matcher(text(value)).matches()))
					.collect(toMap(value -> issue(pattern), Collections::singleton))
			);
		}

		@Override public Trace probe(final Like like) {

			final String expression=like.toExpression();

			final Predicate<String> predicate=java.util.regex.Pattern.compile(expression).asPredicate();

			return trace(focus.stream()
					.filter(negate(value -> predicate.test(text(value))))
					.collect(toMap(v -> issue(like), Collections::singleton))
			);
		}

		@Override public Trace probe(final Stem stem) {

			final String prefix=stem.prefix();

			final Predicate<String> predicate=lexical -> lexical.startsWith(prefix);

			return trace(focus.stream()
					.filter(negate(value -> predicate.test(text(value))))
					.collect(toMap(v -> issue(stem), Collections::singleton))
			);
		}


		@Override public Trace probe(final MinCount minCount) {

			final int count=focus.size();
			final int limit=minCount.limit();

			return count >= limit ? trace() : trace(issue(minCount), literal(count));
		}

		@Override public Trace probe(final MaxCount maxCount) {

			final int count=focus.size();
			final int limit=maxCount.limit();

			return count <= limit ? trace() : trace(issue(maxCount), literal(count));
		}

		@Override public Trace probe(final All all) {

			final Set<Value> range=values(all.values());

			final List<Value> missing=range
					.stream()
					.filter(negate(focus::contains))
					.collect(toList());

			return missing.isEmpty() ? trace() : trace(issue(all), missing);
		}

		@Override public Trace probe(final Any any) {
			return !disjoint(focus, values(any.values()))
					? trace() : trace(issue(any));
		}


		@Override public Trace probe(final Field field) {

			final IRI iri=field.label();
			final Shape shape=field.value();

			return focus.stream().map(value -> { // for each focus value

				// compute the new focus set

				final boolean direct=direct(iri);

				final Set<Statement> edges=(direct

						? source.stream().filter(pattern(value, iri, null))
						: source.stream().filter(pattern(null, inverse(iri), value))

				).collect(toSet());

				final Set<Value> focus=(direct

						? edges.stream().map(Statement::getObject)
						: edges.stream().map(Statement::getSubject)

				).collect(toSet());

				// trace visited statements

				target.addAll(edges);

				// validate the field shape on the new focus set

				return trace(emptyMap(), singletonMap(iri,
						shape.map(new ValidatorProbe(resource, focus, source, target))
				));

			}).reduce(trace(), Trace::trace);
		}


		@Override public Trace probe(final And and) {
			return and.shapes().stream()
					.map(s -> s.map(this))
					.reduce(trace(), Trace::trace);
		}

		@Override public Trace probe(final Or or) {
			return or.shapes().stream().anyMatch(s -> s.map(this).empty()) ? trace() : trace(issue(or));
		}

		@Override public Trace probe(final When when) {
			return (when.test().map(this).empty() ? when.pass() : when.fail()).map(this);
		}


		@Override public Trace probe(final Shape shape) {
			return trace();
		}

	}

}

