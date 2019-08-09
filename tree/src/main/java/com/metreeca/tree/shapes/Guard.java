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
import com.metreeca.tree.probes.Redactor;

import java.util.*;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;


/**
 * Parametric constraint.
 *
 * <p>States that the focus set meets this shape only if at least one of the externally defined values of an axis
 * variable is included in a given set of target values.</p>
 *
 * @see Redactor
 */
public final class Guard implements Shape {

	public static Guard guard(final String axis, final String... values) {
		return guard(axis, asList(values));
	}

	public static Guard guard(final String axis, final Collection<String> values) {
		return new Guard(axis, values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String axis;
	private final Set<String> values;


	private Guard(final String axis, final Collection<String> values) {

		if ( axis == null ) {
			throw new NullPointerException("null axis");
		}

		if ( values == null ||  values.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		this.axis=axis;
		this.values=new HashSet<>(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getAxis() {
		return axis;
	}

	public Set<String> getValues() {
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
		return this == object || object instanceof Guard
				&& axis.equals(((Guard)object).axis)
				&& values.equals(((Guard)object).values);
	}

	@Override public int hashCode() {
		return axis.hashCode()^values.hashCode();
	}

	@Override public String toString() {
		return "guard("+axis+(values.isEmpty() ? "" : values.stream()
				.collect(joining(", ", " = ", ""))
		)+")";
	}

}
