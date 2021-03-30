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

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.json.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


final class WhenTest {

	// different shape type to prevent collapsing by optimizers

	private static final Shape pass=datatype(RDF.NIL);
	private static final Shape fail=clazz(RDF.NIL);

	@Nested final class Optimization {

		@Test void testOptimizeConstantPassTest() {
			assertThat((when(and(), pass, fail))).isEqualTo(pass);
		}

		@Test void testOptimizeConstantFailTest() {
			assertThat((when(or(), pass, fail))).isEqualTo(fail);
		}

		@Test void testCollapseIdenticatBranchesConstantFailTest() {
			assertThat((when(pattern("me"), pass, pass))).isEqualTo(pass);
		}

	}

}
