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

package com.metreeca.form.shapes;

import com.metreeca.form.things.Sets;
import com.metreeca.form.things.Values;

import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.All.all;

import static org.assertj.core.api.Assertions.assertThat;


final class AllTest {

	@Test void testInspectExistential() {

		final All all=All.all(Values.literal(1), Values.literal(2), Values.literal(3));

		assertThat(all(all)
				.filter(values -> values.equals(all.getValues()))
				.isPresent()).as("defined").isTrue();
	}

	@Test void testInspectConjunction() {

		final All x=All.all(Values.literal(1), Values.literal(2), Values.literal(3));
		final All y=All.all(Values.literal(2), Values.literal(3), Values.literal(4));

		assertThat(all(And.and(x, y))
				.filter(values1 -> values1.equals(Sets.union(x.getValues(), y.getValues())))
				.isPresent()).as("all defined").isTrue();

		assertThat(all(And.and(x, And.and()))
				.filter(values -> values.equals(x.getValues()))
				.isPresent()).as("some defined").isTrue();

		assertThat(all(And.and(And.and(), And.and()))
				.isPresent()).as("none defined").isFalse();

	}

	@Test void testInspectOtherShape() {
		assertThat(all(And.and()).isPresent()).as("not defined").isFalse();
	}

}
