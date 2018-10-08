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

package com.metreeca.rest;

import com.metreeca.rest.formats.TextFormat;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.function.Function;

import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Codecs.text;
import static com.metreeca.rest.formats.ReaderFormat.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import static java.util.Collections.emptySet;


final class MessageTest {

	//// Headers ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testHeadersNormalizeHeaderNames() {

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

	@Test void testDefaultsValues() {

		final TestMessage message=new TestMessage()
				.header("+present", "one")
				.header("~present", "two")
				.header("~missing", "two");

		assertThat(message.header("present")).hasValue("one");
		assertThat(message.header("missing")).hasValue("two");

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


	//// Body //////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testBodyCaching() {

		final TestMessage message=new TestMessage().body(reader()).set(() -> new StringReader("test"));

		final Function<Message<?>, String> accessor=m -> {
			return ((Result<String>)m
					.body(TextFormat.text())).map(value -> value, error -> fail("missing test body"));
		};

		assertSame(accessor.apply(message), accessor.apply(message));
	}

	@Test void testBodyOnDemandFiltering() {

		final Message<?> message=new TestMessage()
				.body(TestFormat.test()).pipe(string -> string+"!")
				.body(reader()).set(() -> new StringReader("test"));

		assertEquals("test!",
				((Result<String>)message.body(TestFormat.test())).map(value -> value, error -> fail("missing test body")));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestMessage extends Message<TestMessage> {}

	private static final class TestFormat implements Format<String> {

		private static final TestFormat Instance=new TestFormat();


		private static TestFormat test() { return Instance; }


		@Override public Result<String> get(final Message<?> message) {
			return message.body(reader()).map(supplier -> {
				try (final Reader reader=supplier.get()) {
					return text(reader);
				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});
		}

		@Override public <T extends Message<T>> T set(final T message) {
			return message.header("content-type", "text/plain");
		}

	}

}
