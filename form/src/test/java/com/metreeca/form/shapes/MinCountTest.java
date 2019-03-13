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

package com.metreeca.form.shapes;

import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;

import static java.lang.Integer.max;
import static java.lang.Integer.min;


final class MinCountTest {

	@Test void testInspectMinCount() {

		final MinCount count=MinCount.minCount(10);

		assertThat(MinCount.minCount(count)
				.filter(limit -> limit.equals(count.getLimit()))
				.isPresent()).as("defined").isTrue();
	}

	@Test void testInspectConjunction() {

		final MinCount x=MinCount.minCount(10);
		final MinCount y=MinCount.minCount(100);

		assertThat(MinCount.minCount(and(x, y))
				.filter(limit1 -> limit1.equals(max(x.getLimit(), y.getLimit())))
				.isPresent()).as("all defined").isTrue();

		assertThat(MinCount.minCount(and(x, and()))
				.filter(limit -> limit.equals(x.getLimit()))
				.isPresent()).as("some defined").isTrue();

		assertThat(MinCount.minCount(and(and(), and()))
				.isPresent()).as("none defined").isFalse();

	}

	@Test void testInspectDisjunction() {

		final MinCount x=MinCount.minCount(10);
		final MinCount y=MinCount.minCount(100);

		assertThat(MinCount.minCount(or(x, y))
				.filter(limit1 -> limit1.equals(min(x.getLimit(), y.getLimit())))
				.isPresent()).as("all defined").isTrue();

		assertThat(MinCount.minCount(or(x, and()))
				.filter(limit -> limit.equals(x.getLimit()))
				.isPresent()).as("some defined").isTrue();

		assertThat(MinCount.minCount(or(and(), and()))
				.isPresent()).as("none defined").isFalse();

	}

	@Test void testOption() {

		final MinCount x=MinCount.minCount(10);
		final MinCount y=MinCount.minCount(100);

		assertThat(MinCount.minCount(when(and(), x, y))
				.filter(limit1 -> limit1.equals(min(x.getLimit(), y.getLimit())))
				.isPresent()).as("all defined").isTrue();

		assertThat(MinCount.minCount(when(and(), x, and()))
				.filter(limit -> limit.equals(x.getLimit()))
				.isPresent()).as("some defined").isTrue();

		assertThat(MinCount.minCount(when(and(), and(), and()))
				.isPresent()).as("none defined").isFalse();

	}

	@Test void testInspectOtherShape() {
		assertThat(MinCount.minCount(and()).isPresent()).as("not defined").isFalse();
	}

}
