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

package com.metreeca.tree.probes;

import com.metreeca.tree.Shape;
import com.metreeca.tree.shapes.*;

import java.util.*;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.util.Arrays.asList;
import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toList;


/**
 * Shape redactor.
 *
 * <p>Recursively evaluates {@linkplain Guard parametric} constraints in a shape.</p>
 */
public final class Redactor extends Traverser<Shape> {

	private final String axis;
	private final Set<String> values;


	/**
	 * Creates a new shape redactor.
	 *
	 * @param axis   the identifier of the parametric axis to be redacted
	 * @param values the driving values for the parametric {@code axis}; empty to unconditionally remove guards on
	 *               {@code axis}
	 *
	 * @throws NullPointerException if either {@code axis} or {@code values} is null or {@code values} contains a null
	 *                              value
	 */
	public Redactor(final String axis, final String... values) { this(axis, asList(values)); }

	/**
	 * Creates a new shape redactor.
	 *
	 * @param axis   the identifier of the parametric axis to be redacted
	 * @param values the driving values for the parametric {@code axis}; empty to unconditionally remove guards on
	 *               {@code axis}
	 *
	 * @throws NullPointerException if either {@code axis} or {@code values} is null or {@code values} contains a null
	 *                              value
	 */
	public Redactor(final String axis, final Collection<String> values) {

		if ( axis == null ) {
			throw new NullPointerException("null axis");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null value");
		}

		this.axis=axis;
		this.values=new HashSet<>(values);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Shape probe(final Shape shape) { return shape; }


	@Override public Shape probe(final Guard guard) {

		final Set<String> accepted=guard.getValues();

		return axis.equals(guard.getAxis())
				? values.isEmpty() || !disjoint(accepted, values) ? and() : or()
				: guard; // ignore unrelated variables
	}


	@Override public Shape probe(final Field field) {
		return field(field.getName(), field.getShape().map(this));
	}


	@Override public Shape probe(final And and) {
		return and(and.getShapes().stream().map(shape -> shape.map(this)).collect(toList()));
	}

	@Override public Shape probe(final Or or) {
		return or(or.getShapes().stream().map(shape -> shape.map(this)).collect(toList()));
	}

	@Override public Shape probe(final When when) {
		return when(
				when.getTest().map(this),
				when.getPass().map(this),
				when.getFail().map(this)
		);
	}

}
