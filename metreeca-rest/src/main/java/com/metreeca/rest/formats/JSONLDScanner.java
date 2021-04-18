/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
import static com.metreeca.json.Values.compare;
import static com.metreeca.json.Values.direct;
import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.is;
import static com.metreeca.json.Values.lang;
import static com.metreeca.json.Values.text;
import static com.metreeca.json.Values.traverse;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.formats.JSONLDInspector.driver;

import static java.lang.String.format;
import static java.lang.String.join;
import static java.util.Collections.singleton;
import static java.util.stream.Collectors.*;

/**
 * Shape-driven RDF validator.
 */
final class JSONLDScanner extends Shape.Probe<Either<Trace, Stream<Statement>>> {

	static Either<Trace, Collection<Statement>> scan(
			final Shape shape, final Value focus, final Collection<Statement> model
	) {
		return driver(shape)
				.map(new JSONLDScanner(focus, singleton(focus), model))
				.fold(Either::Left, stream -> Right(stream.collect(toList())));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Value focus;

	private final Collection<Value> group;
	private final Collection<Statement> model;


	private JSONLDScanner(final Value focus, final Collection<Value> group, final Collection<Statement> model) {

		this.focus=focus;

		this.group=group;
		this.model=model;
	}


	private <T> Predicate<T> negate(final Predicate<T> predicate) {
		return predicate.negate();
	}

	private Either<Trace, Stream<Statement>> report(final Trace trace) {
		return trace.empty() ? Right(Stream.empty()) : Left(trace);
	}

	private Either<Trace, Stream<Statement>> merge(
			final Either<Trace, Stream<Statement>> x, final Either<Trace, Stream<Statement>> y
	) {
		return x.fold(
				xtrace -> y.fold(ytrace -> Left(trace(xtrace, ytrace)), ystream -> Left(xtrace)),
				xstream -> y.fold(Either::Left, ystream -> Right(Stream.concat(xstream, ystream)))
		);
	}


	private Value resolve(final Value value) {
		return value instanceof Focus ? ((Focus)value).resolve(focus) : value;
	}

	private Set<Value> resolve(final Collection<Value> values) {
		return values.stream().map(this::resolve).collect(toSet());
	}


	@Override public Either<Trace, Stream<Statement>> probe(final Guard guard) {
		throw new UnsupportedOperationException(guard.toString());
	}


	@Override public Either<Trace, Stream<Statement>> probe(final Datatype datatype) {

		final IRI iri=datatype.iri();

		return report(trace(group.stream()
				.filter(negate(value -> is(value, iri)))
				.map(value -> format("%s is not of datatype %s", format(value), format(iri)))
		));
	}


	@Override public Either<Trace, Stream<Statement>> probe(final Range range) {

		final Set<Value> values=resolve(range.values());

		return report(trace(group.stream()
				.filter(value -> !values.contains(value))
				.map(value -> format("%s is not in the expected value range {%s}", format(value), format(values)))
		));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final Lang lang) {

		final Set<String> tags=lang.tags();

		return report(trace(group.stream()

				.filter(negate(tags.isEmpty()
						? value -> !lang(value).isEmpty()
						: value -> tags.contains(lang(value))
				))

				.map(value -> format(
						"%s is not in the expected language set {%s}", format(value), join(", ", tags)
				))
		));
	}


	@Override public Either<Trace, Stream<Statement>> probe(final MinExclusive minExclusive) {

		final Value limit=resolve(minExclusive.limit());

		return report(trace(group.stream()
				.filter(negate(value -> compare(value, limit) > 0))
				.map(value -> format("%s is not strictly greater than %s", format(value), format(limit)))
		));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final MaxExclusive maxExclusive) {

		final Value limit=resolve(maxExclusive.limit());

		return report(trace(group.stream()
				.filter(negate(value -> compare(value, limit) < 0))
				.map(value -> format("%s is not strictly less than %s", format(value), format(limit)))
		));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final MinInclusive minInclusive) {

		final Value limit=resolve(minInclusive.limit());

		return report(trace(group.stream()
				.filter(negate(value -> compare(value, limit) >= 0))
				.map(value -> format("%s is not greater than or equal to %s", format(value), format(limit)))
		));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final MaxInclusive maxInclusive) {

		final Value limit=resolve(maxInclusive.limit());

		return report(trace(group.stream()
				.filter(negate(value -> compare(value, limit) <= 0))
				.map(value -> format("%s is not less than or equal to %s", format(value), format(limit)))
		));
	}


	@Override public Either<Trace, Stream<Statement>> probe(final MinLength minLength) {

		final int limit=minLength.limit();

		return report(trace(group.stream()
				.filter(negate(value -> text(value).length() >= limit))
				.map(value -> format("%s length is not greater than or equal to %s", format(value), limit))
		));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final MaxLength maxLength) {

		final int limit=maxLength.limit();

		return report(trace(group.stream()
				.filter(negate(value -> text(value).length() <= limit))
				.map(value -> format("%s length is not less than or equal to %s", format(value), limit))
		));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final Pattern pattern) {

		final String expression=pattern.expression();
		final String flags=pattern.flags();

		final java.util.regex.Pattern compiled=java.util.regex.Pattern
				.compile(flags.isEmpty() ? expression : "(?"+flags+":"+expression+")");

		// match the whole string: don't use compiled.asPredicate() (implemented using .find())

		return report(trace(group.stream()
				.filter(negate(value -> compiled.matcher(text(value)).matches()))
				.map(value -> format("%s textual value doesn't match <%s> pattern", format(value), compiled.pattern()))
		));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final Like like) {

		final String expression=like.toExpression();

		final Predicate<String> predicate=java.util.regex.Pattern.compile(expression).asPredicate();

		return report(trace(group.stream()
				.filter(negate(value -> predicate.test(text(value))))
				.map(value -> format("%s textual value doesn't match <%s> keywords", format(value), like.keywords()))
		));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final Stem stem) {

		final String prefix=stem.prefix();

		final Predicate<String> predicate=lexical -> lexical.startsWith(prefix);

		return report(trace(group.stream()
				.filter(negate(value -> predicate.test(text(value))))
				.map(value -> format("%s textual value has not stem <%s>", format(value), prefix))
		));
	}


	@Override public Either<Trace, Stream<Statement>> probe(final MinCount minCount) {

		final int count=group.size();
		final int limit=minCount.limit();

		return count >= limit ? Right(Stream.empty()) : report(trace(format(
				"value count is not greater than or equal to %s", limit
		)));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final MaxCount maxCount) {

		final int count=group.size();
		final int limit=maxCount.limit();

		return count <= limit ? Right(Stream.empty()) : report(trace(format(
				"value count is not less than or equal to %s", limit
		)));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final All all) {

		final Set<Value> values=resolve(all.values());

		return group.containsAll(values) ? Right(Stream.empty()) : report(trace(format(
				"values don't include all the expected set {%s}", format(values)
		)));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final Any any) {

		final Set<Value> values=resolve(any.values());

		return values.stream().anyMatch(group::contains) ? Right(Stream.empty()) : report(trace(format(
				"values don't include at least one of the expected set {%s}", format(values)
		)));
	}

	@Override public Either<Trace, Stream<Statement>> probe(final Localized localized) {
		return report(trace(group.stream()

				.collect(groupingBy(Values::lang, toList()))

				.entrySet().stream()

				.filter(negate(entry -> entry.getValue().size() <= 1))

				.map(entry -> format("multiple values for <%s> language tag", entry.getKey()))
		));
	}


	@Override public Either<Trace, Stream<Statement>> probe(final Link link) {
		return link.shape().map(this);
	}

	@Override public Either<Trace, Stream<Statement>> probe(final Field field) {
		return group.stream().map(value -> {

			final IRI iri=field.iri();
			final Shape shape=field.shape();

			final List<Statement> statements=model.stream()
					.filter(traverse(iri,
							recto -> s -> s.getPredicate().equals(recto) && s.getSubject().equals(value),
							verso -> s -> s.getPredicate().equals(verso) && s.getObject().equals(value)
					))
					.collect(toList());

			final Set<Value> values=statements.stream()
					.map(direct(iri) ? Statement::getObject : Statement::getSubject)
					.collect(toSet());

			return merge(

					shape.map(new JSONLDScanner(focus, values, model)).fold(
							trace -> Left(trace(iri.toString(), trace)),
							Either::Right
					),

					Right(statements.stream())
			);

		}).reduce(Right(Stream.empty()), this::merge);
	}


	@Override public Either<Trace, Stream<Statement>> probe(final When when) {
		return when.test().map(this).fold(trace -> when.fail(), stream -> when.pass()).map(this);
	}

	@Override public Either<Trace, Stream<Statement>> probe(final And and) {
		return and.shapes().stream()

				.map(s -> s.map(this))

				.reduce(Right(Stream.empty()), this::merge);
	}

	@Override public Either<Trace, Stream<Statement>> probe(final Or or) {

		final List<Either<Trace, Stream<Statement>>> reports=or.shapes().stream()

				.map(s -> s.map(this))


				.filter(entry -> entry.fold(trace -> false, stream -> true))

				.collect(toList());

		return reports.isEmpty()
				? Left(trace("values don't match any alternative"))
				: reports.stream().reduce(Right(Stream.empty()), this::merge);
	}


	@Override public Either<Trace, Stream<Statement>> probe(final Shape shape) {
		return Right(Stream.empty());
	}

}
