/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

import org.junit.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.things.Values.literal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AnyTest {

	@Test public void testInspectUniversal() {

		final Any any=any(Values.literal(1), Values.literal(2), Values.literal(3));

		assertTrue("defined", any(any)
				.filter(values -> values.equals(any.getValues()))
				.isPresent());
	}

	@Test public void testInspectConjunction() {

		final Any x=any(Values.literal(1), Values.literal(2), Values.literal(3));
		final Any y=any(Values.literal(2), Values.literal(3), Values.literal(4));

		assertTrue("all defined", any(and(x, y))
				.filter(values -> values.equals(Sets.intersection(x.getValues(), y.getValues())))
				.isPresent());

		assertTrue("some defined", any(and(x, And.and()))
				.filter(values -> values.equals(x.getValues()))
				.isPresent());

		assertFalse("none defined", any(and(And.and(), And.and()))
				.isPresent());

	}

	@Test public void testInspectOtherShape() {
		assertFalse("not defined", any(And.and()).isPresent());
	}

}
