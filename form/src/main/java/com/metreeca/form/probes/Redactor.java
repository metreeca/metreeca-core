/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form.probes;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Option.option;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Trait.trait;

import static java.util.Collections.disjoint;
import static java.util.stream.Collectors.toList;


/**
 * Shape redactor.
 *
 * <p>Recursively evaluates {@linkplain When conditional} section in a shape.</p>
 */
public final class Redactor extends Traverser<Shape> {

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


	@Override public Shape probe(final Shape shape) { return shape; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Shape probe(final When when) {

		final Set<? extends Value> actual=variables.get(when.getIRI());
		final Set<? extends Value> accepted=when.getValues();

		return actual == null ? when // ignore undefined variables
				: actual.contains(Form.any) || !disjoint(accepted, actual) ? and()
				: or();
	}


	@Override public Shape probe(final Trait trait) {
		return trait(trait.getIRI(), trait.getShape().map(this));
	}


	@Override public Shape probe(final And and) {
		return and(and.getShapes().stream().map(shape -> shape.map(this)).collect(toList()));
	}

	@Override public Shape probe(final Or or) {
		return or(or.getShapes().stream().map(shape -> shape.map(this)).collect(toList()));
	}

	@Override public Shape probe(final Option option) {
		return option(
				option.getTest().map(this),
				option.getPass().map(this),
				option.getFail().map(this)
		);
	}

}
