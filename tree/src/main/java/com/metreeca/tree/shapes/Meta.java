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

package com.metreeca.tree.shapes;

import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Traverser;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;


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


	public static Meta meta(final String label, final Object value) {
		return new Meta(label, value);
	}


	public static Map<String, Object> metas(final Shape shape) {
		return shape == null ? emptyMap() : shape.map(new MetaProbe()).collect(toMap(
				Map.Entry::getKey,
				Map.Entry::getValue,
				(x, y) -> Objects.equals(x, y) ? x : null
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String label;
	private final Object value;


	private Meta(final String label, final Object value) {

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

	public String getLabel() {
		return label;
	}

	public Object getValue() {
		return value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


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

	private static final class MetaProbe extends Traverser<Stream<Map.Entry<String, Object>>> {

		@Override public Stream<Map.Entry<String, Object>> probe(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<Map.Entry<String, Object>> probe(final Meta meta) {
			return Stream.of(new SimpleImmutableEntry<>(meta.getLabel(), meta.getValue()));
		}


		@Override public Stream<Map.Entry<String, Object>> probe(final Field field) {
			return Stream.empty();
		}


		@Override public Stream<Map.Entry<String, Object>> probe(final And and) {
			return and.getShapes().stream().flatMap(s -> s.map(this));
		}

		@Override public Stream<Map.Entry<String, Object>> probe(final Or or) {
			return or.getShapes().stream().flatMap(s -> s.map(this));
		}

		@Override public Stream<Map.Entry<String, Object>> probe(final When when) {
			return Stream.of(when.getPass(), when.getFail()).flatMap(s -> s.map(this));
		}

	}

}
