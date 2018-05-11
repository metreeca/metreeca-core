/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec.shapes;

import org.junit.Test;

import static com.metreeca.jeep.Jeep.intersection;
import static com.metreeca.spec.Values.literal;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Any.any;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AnyTest {

	@Test public void testInspectUniversal() {

		final Any any=any(literal(1), literal(2), literal(3));

		assertTrue("defined", any(any)
				.filter(values -> values.equals(any.getValues()))
				.isPresent());
	}

	@Test public void testInspectConjunction() {

		final Any x=any(literal(1), literal(2), literal(3));
		final Any y=any(literal(2), literal(3), literal(4));

		assertTrue("all defined", any(and(x, y))
				.filter(values -> values.equals(intersection(x.getValues(), y.getValues())))
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
