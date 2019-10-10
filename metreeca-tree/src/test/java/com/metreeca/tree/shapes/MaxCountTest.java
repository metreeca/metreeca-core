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

package com.metreeca.tree.shapes;

import org.junit.jupiter.api.Test;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


final class MaxCountTest {

	@Test void testInspectMaxCount() {

		final MaxCount count=maxCount(10);

		assertThat(maxCount(count))
				.contains(count.getLimit());
	}

	@Test void testInspectConjunction() {

		final MaxCount x=maxCount(10);
		final MaxCount y=maxCount(100);

		assertThat(maxCount(and(x, y)))
				.contains(10);

		assertThat(maxCount(and(x, and())))
				.contains(10);

		assertThat(maxCount(and(and(), and())))
				.isEmpty();

	}

	@Test void testInspectDisjunction() {

		final MaxCount x=maxCount(10);
		final MaxCount y=maxCount(100);

		assertThat(maxCount(or(x, y)))
				.contains(100);

		assertThat(maxCount(or(x, and())))
				.contains(10);

		assertThat(maxCount(or(and(), and())))
				.isEmpty();

	}

	@Test void testOption() {

		final MaxCount x=maxCount(10);
		final MaxCount y=maxCount(100);

		assertThat(maxCount(when(and(), x, y)))
				.contains(100);

		assertThat(maxCount(when(and(), x, and())))
				.contains(10);

		assertThat(maxCount(when(and(), and(), and())))
				.isEmpty();

	}

	@Test void testInspectOtherShape() {
		assertThat(maxCount(and()))
				.isEmpty();
	}

}
