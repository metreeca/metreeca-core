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

package com.metreeca.gae.services;

import com.metreeca.gae.GAE;
import com.metreeca.tree.Shape;
import com.metreeca.tree.Trace;
import com.metreeca.tree.shapes.*;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.function.Predicate;

import static com.metreeca.tree.Trace.trace;
import static com.metreeca.tree.shapes.Field.fields;
import static com.metreeca.tree.shapes.Type.type;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toMap;


final class DatastoreValidator {

	Trace validate(final Shape shape, final Object value) {
		if ( GAE.Entity(value) ) {

			final Map<String, Shape> fields=fields(shape);

			final Map<String, Collection<Object>> issues=((PropertyContainer)value).getProperties().keySet().stream()
					.filter(name -> !fields.containsKey(name))
					.map(name -> "unexpected entity property {"+name+"}")
					.collect(toMap(details -> details, details -> emptySet()));

			return trace(trace(issues), shape.map(new ValidatorProbe(value)));

		} else {

			return shape.map(new ValidatorProbe(value));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class ValidatorProbe implements Shape.Probe<Trace> {

		private final Object value; // nullable


		private ValidatorProbe(final Object value) {
			this.value=value;
		}


		private Collection<?> focus() {
			return value instanceof Collection ? (Collection<?>)value : value == null ? emptySet() : singleton(value);
		}

		private <T> Predicate<T> invert(final Predicate<T> predicate) {
			return t -> !predicate.test(t);
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


		@Override public Trace probe(final Type type) {

			final String name=type.getName();

			return trace(focus().stream()
					.filter(invert(v
							-> GAE.Entity(v) ? name.equals(GAE.Entity)
							: GAE.Boolean(v) ? name.equals(GAE.Boolean)
							: GAE.Integral(v) ? name.equals(GAE.Integral)
							: GAE.Floating(v) ? name.equals(GAE.Floating)
							: GAE.String(v) ? name.equals(GAE.String)
							: GAE.Date(v) && name.equals(GAE.Date)
					))
					.collect(toMap(v -> issue(type), Collections::singleton))
			);
		}

		@Override public Trace probe(final Clazz clazz) {

			final String name=clazz.getName();

			return trace(focus().stream()
					.filter(invert(v
							-> v instanceof Entity && ((Entity)v).getKey().getKind().equals(name)
							|| v instanceof EmbeddedEntity && ((EmbeddedEntity)v).getKey().getKind().equals(name)
					))
					.collect(toMap(v -> issue(clazz), Collections::singleton))
			);
		}

		@Override public Trace probe(final MinExclusive minExclusive) {

			final Object value=minExclusive.getValue();

			return trace(focus().stream()
					.filter(invert(v -> GAE.compare(v, value) > 0))
					.collect(toMap(v -> issue(minExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxExclusive maxExclusive) {

			final Object value=maxExclusive.getValue();

			return trace(focus().stream()
					.filter(invert(v -> GAE.compare(v, value) < 0))
					.collect(toMap(v -> issue(maxExclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MinInclusive minInclusive) {

			final Object value=minInclusive.getValue();

			return trace(focus().stream()
					.filter(invert(v -> GAE.compare(v, value) >= 0))
					.collect(toMap(v -> issue(minInclusive), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxInclusive maxInclusive) {

			final Object value=maxInclusive.getValue();

			return trace(focus().stream()
					.filter(invert(v -> GAE.compare(v, value) <= 0))
					.collect(toMap(v -> issue(maxInclusive), Collections::singleton))
			);
		}


		@Override public Trace probe(final MinLength minLength) {

			final int limit=minLength.getLimit();

			return trace(focus().stream()
					.map(Object::toString)
					.filter(invert(s -> s.length() >= limit))
					.collect(toMap(v -> issue(minLength), Collections::singleton))
			);
		}

		@Override public Trace probe(final MaxLength maxLength) {

			final int limit=maxLength.getLimit();

			return trace(focus().stream()
					.map(Object::toString)
					.filter(invert(s -> s.length() <= limit))
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
					.map(Object::toString)
					.filter(invert(s -> compiled.matcher(s).matches()))
					.collect(toMap(v -> issue(pattern), Collections::singleton))
			);
		}

		@Override public Trace probe(final Like like) {

			final String expression=like.toExpression();

			final Predicate<String> predicate=java.util.regex.Pattern.compile(expression).asPredicate();

			return trace(focus().stream()
					.map(Object::toString)
					.filter(invert(predicate))
					.collect(toMap(v -> issue(like), Collections::singleton))
			);
		}

		@Override public Trace probe(final MinCount minCount) {

			final int count=focus().size();
			final int limit=minCount.getLimit();

			return count >= limit ? trace() : trace(issue(minCount));
		}

		@Override public Trace probe(final MaxCount maxCount) {

			final int count=focus().size();
			final int limit=maxCount.getLimit();

			return limit > 1 ? count <= limit ? trace() : trace(issue(maxCount))
					: value instanceof Collection ? trace(issue(maxCount)) : trace();

		}

		@Override public Trace probe(final In in) {
			return in.getValues().containsAll(focus()) ? trace() : trace(issue(in));
		}

		@Override public Trace probe(final All all) {
			return focus().containsAll(all.getValues()) ? trace() : trace(issue(all));
		}

		@Override public Trace probe(final Any any) {
			return !disjoint(focus(), any.getValues()) ? trace() : trace(issue(any));
		}

		@Override public Trace probe(final Field field) {
			return focus().stream()

					.map(v -> {

						if ( GAE.Entity(v) ) {

							return trace(emptyMap(), singletonMap(field.getName(),
									validate(field.getShape(), ((PropertyContainer)v).getProperty(field.getName()))
							));

						} else {

							return trace(issue(type(GAE.Entity)));

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
