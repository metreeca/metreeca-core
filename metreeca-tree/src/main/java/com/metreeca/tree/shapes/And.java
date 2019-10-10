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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import static java.util.Arrays.asList;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;


/**
 * Conjunctive logical constraint.
 *
 * <p>States that the focus set is consistent with all shapes in a given target set.</p>
 */
public final class And implements Shape {

	private static final And empty=new And(emptySet());


	public static And and() { return empty; }

	@SafeVarargs public static <S extends Shape> And and(final S... shapes) {
		return new And(asList(shapes));
	}

	public static <S extends Shape> And and(final Collection<S> shapes) {
		return new And(shapes);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Collection<Shape> shapes;


	private And(final Collection<? extends Shape> shapes) {

		if ( shapes == null ) {
			throw new NullPointerException("null shapes");
		}

		if ( shapes.contains(null) ) {
			throw new NullPointerException("null shape");
		}

		this.shapes=new LinkedHashSet<>(shapes);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Collection<Shape> getShapes() {
		return Collections.unmodifiableCollection(shapes);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof And
				&& shapes.equals(((And)object).shapes);
	}

	@Override public int hashCode() {
		return shapes.hashCode();
	}

	@Override public String toString() {
		return "and("+(shapes.isEmpty() ? "" : shapes.stream()
				.map(shape -> shape.toString().replace("\n", "\n\t"))
				.collect(joining(",\n\t", "\n\t", "\n"))
		)+")";
	}

}