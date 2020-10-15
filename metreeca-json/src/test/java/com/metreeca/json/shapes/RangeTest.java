/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.json.shapes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.False;
import static com.metreeca.json.Values.True;
import static com.metreeca.json.shapes.Or.or;
import static org.assertj.core.api.Assertions.assertThat;

final class RangeTest {

	@Nested final class Optimization {

		@Test void testIgnoreEmptyValueSet() {
			assertThat(Range.range()).isEqualTo(or());
		}

		@Test void testCollapseDuplicates() {
			assertThat(Range.range(True, True, False)).isEqualTo(Range.range(True, False));
		}

	}

}