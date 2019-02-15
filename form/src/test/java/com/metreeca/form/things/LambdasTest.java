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

package com.metreeca.form.things;

import org.junit.jupiter.api.Test;

import java.util.function.Function;

import static com.metreeca.form.things.Lambdas.memoize;

import static org.assertj.core.api.Assertions.assertThat;


final class LambdasTest {

	@Test void testMemoizeMappers() {

		final Function<Object, Object> mapper=key -> new Object();
		final Function<Object, Object> memoized=memoize(mapper);

		assertThat(mapper.apply("key")).isNotSameAs(mapper.apply("key"));
		assertThat(memoized.apply("key")).isSameAs(memoized.apply("key"));

	}

}
