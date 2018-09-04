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

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.lang.Integer.max;
import static java.lang.Integer.min;


public class MinCountTest {

	@org.junit.Test public void testInspectMinCount() {

		final MinCount count=MinCount.minCount(10);

		assertTrue("defined", MinCount.minCount(count)
				.filter(limit -> limit.equals(count.getLimit()))
				.isPresent());
	}

	@org.junit.Test public void testInspectConjunction() {

		final MinCount x=MinCount.minCount(10);
		final MinCount y=MinCount.minCount(100);

		assertTrue("all defined", MinCount.minCount(And.and(x, y))
				.filter(limit -> limit.equals(max(x.getLimit(), y.getLimit())))
				.isPresent());

		assertTrue("some defined", MinCount.minCount(And.and(x, And.and()))
				.filter(limit -> limit.equals(x.getLimit()))
				.isPresent());

		assertFalse("none defined", MinCount.minCount(And.and(And.and(), And.and()))
				.isPresent());

	}

	@org.junit.Test public void testInspectDisjunction() {

		final MinCount x=MinCount.minCount(10);
		final MinCount y=MinCount.minCount(100);

		assertTrue("all defined", MinCount.minCount(Or.or(x, y))
				.filter(limit -> limit.equals(min(x.getLimit(), y.getLimit())))
				.isPresent());

		assertTrue("some defined", MinCount.minCount(Or.or(x, And.and()))
				.filter(limit -> limit.equals(x.getLimit()))
				.isPresent());

		assertFalse("none defined", MinCount.minCount(Or.or(And.and(), And.and()))
				.isPresent());

	}

	@org.junit.Test public void testOption() {

		final MinCount x=MinCount.minCount(10);
		final MinCount y=MinCount.minCount(100);

		assertTrue("all defined", MinCount.minCount(Test.test(And.and(), x, y))
				.filter(limit -> limit.equals(min(x.getLimit(), y.getLimit())))
				.isPresent());

		assertTrue("some defined", MinCount.minCount(Test.test(And.and(), x, And.and()))
				.filter(limit -> limit.equals(x.getLimit()))
				.isPresent());

		assertFalse("none defined", MinCount.minCount(Test.test(And.and(), And.and(), And.and()))
				.isPresent());

	}

	@org.junit.Test public void testInspectOtherShape() {
		assertFalse("not defined", MinCount.minCount(And.and()).isPresent());
	}

}
