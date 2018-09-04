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

package com.metreeca.form.probes;

import com.metreeca.form.shapes.*;
import com.metreeca.form.Shape;
import com.metreeca.form.Shift;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;

import java.util.List;

import static com.metreeca.form.shapes.Trait.trait;

import static java.util.stream.Collectors.toList;


/**
 * Shape pruner.
 *
 * <p>Recursively removes non-filtering constraints from a shape.</p>
 */
public final class Pruner extends Shape.Probe<Shape> {

	@Override protected Shape fallback(final Shape shape) {
		return And.and();
	}


	@Override public Shape visit(final Group group) {
		return group.getShape().accept(this);
	}

	@Override public Shape visit(final MinCount minCount) { return minCount; }

	@Override public Shape visit(final MaxCount maxCount) { return maxCount; }

	@Override public Shape visit(final Clazz clazz) { return clazz; }

	@Override public Shape visit(final Datatype datatype) { return datatype; }

	@Override public Shape visit(final All all) { return all; }

	@Override public Shape visit(final Any any) { return any; }

	@Override public Shape visit(final MinInclusive minInclusive) { return minInclusive; }

	@Override public Shape visit(final MaxInclusive maxInclusive) { return maxInclusive; }

	@Override public Shape visit(final MinExclusive minExclusive) { return minExclusive; }

	@Override public Shape visit(final MaxExclusive maxExclusive) { return maxExclusive; }

	@Override public Shape visit(final Pattern pattern) { return pattern; }

	@Override public Shape visit(final Like like) { return like; }

	@Override public Shape visit(final MinLength minLength) { return minLength; }

	@Override public Shape visit(final MaxLength maxLength) { return maxLength; }


	@Override public Shape visit(final Trait trait) {

		final Step step=trait.getStep();
		final Shape shape=trait.getShape().accept(this);

		return shape.equals(And.and()) ? And.and() : trait(step, shape);
	}

	@Override public Shape visit(final Virtual virtual) {

		final Trait trait=virtual.getTrait();
		final Shift shift=virtual.getShift();

		final Step step=trait.getStep();
		final Shape shape=trait.getShape().accept(this);

		return shape.equals(And.and()) ? And.and() : Virtual.virtual(trait(step, shape), shift);
	}


	@Override public Shape visit(final And and) {

		final List<Shape> shapes=and.getShapes().stream()
				.map(shape -> shape.accept(this))
				.filter(shape -> !shape.equals(And.and()))
				.collect(toList());

		return shapes.isEmpty() ? And.and() : And.and(shapes);
	}

	@Override public Shape visit(final Or or) {

		final List<Shape> shapes=or.getShapes().stream()
				.map(shape -> shape.accept(this))
				.filter(shape -> !shape.equals(And.and()))
				.collect(toList());

		return shapes.isEmpty() ? And.and() : Or.or(shapes);
	}

	@Override public Shape visit(final Test test) {
		return Test.test(test.getTest(), test.getPass().accept(this), test.getFail().accept(this));
	}

}
