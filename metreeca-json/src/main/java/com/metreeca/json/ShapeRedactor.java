/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.json;

import com.metreeca.json.shapes.*;

import java.util.Arrays;
import java.util.Objects;
import java.util.function.Function;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;


final class ShapeRedactor extends Shape.Probe<Shape> {

	private final Function<Guard, Boolean>[] evaluators;


	@SafeVarargs ShapeRedactor(final Function<Guard, Boolean>... evaluators) {
		this.evaluators=evaluators;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Shape probe(final Shape shape) {
		return shape;
	}

	@Override public Shape probe(final Guard guard) {

		final Boolean include=Arrays
				.stream(evaluators)
				.map(redactor -> redactor.apply(guard))
				.filter(Objects::nonNull)
				.findFirst()
				.orElse(null);

		return Boolean.TRUE.equals(include) ? and()
				: Boolean.FALSE.equals(include) ? or()
				: guard;
	}

	@Override public Shape probe(final Field field) {
		return field(field.name(), field.shape().map(this));
	}


	@Override public Shape probe(final And and) {
		return and(and.shapes().stream().map(this));
	}

	@Override public Shape probe(final Or or) {
		return or(or.shapes().stream().map(this));
	}

	@Override public Shape probe(final When when) {
		return when(when.test().map(this), when.pass().map(this), when.fail().map(this));
	}

}
