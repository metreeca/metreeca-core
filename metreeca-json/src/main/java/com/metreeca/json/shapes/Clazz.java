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
import com.metreeca.json.probes.Traverser;

import org.eclipse.rdf4j.model.IRI;

import java.util.*;

import static java.util.stream.Collectors.toSet;


/**
 * Class value constraint.
 *
 * <p>States that each value in the focus set is a member of a given resource class or one of its superclasses.</p>
 */
public final class Clazz implements Shape {

	public static Clazz clazz(final IRI name) {
		return new Clazz(name);
	}

	public static Optional<IRI> clazz(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new ClazzProbe()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI id;


	private Clazz(final IRI id) {

		if ( id == null ) {
			throw new NullPointerException("null id");
		}

		this.id=id;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public IRI id() {
		return id;
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
		return this == object || object instanceof Clazz
				&& id.equals(((Clazz)object).id);
	}

	@Override public int hashCode() {
		return id.hashCode();
	}

	@Override public String toString() {
		return "clazz("+id+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ClazzProbe extends Traverser<IRI> {

		@Override public IRI probe(final Clazz clazz) {
			return clazz.id();
		}

		@Override public IRI probe(final And and) {
			return clazz(and.shapes());
		}

		@Override public IRI probe(final Or or) {
			return clazz(or.shapes());
		}


		private IRI clazz(final Collection<Shape> shapes) {

			final Set<IRI> names=shapes.stream()
					.map(shape -> shape.map(this))
					.filter(Objects::nonNull)
					.collect(toSet());

			return names.size() == 1 ? names.iterator().next() : null;

		}

	}

}
