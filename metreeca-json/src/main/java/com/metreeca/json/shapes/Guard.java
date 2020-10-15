/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public final class Guard extends Shape {

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

	public static Function<Guard, Boolean> retain(final String axis, final boolean status) {
		return guard -> guard.axis.equals(axis)
				? status
				: null;
	}

	public static Function<Guard, Boolean> retain(final String axis, final Object value) {
		return guard -> guard.axis.equals(axis)
				? guard.values.contains(value)
				: null;
	}

	public static Function<Guard, Boolean> retain(final String axis, final Object... values) {
		return guard -> guard.axis.equals(axis)
				? Arrays.stream(values).anyMatch(guard.values::contains)
				: null;
	}

	public static Function<Guard, Boolean> retain(final String axis, final Collection<?> values) {
		return guard -> guard.axis.equals(axis)
				? values.stream().anyMatch(guard.values::contains)
				: null;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape guard(final String axis, final Object... values) {
		return guard(axis, asList(values));
	}

	public static Shape guard(final String axis, final Collection<Object> values) {

		if ( axis == null ) {
			throw new NullPointerException("null axis");
		}

		if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return new Guard(axis, values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String axis;
	private final Set<Object> values;


	private Guard(final String axis, final Collection<Object> values) {
		this.axis=axis;
		this.values=new HashSet<>(values);
	}


	public String axis() {
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
