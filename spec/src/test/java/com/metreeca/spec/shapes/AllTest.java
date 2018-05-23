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

package com.metreeca.spec.shapes;

import org.junit.Test;

import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.things.Sets.union;
import static com.metreeca.spec.things.Values.literal;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class AllTest {

	@Test public void testInspectExistential() {

		final All all=all(literal(1), literal(2), literal(3));

		assertTrue("defined", all(all)
				.filter(values -> values.equals(all.getValues()))
				.isPresent());
	}

	@Test public void testInspectConjunction() {

		final All x=all(literal(1), literal(2), literal(3));
		final All y=all(literal(2), literal(3), literal(4));

		assertTrue("all defined", all(And.and(x, y))
				.filter(values -> values.equals(union(x.getValues(), y.getValues())))
				.isPresent());

		assertTrue("some defined", all(And.and(x, And.and()))
				.filter(values -> values.equals(x.getValues()))
				.isPresent());

		assertFalse("none defined", all(And.and(And.and(), And.and()))
				.isPresent());

	}

	@Test public void testInspectOtherShape() {
		assertFalse("not defined", all(And.and()).isPresent());
	}

}
