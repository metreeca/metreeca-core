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

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;

import static com.metreeca.spec.things.Lists.list;
import static com.metreeca.spec.things.Sets.set;
import static com.metreeca.spec.things.Strings.indent;

import static java.util.stream.Collectors.joining;


/**
 * Disjunctive logical constraint.
 *
 * <p>States that the focus set is consistent with at least one shape in a given target set.</p>
 */
public final class Or implements Shape {

	private static final Or empty=new Or(set());


	public static Or or() {
		return empty;
	}

	public static Or or(final Shape... shapes) {
		return or(list(shapes));
	}

	public static Or or(final Collection<Shape> shapes) {
		return new Or(shapes);
	}


	private final Collection<Shape> shapes;


	public Or(final Collection<Shape> shapes) {

		if ( shapes == null ) {
			throw new NullPointerException("null shapes");
		}

		if ( shapes.contains(null) ) {
			throw new NullPointerException("null shape");
		}

		this.shapes=new LinkedHashSet<>(shapes);
	}


	public Collection<Shape> getShapes() {
		return Collections.unmodifiableCollection(shapes);
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Or
				&& shapes.equals(((Or)object).shapes);
	}

	@Override public int hashCode() {
		return shapes.hashCode();
	}

	@Override public String toString() {
		return "or("+(shapes.isEmpty() ? "" : shapes.stream()
				.map(shape -> indent(shape.toString()))
				.collect(joining(",\n", "\n", "\n"))
		)+")";
	}

}
