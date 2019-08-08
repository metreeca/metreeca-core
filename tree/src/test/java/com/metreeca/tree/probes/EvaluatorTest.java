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

package com.metreeca.tree.probes;

import com.metreeca.tree.Shape;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.tree.probes.Evaluator.fail;
import static com.metreeca.tree.probes.Evaluator.pass;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Meta.label;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


@Nested final class EvaluatorTest {

	private final Shape pass=Evaluator.pass();
	private final Shape fail=fail();
	private final Shape maybe=field("label");


	@Test void testField() {
		assertThat(pass(and(maybe))).as("maybe").isFalse();
		assertThat(fail(and(maybe))).as("maybe").isFalse();
	}

	@Test void testConjunction() {
		assertThat(pass(pass)).as("always pass").isTrue();
		assertThat(pass(and(pass))).as("evaluate nested shapes").isTrue();
		assertThat(pass(and(label("label")))).as("ignore metadata").isTrue();
	}

	@Test void testDisjunction  () {
		assertThat(fail(fail)).as("always fail").isTrue();
		assertThat(fail(or(fail))).as("evaluate nested shapes").isTrue();
		assertThat(fail(or(label("label")))).as("ignore metadata").isTrue();
	}

	@Test void testCondition() {

		assertThat(pass(when(pass, pass, fail))).as("always pass").isTrue();
		assertThat(fail(when(fail, fail, fail))).as("always fail").isTrue();

		assertThat(pass(when(maybe, pass, pass))).as("always pass").isTrue();
		assertThat(fail(when(maybe, fail, fail))).as("always fail").isTrue();

		assertThat(pass(when(maybe, maybe, pass))).as("maybe").isFalse();
		assertThat(fail(when(maybe, fail, maybe))).as("maybe").isFalse();

	}

}
