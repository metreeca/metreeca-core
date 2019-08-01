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

import java.util.List;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.util.stream.Collectors.toList;


/**
 * Shape pruner.
 *
 * <p>Recursively removes non-filtering constraints from a shape.</p>
 */
public final class Pruner extends Traverser<Shape> {

	@Override public Shape probe(final Shape shape) {
		return shape;
	}


	@Override public Shape probe(final Meta meta) { return and(); }

	@Override public Shape probe(final Guard guard) { return and(); }


	@Override public Shape probe(final Field field) {

		final String name=field.getName();
		final Shape shape=field.getShape().map(this);

		return shape.equals(and()) ? and() : field(name, shape);
	}


	@Override public Shape probe(final And and) {

		final List<Shape> shapes=and.getShapes().stream()
				.map(shape -> shape.map(this))
				.filter(shape -> !shape.equals(and()))
				.collect(toList());

		return shapes.isEmpty() ? and() : and(shapes);
	}

	@Override public Shape probe(final Or or) {

		final List<Shape> shapes=or.getShapes().stream()
				.map(shape -> shape.map(this))
				.filter(shape -> !shape.equals(and()))
				.collect(toList());

		return shapes.isEmpty() ? and() : or(shapes);
	}

	@Override public Shape probe(final When when) {
		return when(
				when.getTest(),
				when.getPass().map(this),
				when.getFail().map(this)
		);
	}

}
