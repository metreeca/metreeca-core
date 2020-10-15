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

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;
import static org.assertj.core.api.Assertions.assertThat;


final class WhenTest {

	@Nested final class Optimization {

		@Test void testOptimizeConstantPassTest() {
			assertThat((when(and(), alias("pass"), alias("fail")))).isEqualTo(alias("pass"));
		}

		@Test void testOptimizeConstantFailTest() {
			assertThat((when(or(), alias("pass"), alias("fail")))).isEqualTo(alias("fail"));
		}

		@Test void testCollapseIdenticatBranchesConstantFailTest() {
			assertThat((when(or(), alias("same"), alias("same")))).isEqualTo(alias("same"));
		}

	}

}
