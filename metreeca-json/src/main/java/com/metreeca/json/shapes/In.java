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

import org.eclipse.rdf4j.model.Value;

import java.util.*;

import static com.metreeca.json.shapes.Or.or;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableSet;
import static java.util.stream.Collectors.joining;


/**
 * Range set constraint.
 *
 * <p>States that the focus set is a subset a given set of target values (i.e. that each term in the focus set is a
 * member of the given set of target values.</p>
 */
public final class In extends Shape {

	public static Shape in(final Value... values) {
		return in(asList(values));
	}

	public static Shape in(final Collection<? extends Value> values) {
		return in(new LinkedHashSet<>(values));
	}

	public static Shape in(final Set<? extends Value> values) {
		return values.isEmpty() ? or() : new In(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Set<Value> values;


	public In(final Collection<? extends Value> values) {

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

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
		return this == object || object instanceof In
				&& values.equals(((In)object).values);
	}

	@Override public int hashCode() {
		return values.hashCode();
	}

	@Override public String toString() {
		return "in("+(values.isEmpty() ? "" : values.stream()
				.map(v -> v.toString().replace("\n", "\n\t"))
				.collect(joining(",\n\t", "\n\t", "\n"))
		)+")";
	}

}
