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

package com.metreeca.json.shifts;

import com.metreeca.json.Shift;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static com.metreeca.json.Values.term;
import static com.metreeca.json.shifts.Alt.alt;
import static com.metreeca.json.shifts.Step.step;

import static org.assertj.core.api.Assertions.assertThat;

final class AltTest {

	private static final Path p=step(term("p"));
	private static final Path q=step(term("q"));


	@Nested final class Optimization {

		@Test void testUnwrapSingletons() {
			assertThat(alt(p)).isEqualTo(p);
		}

		@Test void testPreserveOrder() {
			assertThat(alt(p, q).map(new Shift.Probe<Set<Path>>() {

				@Override public Set<Path> probe(final Alt alt) { return alt.paths(); }

			})).containsExactly(p, q);
		}

		@Test void testCollapseDuplicates() {

			assertThat(alt(p, p)).isEqualTo(p);

			assertThat(alt(p, p, q).map(new Shift.Probe<Set<Path>>() {

				@Override public Set<Path> probe(final Alt alt) { return alt.paths(); }

			})).containsExactly(p, q);
		}

	}

}