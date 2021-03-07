/*
 * Copyright © 2013-2021 Metreeca srl
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
	public static final String View="view";
	public static final String Mode="mode";

	public static final String Create="create";
	public static final String Relate="relate";
	public static final String Update="update";
	public static final String Delete="delete";

	public static final String Digest="digest";
	public static final String Detail="detail";

	public static final String Convey="convey";
	public static final String Filter="filter";
	public static final String Expose="expose";


	//// Parametric Guards /////////////////////////////////////////////////////////////////////////////////////////////

	public static Shape role(final Object... roles) { return guard(Role, roles); }

	public static Shape task(final Object... tasks) { return guard(Task, tasks); }

	public static Shape view(final Object... views) { return guard(View, views); }

	public static Shape mode(final Object... modes) { return guard(Mode, modes); }


	public static Shape create(final Shape... shapes) { return task(Create).then(shapes); }

	public static Shape relate(final Shape... shapes) { return task(Relate).then(shapes); }

	public static Shape update(final Shape... shapes) { return task(Update).then(shapes); }

	public static Shape delete(final Shape... shapes) { return task(Delete).then(shapes); }

	/*
	 * Marks shapes as server-defined read-only.
	 */
	public static Shape server(final Shape... shapes) { return task(Relate, Delete).then(shapes); }

	/*
	 * Marks shapes as client-defined write-once.
	 */
	public static Shape client(final Shape... shapes) { return task(Create, Relate, Delete).then(shapes); }


	public static Shape digest(final Shape... shapes) { return view(Digest).then(shapes); }

	public static Shape detail(final Shape... shapes) { return view(Detail).then(shapes); }


	public static Shape convey(final Shape... shapes) { return mode(Convey).then(shapes); }

	public static Shape filter(final Shape... shapes) { return mode(Filter).then(shapes); }

	public static Shape expose(final Shape... shapes) { return mode(Expose).then(shapes); }


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
