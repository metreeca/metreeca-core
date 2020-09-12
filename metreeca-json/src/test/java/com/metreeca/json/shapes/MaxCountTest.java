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

package com.metreeca.json.shapes;

import org.junit.jupiter.api.Test;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Guard.detail;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static org.assertj.core.api.Assertions.assertThat;


final class MaxCountTest {

	@Test void testInspectMaxCount() {
		assertThat(maxCount(maxCount(10))).contains(10);
	}

	@Test void testInspectAnd() {
		assertThat(maxCount(and(maxCount(10), maxCount(100)))).contains(10);
	}

	@Test void testInspectOr() {
		assertThat(maxCount(or(maxCount(10), maxCount(100)))).contains(100);
	}

	@Test void testOption() {
		assertThat(maxCount(when(detail(), maxCount(10), maxCount(100)))).contains(100);
		assertThat(maxCount(when(detail(), maxCount(10), and()))).contains(10);
		assertThat(maxCount(when(detail(), and(), and()))).isEmpty();
	}

	@Test void testInspectOtherShape() {
		assertThat(maxCount(and())).isEmpty();
	}

}
