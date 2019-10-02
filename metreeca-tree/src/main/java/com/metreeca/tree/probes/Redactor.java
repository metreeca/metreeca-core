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

import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

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

	private final Predicate<Set<String>> condition;


	/**
	 * Creates a new shape redactor.
	 *
	 * @param axis   the identifier of the parametric axis to be redacted
	 * @param values the accepted values for the parametric {@code axis}; successfully evaluates guards on {@code axis}
	 *               if the guard accepts at least one of the values in the collection
	 *
	 * @throws NullPointerException if either {@code axis} or {@code values} is null or {@code values} contains a null
	 *                              value
	 */
	public Redactor(final String axis, final String... values) { this(axis, asList(values)); }

	/**
	 * Creates a new shape redactor.
	 *
	 * @param axis   the identifier of the parametric axis to be redacted
	 * @param values the accepted values for the parametric {@code axis}; successfully evaluates guards on {@code axis}
	 *               if the guard accepts at least one of the values in the collection
	 *
	 * @throws NullPointerException if either {@code axis} or {@code values} is null or {@code values} contains a null
	 *                              value
	 */
	public Redactor(final String axis, final Collection<String> values) {

		this(axis, accepted -> !disjoint(accepted, values));

		if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

	}

	/**
	 * Creates a new shape redactor.
	 *
	 * @param axis      the identifier of the parametric axis to be redacted
	 * @param condition the condition for the parametric {@code axis}; successfully evaluates guards on {@code axis} if
	 *                  evaluating to {@code true} on the guard target values
	 *
	 * @throws NullPointerException if either {@code axis} or {@code condition} is null
	 */
	public Redactor(final String axis, final Predicate<Set<String>> condition) {

		if ( axis == null ) {
			throw new NullPointerException("null axis");
		}

		if ( condition == null ) {
			throw new NullPointerException("null condition");
		}

		this.axis=axis;
		this.condition=condition;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Shape probe(final Shape shape) { return shape; }


	@Override public Shape probe(final Guard guard) {
		return axis.equals(guard.getAxis())
				? condition.test(guard.getValues()) ? and() : or()
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
