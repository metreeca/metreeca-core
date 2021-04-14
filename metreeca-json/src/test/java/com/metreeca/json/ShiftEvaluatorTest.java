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

package com.metreeca.json;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.Set;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shifts.Alt.alt;
import static com.metreeca.json.shifts.Seq.seq;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;

final class ShiftEvaluatorTest {

	private static final IRI x=item("x");
	private static final IRI y=item("y");
	private static final IRI z=item("z");

	private static final IRI p=term("p");
	private static final IRI q=term("q");


	private Set<Value> apply(final Shift shift) {
		return shift.map(new ShiftEvaluator(frame(x, asList(

				statement(x, p, y),
				statement(y, p, literal(1)),
				statement(y, q, literal(2)),

				statement(x, q, z),
				statement(z, p, literal(3)),
				statement(z, q, literal(4))

		)))).map(Frame::focus).collect(toCollection(LinkedHashSet::new));
	}


	@Test void testSeq() {
		assertThat(apply(seq(p, q))).containsExactly(literal(2));
	}

	@Test void testAlt() {
		assertThat(apply(alt(p, q))).containsExactly(y, z);
	}

}