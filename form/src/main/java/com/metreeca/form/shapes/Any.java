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

package com.metreeca.form.shapes;

import com.metreeca.form.Shape;
import com.metreeca.form.probes.Inspector;

import org.eclipse.rdf4j.model.Value;

import java.util.*;

import static com.metreeca.form.things.Sets.intersection;
import static com.metreeca.form.things.Strings.indent;
import static com.metreeca.form.things.Values.format;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;


/**
 * Existential set values constraint.
 *
 * <p>States that the focus set includes at least one value from a given set of target values.</p>
 */
public final class Any implements Shape {

	public static Any any(final Value... values) {
		return any(asList(values));
	}

	public static Any any(final Collection<Value> values) {
		return new Any(values);
	}


	public static Optional<Set<Value>> any(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new AnyProbe()));
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<Value> values;


	private Any(final Collection<Value> values) {

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.isEmpty() ) {
			throw new IllegalArgumentException("empty values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		this.values=new LinkedHashSet<>(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Set<Value> getValues() {
		return Collections.unmodifiableSet(values);
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
				.map(v -> indent(format(v)))
				.collect(joining(",\n", "\n", "\n"))
		)+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class AnyProbe extends Inspector<Set<Value>> {

		@Override public Set<Value> probe(final Any any) {
			return any.getValues();
		}

		@Override public Set<Value> probe(final And and) {
			return and.getShapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, (x, y) -> x == null ? y : y == null ? x : intersection(x, y));
		}

	}

}
