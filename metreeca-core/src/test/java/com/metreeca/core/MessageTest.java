/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.core;

import com.metreeca.core.formats.InputFormat;
import com.metreeca.core.formats.TextFormat;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.function.Function;

import static com.metreeca.core.Message.types;
import static com.metreeca.core.MessageAssert.assertThat;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;


final class MessageTest {

	@Nested final class MIMETypes {

		@Test void testTypesParseStrings() {

			assertThat(emptyList())
					.as("empty")
					.isEqualTo(types(""));

			assertThat(singletonList("text/turtle"))
					.as("single")
					.isEqualTo(types("text/turtle"));

			assertThat(asList("text/turtle", "text/plain"))
					.as("multiple")
					.isEqualTo(types("text/turtle, text/plain"));

			assertThat(singletonList("*/*"))
					.as("wildcard")
					.isEqualTo(types("*/*"));

			assertThat(singletonList("text/*"))
					.as("type wildcard")
					.isEqualTo(types("text/*"));

		}

		@Test void testTypesParseLeniently() {

			assertThat(singletonList("text/plain"))
					.as("normalize case")
					.isEqualTo(types("text/Plain"));

			assertThat(singletonList("text/plain"))
					.as("ignores spaces")
					.isEqualTo(types(" text/plain ; q = 0.3"));

			assertThat(asList("text/turtle", "text/plain", "text/csv"))
					.as("lenient separators")
					.isEqualTo(types("text/turtle, text/plain\ttext/csv"));

		}

		@Test void testSortOnQuality() {

			assertThat(asList("text/plain", "text/turtle"))
					.as("sorted")
					.isEqualTo(types("text/turtle;q=0.1, text/plain;q=0.2"));

			assertThat(asList("text/plain", "text/turtle"))
					.as("sorted with default values")
					.isEqualTo(types("text/turtle;q=0.1, text/plain"));

			assertThat(asList("text/plain", "text/turtle"))
					.as("sorted with corrupt values")
					.isEqualTo(types("text/turtle;q=x, text/plain"));

		}

	}

	@Nested final class Headers {

		@Test void testHeadersNormalizeHeaderNames() {
			assertThat(new MessageMock()
					.headers("TEST-header", "value")
			)
					.hasHeader("TEST-Header");
		}

		@Test void testHeadersIgnoreHeaderCase() {
			assertThat(new MessageMock()
					.header("TEST-header", "value")
			)
					.hasHeader("test-header", "value");
		}

		@Test void testHeadersIgnoreEmptyHeaders() {
			assertThat(new MessageMock()
					.headers("test-header", emptySet())
			)
					.doesNotHaveHeader("test-header");
		}

		@Test void testHeadersOverwritesValues() {
			assertThat(new MessageMock()
					.header("test-header", "one")
					.header("test-header", "two")
			)
					.hasHeader("test-header", "two");
		}

		@Test void testDefaultsValues() {

			final MessageMock message=new MessageMock()
					.header("+present", "one")
					.header("~present", "two")
					.header("~missing", "two");

			assertThat(message.header("present")).hasValue("one");
			assertThat(message.header("missing")).hasValue("two");

		}

		//@Test void testHeadersAppendsValues() {
		//
		//	final MessageMock message=new MessageMock()
		//			.header("+test-header", "one")
		//			.header("+test-header", "two");
		//
		//	assertEquals(list("one", "two"), list(message.headers("test-header")));
		//}
		//
		//@Test void testHeadersAppendsCookies() {
		//
		//	final MessageMock message=new MessageMock()
		//			.header("set-cookie", "one")
		//			.header("set-cookie", "two");
		//
		//	assertEquals(list("one", "two"), list(message.headers("set-cookie")));
		//}
		//
		//@Test void testHeadersIgnoresEmptyAndDuplicateValues() {
		//
		//	final MessageMock message=new MessageMock()
		//			.headers("test-header", "one", "two", "", "two");
		//
		//	assertEquals(list("one", "two"), list(message.headers("test-header")));
		//}

	}

	@Nested final class Body {

		@Test void testBodyCaching() {

			final MessageMock message=new MessageMock()
					.body(InputFormat.input(), () -> new ByteArrayInputStream("test".getBytes(UTF_8)));

			final Function<Message<?>, String> accessor=m -> m
					.body(TextFormat.text()).fold(error -> fail("missing test body"), value -> value);

			assertSame(accessor.apply(message), accessor.apply(message));
		}

	}

}
