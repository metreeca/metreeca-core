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

package com.metreeca.jeep;

import org.junit.Test;

import static com.metreeca.jeep.Comparables.compare;

import static org.junit.Assert.assertEquals;


public class ComparablesTest {

	private enum Ordered {Lower, Upper}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testCompare() {

		assertEquals("=", 0, compare(Ordered.Upper, Ordered.Upper));
		assertEquals("<", -1, compare(Ordered.Lower, Ordered.Upper));
		assertEquals(">", +1, compare(Ordered.Upper, Ordered.Lower));

		assertEquals("=/null", 0, compare(null, null));
		assertEquals("</null", -1, compare(null, Ordered.Upper));
		assertEquals(">/null", +1, compare(Ordered.Upper, null));

	}

}
