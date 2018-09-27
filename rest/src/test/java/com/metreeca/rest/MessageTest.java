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

import com.metreeca.form.Result;
import com.metreeca.rest.formats.ReaderFormat;
import com.metreeca.rest.formats.TextFormat;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.function.Function;

import static com.metreeca.form.Result.value;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Transputs.text;

import static com.google.common.truth.Truth8.assertThat;
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

		final TestMessage message=new TestMessage().body(ReaderFormat.asReader, () -> new StringReader("test"));

		final Function<Message<?>, String> accessor=m -> m
				.body(TextFormat.asText).map(value -> value, error -> fail("missing test body"));

		assertSame(accessor.apply(message), accessor.apply(message));
	}

	@Test void testBodyOnDemandFiltering()  {

		final Message<?> message=new TestMessage()
				.filter(TestFormat.asTest, string -> string+"!")
				.body(ReaderFormat.asReader, () -> new StringReader("test"));

		assertEquals("test!",
				message.body(TestFormat.asTest).map(value -> value, error -> fail("missing test body")));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestMessage extends Message<TestMessage> {

		@Override protected TestMessage self() { return this; }

	}

	private static final class TestFormat implements Format<String> {

		private static final Format<String> asTest=new TestFormat();

		@Override public Result<String, Failure> get(final Message<?> message) {
			return message.body(ReaderFormat.asReader).value(supplier -> {
				try (final Reader reader=supplier.get()) {
					return value(text(reader));
				} catch ( final IOException e ) {
					throw new UncheckedIOException(e);
				}
			});
		}

		@Override public <T extends Message<T>> T set(final T message, final String value) {
			return message
					.header("content-type", "text/plain")
					.body(asTest, value);
		}

	}

}
