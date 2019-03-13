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

package com.metreeca.form.shapes;

import com.metreeca.form.Shape;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Memoizing.memoizing;
import static com.metreeca.form.shapes.Memoizing.memoizable;

import static org.assertj.core.api.Assertions.assertThat;


final class MemoizingTest {

	@Test void testHandleMemoizableMappers() {

		final Function<Shape, Object> mapper=shape -> new Object();
		final Function<Shape, Object> memoer=memoizable(mapper);

		final Shape memo=memoizing(and());

		assertThat(memo.map(memoer)).isSameAs(memo.map(memoer));

	}

	@Test void testIgnorePlainMappers() {

		final Function<Shape, Object> mapper=shape -> new Object();

		final Shape memo=memoizing(and());

		assertThat(memo.map(mapper)).isNotSameAs(memo.map(mapper));

	}

}
