/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form;

import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Shape.constant;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Meta.label;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


final class ShapeTest {

	@Nested final class Constatnt {

		private final Shape pass=and();
		private final Shape fail=or();
		private final Shape maybe=field(RDFS.LABEL);


		@Test void testField() {
			assertThat(constant(and(maybe))).as("maybe").isNull();
		}

		@Test void testConjunction() {
			assertThat(constant(pass)).as("always pass").isTrue();
			assertThat(constant(and(pass))).as("evaluate nested shapes").isTrue();
			assertThat(constant(and(label("label")))).as("ignore metadata").isTrue();
		}

		@Test void testDisjunction  () {
			assertThat(constant(fail)).as("always fail").isFalse();
			assertThat(constant(or(fail))).as("evaluate nested shapes").isFalse();
			assertThat(constant(or(label("label")))).as("ignore metadata").isFalse();
		}

		@Test void testCondition() {

			assertThat(constant(when(pass, pass, fail))).as("always pass").isTrue();
			assertThat(constant(when(fail, fail, fail))).as("always fail").isFalse();

			assertThat(constant(when(maybe, pass, pass))).as("always pass").isTrue();
			assertThat(constant(when(maybe, fail, fail))).as("always fail").isFalse();

			assertThat(constant(when(maybe, maybe, pass))).as("maybe").isNull();

		}

	}

}
