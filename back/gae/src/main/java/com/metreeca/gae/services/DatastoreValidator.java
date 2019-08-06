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

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.PropertyContainer;

import java.util.Collection;
import java.util.Map;
import java.util.function.Predicate;
import java.util.stream.Stream;

import static com.metreeca.tree.Trace.trace;
import static com.metreeca.tree.shapes.Field.fields;
import static com.metreeca.tree.shapes.Type.type;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;
import static java.util.stream.Collectors.toSet;


final class DatastoreValidator {

	Trace validate(final Shape shape, final Entity entity) {
		return validate(shape, singleton(entity));
	}

	private Trace validate(final Shape shape, final Collection<?> focus) {

		final Map<String, Shape> fields=fields(shape);

		final Trace x=fields.isEmpty() ? trace() : trace(focus.stream()

				.flatMap(v -> {

					if ( v instanceof PropertyContainer ) {

						return ((PropertyContainer)v).getProperties().keySet().stream()
								.filter(name -> !fields.containsKey(name))
								.map(name -> "unexpected entity property {"+name+"}");

					} else {

						return Stream.empty();

					}

				})

				.collect(toSet())
		);


		final Trace y=shape.map(new ValidatorProbe(focus));

		return merge(x, y);
	}

	private Trace merge(final Trace x, final Trace y) {
		return x.isEmpty() ? y : y.isEmpty() ? x : trace(
				Stream.of(x, y).flatMap(t -> t.getIssues().stream()).collect(toList()),
				Stream.of(x, y).flatMap(t -> t.getFields().entrySet().stream()).collect(toMap(Map.Entry::getKey, Map.Entry::getValue))
		);
	}

	private String issue(final Shape shape) {
		return shape.toString().replaceAll("\\s+", " ");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class ValidatorProbe implements Shape.Probe<Trace> {

		private final Collection<?> focus;


		private ValidatorProbe(final Collection<?> focus) {
			this.focus=focus;
		}


		@Override public Trace probe(final Meta meta) {
			return trace();
		}

		@Override public Trace probe(final Guard guard) {
			throw new UnsupportedOperationException("guard shape");
		}


		@Override public Trace probe(final Type type) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Trace probe(final Clazz clazz) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}


		@Override public Trace probe(final MinExclusive minExclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Trace probe(final MaxExclusive maxExclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Trace probe(final MinInclusive minInclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}

		@Override public Trace probe(final MaxInclusive maxInclusive) {
			throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		}


		@Override public Trace probe(final MinLength minLength) {

			final int limit=minLength.getLimit();

			return trace(focus.stream()
					.map(Object::toString)
					.filter(s -> s.length() < limit)
					.map(s -> issue(minLength))
					.collect(toList())
			);
		}

		@Override public Trace probe(final MaxLength maxLength) {

			final int limit=maxLength.getLimit();

			return trace(focus.stream()
					.map(Object::toString)
					.filter(s -> s.length() > limit)
					.map(s -> issue(maxLength))
					.collect(toList())
			);
		}

		@Override public Trace probe(final Pattern pattern) {

			final String expression=pattern.getText();
			final String flags=pattern.getFlags();

			final java.util.regex.Pattern compiled=java.util.regex.Pattern
					.compile(flags.isEmpty() ? expression : "(?"+flags+":"+expression+")");

			// match the whole string: don't use compiled.asPredicate() (implemented using .find())

			return trace(focus.stream()
					.map(Object::toString)
					.filter(s -> !compiled.matcher(s).matches())
					.map(s -> issue(pattern))
					.collect(toList())
			);
		}

		@Override public Trace probe(final Like like) {

			final String expression=like.toExpression();

			final Predicate<String> predicate=java.util.regex.Pattern.compile(expression).asPredicate();

			return trace(focus.stream()
					.map(Object::toString)
					.filter(s -> !predicate.test(s))
					.map(s -> issue(like))
					.collect(toList())
			);
		}

		@Override public Trace probe(final MinCount minCount) {

			final int count=focus.size();
			final int limit=minCount.getLimit();

			return count >= limit ? trace() : trace(issue(minCount));
		}

		@Override public Trace probe(final MaxCount maxCount) {

			final int count=focus.size();
			final int limit=maxCount.getLimit();

			return count <= limit ? trace() : trace(issue(maxCount));
		}

		@Override public Trace probe(final In in) {
			return in.getValues().containsAll(focus)? trace() : trace(issue(in));
		}

		@Override public Trace probe(final All all) {
			return focus.containsAll(all.getValues())? trace() : trace(issue(all));
		}

		@Override public Trace probe(final Any any) {
			return !disjoint(focus, any.getValues())? trace() : trace(issue(any));
		}

		@Override public Trace probe(final Field field) {
			return focus.stream()

					.map(v -> {

						if ( v instanceof PropertyContainer ) {

							final Object value=((PropertyContainer)v).getProperty(field.getName());

							final Collection<?> focus=value == null ? emptyList()
									: value instanceof Collection ? (Collection<?>)value
									: singleton(value);

							return  trace(emptySet(), singletonMap(
									field.getName(), validate(field.getShape(), focus)
							));

						} else {

							return trace(issue(type(GAE.Entity)));

						}

					})

					.reduce(trace(), DatastoreValidator.this::merge);
		}

		@Override public Trace probe(final And and) {
			return and.getShapes().stream()
					.map(s -> s.map(this))
					.reduce(trace(), DatastoreValidator.this::merge);
		}

		@Override public Trace probe(final Or or) {
			return or.getShapes().stream().anyMatch(s -> s.map(this).isEmpty())? trace() : trace(issue(or));
		}

		@Override public Trace probe(final When when) {
			throw new UnsupportedOperationException("conditional shape");
		}

	}

}
