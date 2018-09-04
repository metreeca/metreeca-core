/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.shapes;

import com.metreeca.form.Shape;

import java.util.Collection;

import static com.metreeca.form.things.Lists.list;


/**
 * Group annotation.
 *
 * <p>Groups multiple shapes for presentation purposes.</p>
 */
public final class Group implements Shape {

	public static Group group(final Shape... shapes) {
		return group(list(shapes));
	}

	public static Group group(final Collection<Shape> shapes) {
		return new Group(And.and(shapes));
	}

	public static Group group(final Shape shape) {
		return new Group(shape);
	}


	private final Shape shape;


	public Group(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape;
	}


	public Shape getShape() {
		return shape;
	}


	@Override public <T> T accept(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.visit(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Group
				&& shape.equals(((Group)object).shape);
	}

	@Override public int hashCode() {
		return shape.hashCode();
	}

	@Override public String toString() {
		return "group("+shape+")";
	}
}
