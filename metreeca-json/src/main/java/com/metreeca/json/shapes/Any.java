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
import com.metreeca.json.Values;

import org.eclipse.rdf4j.model.Value;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.json.Values.format;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;

import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;


/**
 * Existential set values constraint.
 *
 * <p>States that the focus set includes at least one value from a given set of target values.</p>
 */
public final class Any extends Shape {

	public static Shape any(final Value... values) {

		if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return any(asList(values));
	}

	public static Shape any(final Collection<? extends Value> values) {

		if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}
		return values.isEmpty() ? and()
				: values.size() == 1 ? all(values.iterator().next())
				: new Any(values);
	}


	public static Optional<Set<Value>> any(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new AnyProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<Value> values;


	private Any(final Collection<? extends Value> values) {
		this.values=new LinkedHashSet<>(values);
	}


	public Set<Value> values() {
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
		return this == object || object instanceof Any
				&& values.equals(((Any)object).values);
	}

	@Override public int hashCode() {
		return values.hashCode();
	}

	@Override public String toString() {
		return "any("+(values.isEmpty() ? "" : values.stream()
				.map(v -> Values.indent(format(v)))
				.collect(joining(",\n\t", "\n\t", "\n"))
		)+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class AnyProbe extends Probe<Set<Value>> {

		@Override public Set<Value> probe(final Any any) {
			return any.values();
		}

		@Override public Set<Value> probe(final Link link) {
			return link.shape().map(this);
		}

		@Override public Set<Value> probe(final Or or) {
			return or.shapes().stream()
					.map(shape -> shape.map(this))
					.reduce(null, this::union);
		}


		private Set<Value> union(final Set<Value> x, final Set<Value> y) {
			return x == null ? y : y == null ? x
					: unmodifiableSet(Stream.concat(x.stream(), y.stream()).collect(toSet()));
		}

	}

}
