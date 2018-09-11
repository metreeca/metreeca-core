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

import java.io.Reader;
import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;


final class InboundTest {

	@Test void testBodyPreventRepeatedUsage() {

		final TestInbound inbound=new TestInbound();

		assertThrows(IllegalStateException.class, () -> {
			inbound.body();
			inbound.body();
		});

	}

	@Test void testRepresentationCaching() {

		final TestInbound inbound=new TestInbound().body(() -> new Source() {

			@Override public Reader reader() { return new StringReader("test"); }

		});

		assertEquals(
				inbound.text().orElseThrow(IllegalStateException::new),
				inbound.text().orElseThrow(IllegalStateException::new)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestInbound extends Inbound<TestInbound> {

		@Override protected TestInbound self() { return this; }

	}

}
