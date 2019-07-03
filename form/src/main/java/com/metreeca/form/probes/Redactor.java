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

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.HashSet;
import java.util.Objects;
import java.util.Set;

import static com.metreeca.form.probes.Evaluator.fail;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Sets.set;

import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toList;


/**
 * Shape redactor.
 *
 * <p>Recursively evaluates {@linkplain Guard parametric} constraints in a shape.</p>
 */
public final class Redactor extends Traverser<Shape> {

	private final IRI axis;
	private final Set<Value> values;


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
	public Redactor(final IRI axis, final Value... values) { this(axis, set(values)); }

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
	public Redactor(final IRI axis, final Set<? extends Value> values) {

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

		final Set<? extends Value> accepted=guard.getValues();

		return axis.equals(guard.getAxis())
				? values.isEmpty() || !disjoint(accepted, values) ? pass() : fail()
				: guard; // ignore unrelated variables
	}


	@Override public Shape probe(final Field field) {
		return field(field.getIRI(), field.getShape().map(this));
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
