/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.gcp.services;

import com.metreeca.rest.Failure;
import com.metreeca.rest.Message;
import com.metreeca.rest.Result;
import com.metreeca.tree.Shape;
import com.metreeca.tree.Trace;
import com.metreeca.tree.shapes.*;

import com.google.cloud.datastore.*;

import java.util.*;
import java.util.function.Predicate;

import static com.metreeca.gcp.formats.EntityFormat.entity;
import static com.metreeca.gcp.services.Datastore.values;
import static com.metreeca.rest.Failure.invalid;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.tree.Trace.trace;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.fields;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


final class DatastoreValidator extends DatastoreProcessor {

	private final Datastore datastore;


	DatastoreValidator(final Datastore datastore) {
		this.datastore=datastore;
	}


	<M extends Message<M>> Result<M, Failure> validate(final M message) {
		return message

				.body(entity(datastore))

				.process(entity -> Optional.of(validate(convey(message.shape()), EntityValue.of(entity)))
						.filter(trace -> !trace.isEmpty())
						.map(trace -> Result.<M, Failure>Error(invalid(trace)))
						.orElseGet(() -> Value(message))
				);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Trace validate(final Shape shape, final Value<?> value) {
		if ( value.getType() == ValueType.ENTITY ) {

			final Map<Object, Shape> fields=fields(shape);

			final Map<String, Collection<Object>> issues=((EntityValue)value).get().getProperties().entrySet().stream()
					.filter(entry -> !fields.containsKey(entry.getKey()))
					.collect(toMap(
							entry -> "unexpected entity property {"+entry.getKey()+"}",
							entry -> entry.getValue() instanceof ListValue
									? ((ListValue)entry.getValue()).get().stream().map(Value::get).collect(toList())
									: singleton(entry.getValue().get())
					));

			return trace(trace(issues), shape.map(new ValidatorProbe(value)));

		} else {

			return shape.map(new ValidatorProbe(value));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class ValidatorProbe implements Shape.Probe<Trace> {

		private final Value<?> value;


		private ValidatorProbe(final Value<?> value) {
			this.value=value;
		}


		private Collection<? extends Value<?>> focus() {
			return value.getType() == ValueType.NULL ? emptySet()
					: value.getType() == ValueType.LIST ? ((ListValue)value).get()
					: singleton(value);
		}

		private <T> Predicate<T> negate(final Predicate<T> predicate) {
			return predicate.negate();
		}

		private String issue(final Shape shape) {
			return shape.toString().replaceAll("\\s+", " ");
		}


		@Override public Trace probe(final Meta meta) {
			return trace();
		}

		@Override public Trace probe(final Guard guard) {
			throw new UnsupportedOperationException("guard shape");
		}


		@Override public Trace probe(final Datatype datatype) {

			final Object name=datatype.getName();

			return trace(focus().stream()
					.filter(negate(v -> v.getType() == name))
					.collect(toMap(v -> issue(datatype), Collections::singleton))
			);
		}

		@Override public Trace probe(final Clazz clazz) {

			final Object name=clazz.getName();

			return trace(focus().stream()
					.filter(negate(v -> v.getType() == ValueType.ENTITY && Optional.of((EntityValue)v)
							.map(Value::get)
							.map(BaseEntity::getKey)
							.map(BaseKey::getKind)
							.filter(s -> s.equals(name))
							.isPresent()
					))
					.collect(toMap(v -> issue(clazz), Collections::singleton))
			);
		}

		@Override public Trace probe(final MinExclusive minExclusive) {

			final Object value=minExclusive.getValue();

			return trace(focus().stream()
					.filter(negate(v -> Datastore.compare(v, Datastore.value(value)) > 0))
					.collect(toMap(v -> issue(minExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxExclusive maxExclusive) {

			final Object value=maxExclusive.getValue();

			return trace(focus().stream()
					.filter(negate(v -> Datastore.compare(v, Datastore.value(value)) < 0))
					.collect(toMap(v -> issue(maxExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MinInclusive minInclusive) {

			final Object value=minInclusive.getValue();

			return trace(focus().stream()
					.filter(negate(v -> Datastore.compare(v, Datastore.value(value)) >= 0))
					.collect(toMap(v -> issue(minInclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxInclusive maxInclusive) {

			final Object value=maxInclusive.getValue();

			return trace(focus().stream()
					.filter(negate(v -> Datastore.compare(v, Datastore.value(value)) <= 0))
					.collect(toMap(v -> issue(maxInclusive), Collections::singleton))
			);
		}


		@Override public Trace probe(final MinLength minLength) {

			final int limit=minLength.getLimit();

			return trace(focus().stream()
					.map(Value::get)
					.map(String::valueOf)
					.filter(negate(s -> s.length() >= limit))
					.collect(toMap(v -> issue(minLength), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxLength maxLength) {

			final int limit=maxLength.getLimit();

			return trace(focus().stream()
					.map(Value::get)
					.map(String::valueOf)
					.filter(negate(s -> s.length() <= limit))
					.collect(toMap(v -> issue(maxLength), Collections::singleton))
			);
		}

		@Override public Trace probe(final Pattern pattern) {

			final String expression=pattern.getText();
			final String flags=pattern.getFlags();

			final java.util.regex.Pattern compiled=java.util.regex.Pattern
					.compile(flags.isEmpty() ? expression : "(?"+flags+":"+expression+")");

			// match the whole string: don't use compiled.asPredicate() (implemented using .find())

			return trace(focus().stream()
					.map(Value::get)
					.map(String::valueOf)
					.filter(negate(s -> compiled.matcher(s).matches()))
					.collect(toMap(v -> issue(pattern), Collections::singleton))
			);
		}

		@Override public Trace probe(final Like like) {

			final String expression=like.toExpression();

			final Predicate<String> predicate=java.util.regex.Pattern.compile(expression).asPredicate();

			return trace(focus().stream()
					.map(Value::get)
					.map(String::valueOf)
					.filter(negate(predicate))
					.collect(toMap(v -> issue(like), Collections::singleton))
			);
		}

		@Override public Trace probe(final MinCount minCount) {

			final int count=focus().size();
			final int limit=minCount.getLimit();

			return count >= limit ? trace() : trace(issue(minCount), count);
		}

		@Override public Trace probe(final MaxCount maxCount) {

			final int count=focus().size();
			final int limit=maxCount.getLimit();

			return limit > 1 ? count <= limit ? trace() : trace(issue(maxCount), count)
					: value.getType() == ValueType.LIST ? trace(issue(maxCount), count) : trace(); // limit == 1 => not list

		}

		@Override public Trace probe(final In in) {

			final Set<Value<?>> range=values(in.getValues());

			final List<Value<?>> unexpected=focus()
					.stream()
					.filter(negate(range::contains))
					.collect(toList());

			return unexpected.isEmpty() ? trace() : trace(issue(in), unexpected);
		}

		@Override public Trace probe(final All all) {

			final Collection<? extends Value<?>> focus=focus();
			final Set<Value<?>> range=values(all.getValues());

			final List<Value<?>> missing=range
					.stream()
					.filter(negate(focus::contains))
					.collect(toList());

			return missing.isEmpty() ? trace() : trace(issue(all), missing);
		}

		@Override public Trace probe(final Any any) {
			return !disjoint(focus(), values(any.getValues())) ? trace() : trace(issue(any));
		}

		@Override public Trace probe(final Field field) {
			return focus().stream()

					.map(v -> {

						if ( v.getType() == ValueType.ENTITY ) {

							final String name=field.getName().toString();
							final FullEntity<?> entity=((EntityValue)v).get();
							final Value<?> value=entity.contains(name) ? entity.getValue(name) : NullValue.of();

							return trace(emptyMap(), singletonMap(name, validate(field.getShape(), value)));

						} else {

							return trace(issue(datatype(ValueType.ENTITY)));

						}

					})

					.reduce(trace(), Trace::trace);
		}

		@Override public Trace probe(final And and) {
			return and.getShapes().stream()
					.map(s -> s.map(this))
					.reduce(trace(), Trace::trace);
		}

		@Override public Trace probe(final Or or) {
			return or.getShapes().stream().anyMatch(s -> s.map(this).isEmpty()) ? trace() : trace(issue(or));
		}

		@Override public Trace probe(final When when) {
			throw new UnsupportedOperationException("conditional shape");
		}

	}

}
