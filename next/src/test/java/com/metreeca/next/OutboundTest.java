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

package com.metreeca.next;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


final class OutboundTest {

	@Test void testBodyPreventRepeatedUsage() {

		final TestOutbound outbound=new TestOutbound();

		assertThrows(IllegalStateException.class, () -> {
			outbound.body();
			outbound.body();
		});

	}

	@Test void testRepresentationCaching() {

		final TestOutbound outbound=new TestOutbound().text("text");

		assertEquals(
				outbound.text().orElseThrow(IllegalStateException::new),
				outbound.text().orElseThrow(IllegalStateException::new)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestOutbound extends Outbound<TestOutbound> {

		@Override protected TestOutbound self() { return this; }

	}

}
