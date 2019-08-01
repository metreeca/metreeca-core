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
 * Kind value constraint.
 *
 * <p>States that each value in the focus set is a member of a given resource kind.</p>
 */
public final class Kind implements Shape {

	public static Kind kind(final Object kind) {
		return new Kind(kind);
	}

	public static Optional<Object> kind(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new KindProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Object kind;


	private Kind(final Object kind) {

		if ( kind == null ) {
			throw new NullPointerException("null kind");
		}

		this.kind=kind;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Object getKind() {
		return kind;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Kind
				&& kind.equals(((Kind)object).kind);
	}

	@Override public int hashCode() {
		return kind.hashCode();
	}

	@Override public String toString() {
		return "kind("+kind+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class KindProbe extends Inspector<Object> {

		@Override public Object probe(final Kind kind) {
			return kind.getKind();
		}

		@Override public Object probe(final And and) {
			return kind(and.getShapes());
		}

		@Override public Object probe(final Or or) {
			return kind(or.getShapes());
		}


		private Object kind(final Collection<Shape> shapes) {

			final Set<Object> kinds=shapes.stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return kinds.size() == 1 ? kinds.iterator().next() : null;

		}

	}

}
