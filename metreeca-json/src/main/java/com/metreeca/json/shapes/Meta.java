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

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;
import com.metreeca.json.probes.Inspector;

import java.util.*;
import java.util.stream.Stream;

import static java.util.stream.Collectors.toSet;


/**
 * Non-validating annotation constraint.
 *
 * <p>States that the enclosing shape has a given value for an annotation property.</p>
 */
public final class Meta implements Shape {

	public static Meta alias(final String value) {
		return new Meta(Shape.Alias, value);
	}

	public static Meta label(final String value) {
		return new Meta(Shape.Label, value);
	}

	public static Meta notes(final String value) {
		return new Meta(Shape.Notes, value);
	}

	public static Meta placeholder(final String value) {
		return new Meta(Shape.Placeholder, value);
	}

	public static Meta dflt(final Object value) {
		return new Meta(Shape.Default, value);
	}

	public static Meta hint(final String value) {
		return new Meta(Shape.Hint, value);
	}

	public static Meta group(final String value) {
		return new Meta(Shape.Group, value);
	}

	public static Meta index(final boolean value) {
		return new Meta(Shape.Index, value);
	}


	public static Optional<String> alias(final Shape shape) {
		return meta(Alias, shape, String.class);
	}

	public static Optional<String> label(final Shape shape) {
		return meta(Label, shape, String.class);
	}

	public static Optional<String> notes(final Shape shape) {
		return meta(Notes, shape, String.class);
	}

	public static Optional<String> placeholder(final Shape shape) {
		return meta(Placeholder, shape, String.class);
	}

	public static Optional<String> dflt(final Shape shape) {
		return meta(Default, shape, String.class);
	}

	public static Optional<String> hint(final Shape shape) {
		return meta(Hint, shape, String.class);
	}

	public static Optional<String> group(final Shape shape) {
		return meta(Group, shape, String.class);
	}

	public static Optional<Boolean> index(final Shape shape) {
		return meta(Index, shape, Boolean.class);
	}


	public static Meta meta(final Object label, final Object value) {
		return new Meta(label, value);
	}

	public static Optional<Object> meta(final Object label, final Shape shape) {
		return Optional.ofNullable(shape.map(new MetaProbe(label)));
	}

	public static <T> Optional<T> meta(final Object label, final Shape shape, final Class<T> clazz) {
		return meta(label, shape)
				.filter(clazz::isInstance)
				.map(clazz::cast);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Object label;
	private final Object value;


	private Meta(final Object label, final Object value) {

		if ( label == null ) {
			throw new NullPointerException("null label");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		this.label=label;
		this.value=value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Object label() {
		return label;
	}

	public Object value() {
		return value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Meta
				&& label.equals(((Meta)object).label)
				&& value.equals(((Meta)object).value);
	}

	@Override public int hashCode() {
		return label.hashCode()^value.hashCode();
	}

	@Override public String toString() {
		return "meta("+label+"="+value+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class MetaProbe extends Inspector<Object> {

		private final Object label;


		private MetaProbe(final Object label) {
			this.label=label;
		}


		@Override public Object probe(final Meta meta) {
			return meta.label().equals(label) ? meta.value() : null;
		}


		@Override public Object probe(final Field field) {
			return null;
		}


		@Override public Object probe(final And and) {
			return probe(and.shapes().stream());
		}

		@Override public Object probe(final Or or) {
			return probe(or.shapes().stream());
		}

		@Override public Object probe(final When when) {
			return probe(Stream.of(when.pass(), when.fail()));
		}


		private Object probe(final Stream<Shape> shapes) {

			final Set<Object> values=shapes
					.map(s -> s.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return values.size() == 1 ? values.iterator().next() : null;
		}

	}

}
