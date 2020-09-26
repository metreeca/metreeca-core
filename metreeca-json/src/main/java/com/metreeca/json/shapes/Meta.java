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

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

import static java.lang.String.format;


/**
 * Non-validating annotation constraint.
 *
 * <p>States that the enclosing shape has a given value for an annotation property.</p>
 */
public final class Meta extends Shape {

	/**
	 * Creates an alias annotation.
	 *
	 * @param value an alternate property name for reporting values for the enclosing shape (e.g. in the context of
	 *              JSON-based serialization results)
	 *
	 * @return a new alias annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape alias(final String value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return meta("alias", value);
	}

	/**
	 * Creates a label annotation.
	 *
	 * @param value a human-readable textual label for the enclosing shape
	 *
	 * @return a new label annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape label(final String value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Meta("label", value);
	}

	/**
	 * Creates a notes annotation.
	 *
	 * @param value a human-readable textual description for the enclosing shape
	 *
	 * @return a new notes annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape notes(final String value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Meta("notes", value);
	}

	/**
	 * Creates an index annotation.
	 *
	 * @param value a  a storage indexing hint for the enclosing shape
	 *
	 * @return a new index annotation
	 *
	 * @throws NullPointerException if {@code value} is null
	 */
	public static Shape index(final boolean value) {
		return new Meta("index", Boolean.toString(value));
	}


	public static Meta meta(final String label, final String value) {

		if ( label == null ) {
			throw new NullPointerException("null label");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return new Meta(label, value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static Stream<Meta> metas(final Stream<Meta> metas) { // make sure meta annotations are unique

		final Map<Object, Object> mappings=new HashMap<>();

		return metas.filter(meta -> {

			final Object current=mappings.put(meta.label(), meta.value());

			if ( current != null && !current.equals(meta.value()) ) {
				throw new IllegalArgumentException(format("clashing <%s> annotations <%s> / <%s>",
						meta.label(), meta.value(), current
				));
			}

			return true;

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String label;
	private final String value;


	private Meta(final String label, final String value) {
		this.label=label;
		this.value=value;
	}


	public String label() {
		return label;
	}

	public String value() {
		return value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <V> V map(final Probe<V> probe) {

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

}
