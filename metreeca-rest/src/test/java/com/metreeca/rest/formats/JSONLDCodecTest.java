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

package com.metreeca.rest.formats;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Guard.detail;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rest.formats.JSONLDCodec.maxCount;
import static com.metreeca.rest.formats.JSONLDCodec.minCount;
import static org.assertj.core.api.Assertions.assertThat;

final class JSONLDCodecTest {

	@Nested final class MinCountProbe {

		@Test void testInspectMinCount() {
			assertThat(minCount(minCount(10))).contains(10);
		}

		@Test void testInspectAnd() {
			assertThat(minCount(and(minCount(10), minCount(100)))).contains(100);
		}

		@Test void testInspectDisjunction() {
			assertThat(minCount(or(minCount(10), minCount(100)))).contains(10);
		}

		@Test void testOption() {
			assertThat(minCount(when(detail(), minCount(10), minCount(100)))).contains(10);
			assertThat(minCount(when(detail(), minCount(10), and()))).contains(10);
			assertThat(minCount(when(detail(), and(), and()))).isEmpty();
		}

		@Test void testInspectOtherShape() {
			assertThat(minCount(and())).isEmpty();
		}

	}

	static final class MaxCountProbe {

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
}