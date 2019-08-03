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

package com.metreeca.gae.services;

import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Traverser;
import com.metreeca.tree.shapes.*;

import java.util.Collection;
import java.util.function.Function;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static java.util.stream.Collectors.toList;


/**
 * Shape splitter.
 *
 * <p>Splits combo container/resource shapes.</p>
 */
final class Splitter implements Function<Shape, Shape> { // factor with Shapes in base branch

	private final boolean traverse;
	private final String contains;


	/**
	 * Creates a shape splitter
	 *
	 * @param traverse if {@code true}, traverse {@code contains} field to extract resource shape; otherwise, ignore
	 *                 {@code contains} fields to extract container shape
	 *
	 * @param contains the name of the container field linking nested resource shapes
	 * @throws NullPointerException if {@code contains} is null
	 */
	public Splitter(final boolean traverse, final String contains) {

		if ( contains == null ) {
			throw new NullPointerException("null contains");
		}

		this.traverse=traverse;
		this.contains=contains;
	}


	@Override public Shape apply(final Shape shape) {

		final Shape split=shape
				.map(new SplitterTraverser())
				.map(new Optimizer());

		return split.equals(and()) ? shape : split;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class SplitterTraverser extends Traverser<Shape> {

		@Override public Shape probe(final Shape shape) {
			return traverse ? and() : shape;
		}


		@Override public Shape probe(final Field field) {
			return field.getName().equals(contains)
					? traverse ? field.getShape() : and()
					: traverse ? and() : field.getShape();
		}

		@Override public Shape probe(final And and) {

			final Collection<Shape> shapes=and.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(shape -> !shape.equals(and()))
					.collect(toList());

			return and(shapes);
		}

		@Override public Shape probe(final Or or) {

			final Collection<Shape> shapes=or.getShapes().stream()
					.map(shape -> shape.map(this))
					.filter(shape -> !shape.equals(and()))
					.collect(toList());

			return shapes.isEmpty() ? and() : or(shapes);
		}

		@Override public Shape probe(final When when) {
			return when(
					when.getTest().map(this),
					when.getPass().map(this),
					when.getFail().map(this)
			);
		}

	}

}
