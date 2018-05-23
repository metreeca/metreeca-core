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

import static com.metreeca.spec.shapes.MaxCount.maxCount;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.lang.Integer.max;
import static java.lang.Integer.min;


public class MaxCountTest {

	@org.junit.Test public void testInspectMaxCount() {

		final MaxCount count=maxCount(10);

		assertTrue("defined", maxCount(count)
				.filter(limit -> limit.equals(count.getLimit()))
				.isPresent());
	}

	@org.junit.Test public void testInspectConjunction() {

		final MaxCount x=maxCount(10);
		final MaxCount y=maxCount(100);

		assertTrue("all defined", maxCount(And.and(x, y))
				.filter(limit -> limit.equals(min(x.getLimit(), y.getLimit())))
				.isPresent());

		assertTrue("some defined", maxCount(And.and(x, And.and()))
				.filter(limit -> limit.equals(x.getLimit()))
				.isPresent());

		assertFalse("none defined", maxCount(And.and(And.and(), And.and()))
				.isPresent());

	}

	@org.junit.Test public void testInspectDisjunction() {

		final MaxCount x=maxCount(10);
		final MaxCount y=maxCount(100);

		assertTrue("all defined", maxCount(Or.or(x, y))
				.filter(limit -> limit.equals(max(x.getLimit(), y.getLimit())))
				.isPresent());

		assertTrue("some defined", maxCount(Or.or(x, And.and()))
				.filter(limit -> limit.equals(x.getLimit()))
				.isPresent());

		assertFalse("none defined", maxCount(Or.or(And.and(), And.and()))
				.isPresent());

	}

	@org.junit.Test public void testOption() {

		final MaxCount x=maxCount(10);
		final MaxCount y=maxCount(100);

		assertTrue("all defined", maxCount(Test.test(And.and(), x, y))
				.filter(limit -> limit.equals(max(x.getLimit(), y.getLimit())))
				.isPresent());

		assertTrue("some defined", maxCount(Test.test(And.and(), x, And.and()))
				.filter(limit -> limit.equals(x.getLimit()))
				.isPresent());

		assertFalse("none defined", maxCount(Test.test(And.and(), And.and(), And.and()))
				.isPresent());

	}

	@org.junit.Test public void testInspectOtherShape() {
		assertFalse("not defined", maxCount(And.and()).isPresent());
	}

}
