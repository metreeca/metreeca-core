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

package com.metreeca.spec.probes;

import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Group.group;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.Virtual.virtual;

import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toList;


/**
 * Shape redactor.
 *
 * <p>Recursively evaluates and replace {@linkplain When conditional} shapes from a shape.</p>
 */
public final class Redactor extends Shape.Probe<Shape> {

	private final Map<IRI, Set<? extends Value>> variables;


	public Redactor(final Map<IRI, Set<? extends Value>> variables) {

		if ( variables == null ) {
			throw new NullPointerException("null variables");
		}

		if ( variables.containsKey(null) ) {
			throw new NullPointerException("null variable");
		}

		if ( variables.containsValue(null) ) {
			throw new NullPointerException("null variable values");
		}

		this.variables=new LinkedHashMap<>(variables); // !!! clone map values
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected Shape fallback(final Shape shape) {
		return shape;
	}


	@Override public Shape visit(final Group group) {
		return group(group.getShape().accept(this));
	}

	@Override public Shape visit(final Trait trait) {
		return trait(trait.getStep(), trait.getShape().accept(this));
	}

	@Override public Shape visit(final Virtual virtual) {
		return virtual((Trait)virtual.getTrait().accept(this), virtual.getShift());
	}


	@Override public Shape visit(final And and) {
		return and(and.getShapes().stream().map(shape -> shape.accept(this)).collect(toList()));
	}

	@Override public Shape visit(final Or or) {
		return or(or.getShapes().stream().map(shape -> shape.accept(this)).collect(toList()));
	}

	@Override public Shape visit(final Test test) {
		return test(
				test.getTest().accept(this),
				test.getPass().accept(this),
				test.getFail().accept(this)
		);
	}

	@Override public Shape visit(final When when) {

		final Set<? extends Value> actual=variables.get(when.getIRI());
		final Set<? extends Value> accepted=when.getValues();

		return actual == null ? when // ignore undefined variables
				: !disjoint(accepted, actual) || actual.contains(Spec.any) ? and()
				: or();
	}

}
