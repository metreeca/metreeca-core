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

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;

import static java.util.stream.Collectors.toList;


/**
 * Shape cleaner.
 *
 * <p>Recursively removes {@linkplain Meta meta} annotations from a shape.</p>
 */
public final class Cleaner extends Traverser<Shape> {

	@Override public Shape probe(final Shape shape) { return shape; }


	@Override public Shape probe(final Meta meta) { return and(); }


	@Override public Shape probe(final Field field) {
		return field(field.getIRI(), field.getShape().map(this));
	}


	@Override public Shape probe(final And and) {
		return and(and.getShapes().stream().map(s -> s.map(this)).collect(toList()));
	}

	@Override public Shape probe(final Or or) {
		return or(or.getShapes().stream().map(s -> s.map(this)).collect(toList()));
	}

	@Override public Shape probe(final When when) {
		return when(
				when.getTest().map(this),
				when.getPass().map(this),
				when.getFail().map(this)
		);
	}

}
