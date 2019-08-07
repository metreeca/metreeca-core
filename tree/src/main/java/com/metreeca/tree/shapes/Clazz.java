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
import com.metreeca.tree.probes.Inspector;

import java.util.*;

import static java.util.stream.Collectors.toSet;


/**
 * Class value constraint.
 *
 * <p>States that each value in the focus set is a member of a given resource class or one of its superclasses.</p>
 */
public final class Clazz implements Shape {

	public static Clazz clazz(final String name) {
		return new Clazz(name);
	}

	public static Optional<String> clazz(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new ClazzProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String name;


	private Clazz(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		this.name=name;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public String getName() {
		return name;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Clazz
				&& name.equals(((Clazz)object).name);
	}

	@Override public int hashCode() {
		return name.hashCode();
	}

	@Override public String toString() {
		return "clazz("+name+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ClazzProbe extends Inspector<String> {

		@Override public String probe(final Clazz clazz) {
			return clazz.getName();
		}

		@Override public String probe(final And and) {
			return clazz(and.getShapes());
		}

		@Override public String probe(final Or or) {
			return clazz(or.getShapes());
		}


		private String clazz(final Collection<Shape> shapes) {

			final Set<String> names=shapes.stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return names.size() == 1 ? names.iterator().next() : null;

		}

	}

}
