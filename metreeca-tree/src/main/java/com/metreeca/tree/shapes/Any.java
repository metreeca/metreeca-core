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
import com.metreeca.tree.probes.Inspector;

import java.util.*;
import java.util.stream.Stream;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;


/**
 * Existential set values constraint.
 *
 * <p>States that the focus set includes at least one value from a given set of target values.</p>
 */
public final class Any implements Shape {

	public static Any any(final Object... values) {
		return any(asList(values));
	}

	public static Any any(final Collection<Object> values) {
		return new Any(values);
	}


	public static Optional<Set<Object>> any(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new AnyProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<Object> values;


	private Any(final Collection<Object> values) {

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		this.values=new LinkedHashSet<>(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Set<Object> getValues() {
		return unmodifiableSet(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Any
				&& values.equals(((Any)object).values);
	}

	@Override public int hashCode() {
		return values.hashCode();
	}

	@Override public String toString() {
		return "any("+(values.isEmpty() ? "" : values.stream()
				.map(v -> v.toString().replace("\n", "\n\t"))
				.collect(joining(",\n\t", "\n\t", "\n"))
		)+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class AnyProbe extends Inspector<Set<Object>> {

		@Override public Set<Object> probe(final Any any) {
			return any.getValues();
		}

		@Override public Set<Object> probe(final Or or) {
			return or.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, this::union);
		}


		private Set<Object> union(final Set<Object> x, final Set<Object> y) {
			return x == null ? y : y == null ? x
					: unmodifiableSet(Stream.concat(x.stream(), y.stream()).collect(toSet()));
		}

	}

}