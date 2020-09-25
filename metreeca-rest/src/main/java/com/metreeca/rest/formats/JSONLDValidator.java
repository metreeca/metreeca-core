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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import javax.json.*;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.json.Trace.trace;
import static com.metreeca.json.Values.*;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.formats.JSONLDCodec.driver;
import static com.metreeca.rest.formats.JSONLDCodec.fields;
import static java.lang.String.format;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;


final class JSONLDValidator {

	private static <T> Predicate<T> negate(final Predicate<T> predicate) {
		return predicate.negate();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI focus;
	private final Shape shape;
	private final Map<String, String> keywords;

	JSONLDValidator(final IRI focus, final Shape shape, final Map<String, String> keywords) {

		this.focus=focus;
		this.shape=driver(shape);

		this.keywords=keywords;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	Either<Trace, JsonObject> validate(final JsonObject object) {

		final Trace trace=validate(shape, singleton(object));

		return trace.empty() ? Right(object) : Left(trace);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Trace validate(final Shape shape, final Collection<JsonValue> values) {

		final Map<String, Field> fields=fields(shape);

		return Stream.concat(

				Stream.of(shape.map(new ValidatorProbe(focus, shape, fields, values))),

				values.stream() // validate shape envelope

						.filter(JsonObject.class::isInstance)
						.map(JsonValue::asJsonObject)

						.map(Map::keySet)
						.flatMap(Collection::stream)

						.filter(negate(alias
								-> alias.startsWith("@")
								|| keywords.containsValue(alias)
								|| fields.containsKey(alias)
						))

						.map(alias -> trace(emptyMap(), singletonMap(alias, trace("unexpected field"))))

		).reduce(trace(), Trace::trace);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class ValidatorProbe extends Shape.Probe<Trace> {

		private final IRI focus;
		private final Shape shape;

		private Map<String, Field> fields;
		private final Collection<JsonValue> values;

		private final JSONLDDecoder decoder;


		private ValidatorProbe(final IRI focus, final Shape shape,
				final Map<String, Field> fields, final Collection<JsonValue> values
		) {

			this.focus=focus;
			this.shape=shape;

			this.fields=fields;
			this.values=values;

			this.decoder=new JSONLDDecoder(focus, shape, keywords);
		}


		private Value value(final JsonValue value) {
			return decoder.value(value, shape).getKey();
		}


		private Value resolve(final Value value) {
			return value instanceof Focus ? ((Focus)value).resolve(focus) : value;
		}

		private Set<Value> resolve(final Collection<Value> values) {
			return values.stream().map(this::resolve).collect(toSet());
		}


		private String issue(final Shape shape) { // !!! remove
			return shape.toString().replaceAll("\\s+", " ");
		}


		@Override public Trace probe(final Guard guard) {
			throw new UnsupportedOperationException(guard.toString());
		}

		@Override public Trace probe(final Datatype datatype) {

			final IRI iri=datatype.iri();

			return trace(values.stream()
					.filter(negate(value -> is(value(value), iri)))
					.collect(toMap(value -> issue(datatype), Collections::singleton))
			);
		}

		@Override public Trace probe(final Range range) {

			final Set<Value> set=resolve(range.values());

			final Collection<JsonValue> unexpected=values.stream()
					.filter(value -> !set.contains(value(value)))
					.collect(toList());

			return unexpected.isEmpty() ? trace() : trace(issue(range), unexpected);
		}


		@Override public Trace probe(final MinExclusive minExclusive) {

			final Value limit=resolve(minExclusive.limit());

			return trace(values.stream()
					.filter(negate(value -> compare(value(value), limit) > 0))
					.collect(toMap(value -> issue(minExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxExclusive maxExclusive) {

			final Value limit=resolve(maxExclusive.limit());

			return trace(values.stream()
					.filter(negate(value -> compare(value(value), limit) < 0))
					.collect(toMap(v -> issue(maxExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MinInclusive minInclusive) {

			final Value limit=resolve(minInclusive.limit());

			return trace(values.stream()
					.filter(negate(value -> compare(value(value), limit) >= 0))
					.collect(toMap(v -> issue(minInclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxInclusive maxInclusive) {

			final Value limit=resolve(maxInclusive.value());

			return trace(values.stream()
					.filter(negate(value -> compare(value(value), limit) <= 0))
					.collect(toMap(v -> issue(maxInclusive), Collections::singleton))
			);
		}


		@Override public Trace probe(final MinLength minLength) {

			final int limit=minLength.limit();

			return trace(values.stream()
					.filter(negate(value -> text(value(value)).length() >= limit))
					.collect(toMap(value -> issue(minLength), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxLength maxLength) {

			final int limit=maxLength.limit();

			return trace(values.stream()
					.filter(negate(value -> text(value(value)).length() <= limit))
					.collect(toMap(value -> issue(maxLength), Collections::singleton))
			);
		}

		@Override public Trace probe(final Pattern pattern) {

			final String expression=pattern.text();
			final String flags=pattern.flags();

			final java.util.regex.Pattern compiled=java.util.regex.Pattern
					.compile(flags.isEmpty() ? expression : "(?"+flags+":"+expression+")");

			// match the whole string: don't use compiled.asPredicate() (implemented using .find())

			return trace(values.stream()
					.filter(negate(value -> compiled.matcher(text(value(value))).matches()))
					.collect(toMap(value -> issue(pattern), Collections::singleton))
			);
		}

		@Override public Trace probe(final Like like) {

			final String expression=like.toExpression();

			final Predicate<String> predicate=java.util.regex.Pattern.compile(expression).asPredicate();

			return trace(values.stream()
					.filter(negate(value -> predicate.test(text(value(value)))))
					.collect(toMap(v -> issue(like), Collections::singleton))
			);
		}

		@Override public Trace probe(final Stem stem) {

			final String prefix=stem.prefix();

			final Predicate<String> predicate=lexical -> lexical.startsWith(prefix);

			return trace(values.stream()
					.filter(negate(value -> predicate.test(text(value(value)))))
					.collect(toMap(v -> issue(stem), Collections::singleton))
			);
		}


		@Override public Trace probe(final MinCount minCount) {

			final int count=values.size();
			final int limit=minCount.limit();

			return count >= limit ? trace() : trace(issue(minCount), Json.createValue(count));
		}

		@Override public Trace probe(final MaxCount maxCount) {

			final int count=values.size();
			final int limit=maxCount.limit();

			return count <= limit ? trace() : trace(issue(maxCount), Json.createValue(count));
		}

		@Override public Trace probe(final All all) {

			final Set<Value> expected=resolve(all.values());
			final Set<Value> actual=values.stream().map(this::value).collect(toSet());

			return actual.containsAll(expected) ? trace() : trace(issue(all));
		}

		@Override public Trace probe(final Any any) {

			final Set<Value> expected=resolve(any.values());
			final Set<Value> actual=values.stream().map(this::value).collect(toSet());

			return expected.stream().anyMatch(actual::contains) ? trace() : trace(issue(any));
		}


		@Override public Trace probe(final Field field) {

			final String alias=fields.entrySet().stream()
					.filter(entry -> entry.getValue().equals(field))
					.map(Map.Entry::getKey)
					.findFirst()
					.orElseThrow(() -> new RuntimeException(format("undefined alias for field <%s>", field.label())));

			return values.stream().map(value -> {

				if ( value instanceof JsonObject ) { // validate the field shape on the new field values

					return trace(emptyMap(), singletonMap(alias, validate(field.value(), Optional
									.ofNullable(((JsonObject)value).get(alias))
									.map(v -> v instanceof JsonArray ? (List<JsonValue>)v : singleton(v))
									.orElseGet(Collections::emptySet)
							)

					));

				} else {

					return trace("unexpected literal", value);

				}

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

