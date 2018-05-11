/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec.shapes;

import com.metreeca.spec.Shape;

import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.metreeca.jeep.Jeep.list;
import static com.metreeca.jeep.Strings.indent;
import static com.metreeca.spec.Values.format;

import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;


/**
 * Range set constraint.
 *
 * <p>States that the focus set is a subset a given set of target values (i.e. that each term in the focus set is a
 * member of the given set of target values.</p>
 */
public final class In implements Shape {

	public static In in(final Value... values) {
		return new In(list(values));
	}

	public static In in(final Collection<Value> values) {
		return new In(values);
	}


	private final Set<Value> values;


	public In(final Collection<Value> values) {

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


	public Set<Value> getValues() {
		return unmodifiableSet(values);
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof In
				&& values.equals(((In)object).values);
	}

	@Override public int hashCode() {
		return values.hashCode();
	}

	@Override public String toString() {
		return "in("+(values.isEmpty() ? "" : values.stream()
				.map(v -> indent(format(v)))
				.collect(joining(",\n", "\n", "\n"))
		)+")";
	}

}
