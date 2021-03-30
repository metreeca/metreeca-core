/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.util.function.Function;

import static com.metreeca.rest.MessageAssert.assertThat;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.TextFormat.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Collections.emptySet;


final class MessageTest {

	private Message<?> message() {
		return new Request();
	}


	@Nested final class Headers {

		@Test void testHeadersNormalizeHeaderNames() {
			assertThat(message()
					.headers("TEST-header", "value")
			)
					.hasHeader("TEST-Header");
		}

		@Test void testHeadersIgnoreHeaderCase() {
			assertThat(message()
					.header("TEST-header", "value")
			)
					.hasHeader("test-header", "value");
		}

		@Test void testHeadersIgnoreEmptyHeaders() {
			assertThat(message()
					.headers("test-header", emptySet())
			)
					.doesNotHaveHeader("test-header");
		}

		@Test void testHeadersOverwritesValues() {
			assertThat(message()
					.header("test-header", "one")
					.header("test-header", "two")
			)
					.hasHeader("test-header", "two");
		}

		@Test void testDefaultsValues() {

			final Message<?> message=message()
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

			final Message<?> message=message()
					.body(input(), () -> new ByteArrayInputStream("test".getBytes(UTF_8)));

			final Function<Message<?>, String> accessor=m -> m
					.body(text()).fold(error -> fail("missing test body"), value -> value);

			assertSame(accessor.apply(message), accessor.apply(message));
		}

	}

}
