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

import com.metreeca.next.formats.Text;

import org.junit.jupiter.api.Test;

import static com.metreeca.form.things.Lists.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import static java.util.Collections.emptySet;


final class MessageTest {

	@Test void testHeadersNormalizeHeadreNames() {

		final TestMessage message=new TestMessage()
				.headers("test-header", "value");

		assertTrue(message.headers().keySet().contains("Test-Header"));
	}

	@Test void testHeadersIgnoreEmptyHeaders() {

		final TestMessage message=new TestMessage()
				.headers("test-header", emptySet());

		assertTrue(message.headers().entrySet().isEmpty());
	}

	@Test void testHeadersOverwritesValues() {

		final TestMessage message=new TestMessage()
				.header("test-header", "one")
				.header("test-header", "two");

		assertEquals(list("two"), list(message.headers("test-header")));
	}

	@Test void testHeadersAppendsValues() {

		final TestMessage message=new TestMessage()
				.header("+test-header", "one")
				.header("+test-header", "two");

		assertEquals(list("one", "two"), list(message.headers("test-header")));
	}

	@Test void testHeadersAppendsCookies() {

		final TestMessage message=new TestMessage()
				.header("set-cookie", "one")
				.header("set-cookie", "two");

		assertEquals(list("one", "two"), list(message.headers("set-cookie")));
	}

	@Test void testHeadersIgnoresEmptyAndDuplicateValues() {

		final TestMessage message=new TestMessage()
				.headers("test-header", "one", "two", "", "two");

		assertEquals(list("one", "two"), list(message.headers("test-header")));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRepresentationCaching() {

		final TestMessage outbound=new TestMessage().body(Text.Format, "text");

		assertEquals(
				outbound.body(Text.Format).value().orElseThrow(IllegalStateException::new),
				outbound.body(Text.Format).value().orElseThrow(IllegalStateException::new)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestMessage extends Message<TestMessage> {

		@Override protected TestMessage self() { return this; }

	}

}
