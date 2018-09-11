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

import static com.metreeca.form.things.Lists.list;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import static java.util.Collections.emptySet;


final class MessageTest {

	@Test void testIgnoreEmptyHeaders() {

		final TestMessage message=new TestMessage()
				.headers("test-header", emptySet());

		assertFalse(message.headers().containsKey("test-header"));
	}


	@Test void testConfigurePreserving() {

		final TestMessage message=new TestMessage()
				.header("test-header", "one")
				.header("test-header", "two");

		assertEquals(list("one", "two"), list(message.headers("test-header")));
	}

	@Test void testConfigurePreservingIgnoreEmptyAndDuplicateValues() {

		final TestMessage message=new TestMessage()
				.headers("test-header", "", "one")
				.headers("test-header", "two", "", "one", "two");

		assertEquals(list("one", "two"), list(message.headers("test-header")));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestMessage extends Message<TestMessage> {

		@Override protected TestMessage self() { return this; }

	}
}
