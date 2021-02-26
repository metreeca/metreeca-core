/*
 * Copyright © 2013-2021 Metreeca srl
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

import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;

import static org.assertj.core.api.Assertions.assertThat;


final class AllTest {

	@Nested final class Optimization {

		@Test void testIgnoreEmptyValueSet() {
			assertThat(all()).isEqualTo(and());
		}

		@Test void testCollapseDuplicates() {
			assertThat(all(True, True, False)).isEqualTo(all(True, False));
		}

	}

	@Nested final class Probe {

		private final Value a=literal(1);
		private final Value b=literal(2);
		private final Value c=literal(3);

		@Test void testInspectAll() {
			assertThat(All.all(all(a, b, c)))
					.hasValueSatisfying(values -> assertThat(values).containsExactly(a, b, c));
		}

		@Test void testInspectAnd() {
			assertThat(All.all(and(all(a, b), all(b, c))))
					.hasValueSatisfying(values -> assertThat(values).containsExactly(a, b, c));
		}

		@Test void testInspectOtherShape() {
			assertThat(All.all(and()))
					.isEmpty();
		}

	}

}
