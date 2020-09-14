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

import java.util.*;
import java.util.function.Function;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;


/**
 * Parametric annotation.
 *
 * <p>States that the focus set meets this shape only if at least one of the externally defined values of an axis
 * variable is included in a given set of target values.</p>
 *
 * @see Shape#redact(Function[])
 */
public final class Guard implements Shape {

	//// Parametric Axes and Values ////////////////////////////////////////////////////////////////////////////////////

	public static final String Role="role";
	public static final String Task="task";
	public static final String Area="area";
	public static final String Mode="mode";

	public static final String Create="create";
	public static final String Relate="relate";
	public static final String Update="update";
	public static final String Delete="delete";

	public static final String Target="target";

	public static final String Digest="digest";
	public static final String Detail="detail";

	public static final String Convey="convey";
	public static final String Filter="filter";


	//// Parametric Guards /////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape role(final Object... roles) { return guard(Role, roles); }

	public static Shape task(final Object... tasks) { return guard(Task, tasks); }

	public static Shape area(final Object... areas) { return guard(Area, areas); }

	public static Shape mode(final Object... modes) { return guard(Mode, modes); }


	public static Shape create() { return task(Create); }

	public static Shape relate() { return task(Relate); }

	public static Shape update() { return task(Update); }

	public static Shape delete() { return task(Delete); }

	/*
	 * Marks shapes as server-defined internal.
	 */
	public static Shape hidden() { return task(Delete); }

	/*
	 * Marks shapes as server-defined read-only.
	 */
	public static Shape server() { return task(Relate, Delete); }

	/*
	 * Marks shapes as client-defined write-once.
	 */
	public static Shape client() { return task(Create, Relate, Delete); }


	public static Shape target() { return area(Target); }

	public static Shape member() { return area(Digest, Detail); }


	public static Shape digest() { return area(Digest); }

	public static Shape detail() { return area(Detail); }

	public static Shape convey() { return mode(Convey); }

	public static Shape filter() { return mode(Filter); }


	//// Guard Evaluators //////////////////////////////////////////////////////////////////////////////////////////////

	public static Function<Guard, Boolean> retain(final Object axis) {
		return guard -> guard.axis.equals(axis)
				? true
				: null;
	}

	public static Function<Guard, Boolean> retain(final Object axis, final Object value) {
		return guard -> guard.axis.equals(axis)
				? guard.values.contains(value)
				: null;
	}

	public static Function<Guard, Boolean> retain(final Object axis, final Object... values) {
		return guard -> guard.axis.equals(axis)
				? values.length == 0 || Arrays.stream(values).anyMatch(guard.values::contains)
				: null;
	}

	public static Function<Guard, Boolean> retain(final Object axis, final Collection<?> values) {
		return guard -> guard.axis.equals(axis)
				? values.isEmpty() || values.stream().anyMatch(guard.values::contains)
				: null;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape guard(final Object axis, final Object... values) {
		return guard(axis, asList(values));
	}

	public static Shape guard(final Object axis, final Collection<Object> values) {
		return new Guard(axis, values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Object axis;
	private final Set<Object> values;


	private Guard(final Object axis, final Collection<Object> values) {

		if ( axis == null ) {
			throw new NullPointerException("null axis");
		}

		if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		this.axis=axis;
		this.values=new HashSet<>(values);
	}


	public Object axis() {
		return axis;
	}

	public Set<Object> values() {
		return unmodifiableSet(values);
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
		return this == object || object instanceof Guard
				&& axis.equals(((Guard)object).axis)
				&& values.equals(((Guard)object).values);
	}

	@Override public int hashCode() {
		return axis.hashCode()^values.hashCode();
	}

	@Override public String toString() {
		return "guard("+axis+(values.isEmpty() ? "" : values.stream()
				.map(Object::toString)
				.collect(joining(", ", " = ", ""))
		)+")";
	}

}
