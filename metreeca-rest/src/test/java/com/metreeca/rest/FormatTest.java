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

import static com.metreeca.rest.Format.mime;
import static com.metreeca.rest.Format.mimes;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

final class FormatTest {

	@Nested final class MIMEType {

		@Test void testGuessKnownType() {
			assertThat(mime("/static/index.html"))
					.isEqualTo("text/html");
		}

		@Test void testHandleUknownType() {
			assertThat(mime("/static/index.unknown"))
					.isEqualTo("application/octet-stream");
		}

		@Test void testHandleMissingType() {
			assertThat(mime("/static/index"))
					.isEqualTo("application/octet-stream");
		}

	}

	@Nested final class MIMETypes {

		@Test void testParseStrings() {

			assertThat(mimes(""))
					.as("empty")
					.isEmpty();

			assertThat(mimes("text/turtle"))
					.as("single")
					.isEqualTo(singletonList("text/turtle"));

			assertThat(mimes("text/turtle, text/plain"))
					.as("multiple")
					.isEqualTo(asList("text/turtle", "text/plain"));

			assertThat(mimes("*/*"))
					.as("wildcard")
					.isEqualTo(singletonList("*/*"));

			assertThat(mimes("text/*"))
					.as("type wildcard")
					.isEqualTo(singletonList("text/*"));

		}

		@Test void testParseLeniently() {

			assertThat(mimes("text/Plain"))
					.as("normalize case")
					.isEqualTo(singletonList("text/plain"));

			assertThat(mimes(" text/plain ; q = 0.3"))
					.as("ignores spaces")
					.isEqualTo(singletonList("text/plain"));

			assertThat(mimes("text/turtle, text/plain\ttext/csv"))
					.as("lenient separators")
					.isEqualTo(asList("text/turtle", "text/plain", "text/csv"));

		}

		@Test void testSortOnQuality() {

			assertThat(mimes("text/turtle;q=0.1, text/plain;q=0.2"))
					.as("sorted")
					.isEqualTo(asList("text/plain", "text/turtle"));

			assertThat(mimes("text/turtle;q=0.1, text/plain"))
					.as("sorted with default values")
					.isEqualTo(asList("text/plain", "text/turtle"));

			assertThat(mimes("text/turtle;q=x, text/plain"))
					.as("sorted with corrupt values")
					.isEqualTo(asList("text/plain", "text/turtle"));

		}

	}

}