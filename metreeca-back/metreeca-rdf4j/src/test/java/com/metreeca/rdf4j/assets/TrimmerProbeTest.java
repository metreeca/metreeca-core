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

package com.metreeca.rdf4j.assets;

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.Like.like;

import static org.assertj.core.api.Assertions.assertThat;


final class TrimmerProbeTest {

	private static final IRI x=iri("test:x");

	private static final Shape f=like("filter");


	private static Shape trim(final Shape shape) {
		return shape.map(new TrimmerProbe(Mode, Filter));
	}


	@Test void testPrune() {
		assertThat(trim(f)).isEqualTo(and());
		assertThat(trim(field(x, f))).isEqualTo(and());
	}

	@Test void testRetainFilter() {
		assertThat(trim(filter(f))).isEqualTo(f);
		assertThat(trim(field(x, filter(f)))).isEqualTo(field(x, f));
	}

	@Test void testIgnoreOtherAxisValues() {
		assertThat(trim(convey(field(x)))).isEqualTo(and());
	}

}