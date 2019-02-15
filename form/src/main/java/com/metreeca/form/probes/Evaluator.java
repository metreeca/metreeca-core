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
import static com.metreeca.form.shapes.Or.or;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


/**
 * Shape evaluator.
 *
 * <p>Tests if a shape is a constant, ignoring {@linkplain Meta metadata} annotations.</p>
 */
public final class Evaluator extends Traverser<Boolean> {

	private static final Shape pass=and();
	private static final Shape fail=or();

	private static final Shape.Probe<Boolean> instance=new Evaluator();


	/**
	 * Tests if a shape is empty.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true}, if {@code shape} evaluates either to an {@linkplain #pass() empty conjunction} or to an
	 * {@linkplain #pass() empty disjunction} ignoring {@linkplain Meta metadata} annotations; {@code false}, otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static boolean empty(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.map(instance) != null;
	}


	/**
	 * Creates a shape that is always matched.
	 *
	 * @return a empty {@linkplain And conjunction}.
	 */
	public static Shape pass() { return pass; }

	/**
	 * Tests if a shape is always matched.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true}, if {@code shape} evaluates to an {@linkplain #pass() empty conjunction} ignoring
	 * {@linkplain Meta metadata} annotations; {@code false}, otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static boolean pass(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return TRUE.equals(shape.map(instance));
	}


	/**
	 * Creates a shape that is never matched.
	 *
	 * @return an empty {@linkplain Or disjunction}.
	 */
	public static Shape fail() { return fail; }

	/**
	 * Tests if a shape is never matched.
	 *
	 * @param shape the shape to be tested
	 *
	 * @return {@code true}, if {@code shape} evaluates to an {@linkplain #fail() empty disjunction} ignoring
	 * {@linkplain Meta metadata} annotations; {@code false}, otherwise
	 *
	 * @throws NullPointerException if {@code shape} is null
	 */
	public static boolean fail(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return FALSE.equals(shape.map(instance));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Boolean probe(final Meta meta) {
		return true;
	}


	@Override public Boolean probe(final Field field) {
		return null;
	}

	@Override public Boolean probe(final And and) {
		return and.getShapes().stream()
				.filter(shape -> !(shape instanceof Meta))
				.map(shape -> shape.map(this))
				.reduce(true, (x, y) -> x == null || y == null ? null : x && y);
	}

	@Override public Boolean probe(final Or or) {
		return or.getShapes().stream()
				.filter(shape -> !(shape instanceof Meta))
				.map(shape -> shape.map(this))
				.reduce(false, (x, y) -> x == null || y == null ? null : x || y);
	}

	@Override public Boolean probe(final When when) {

		final Boolean test=when.getTest().map(this);
		final Boolean pass=when.getPass().map(this);
		final Boolean fail=when.getFail().map(this);

		return TRUE.equals(test) ? pass
				: FALSE.equals(test) ? fail
				: TRUE.equals(pass) && TRUE.equals(fail) ? TRUE
				: FALSE.equals(pass) && FALSE.equals(fail) ? FALSE
				: null;
	}

}
