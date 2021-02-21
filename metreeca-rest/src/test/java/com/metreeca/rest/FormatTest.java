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

import static com.metreeca.rest.Format.mimes;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;

final class FormatTest {

	@Nested final class MIMETypes {

		@Test void testParseStrings() {

			assertThat(emptyList())
					.as("empty")
					.isEqualTo(mimes(""));

			assertThat(singletonList("text/turtle"))
					.as("single")
					.isEqualTo(mimes("text/turtle"));

			assertThat(asList("text/turtle", "text/plain"))
					.as("multiple")
					.isEqualTo(mimes("text/turtle, text/plain"));

			assertThat(singletonList("*/*"))
					.as("wildcard")
					.isEqualTo(mimes("*/*"));

			assertThat(singletonList("text/*"))
					.as("type wildcard")
					.isEqualTo(mimes("text/*"));

		}

		@Test void testParseLeniently() {

			assertThat(singletonList("text/plain"))
					.as("normalize case")
					.isEqualTo(mimes("text/Plain"));

			assertThat(singletonList("text/plain"))
					.as("ignores spaces")
					.isEqualTo(mimes(" text/plain ; q = 0.3"));

			assertThat(asList("text/turtle", "text/plain", "text/csv"))
					.as("lenient separators")
					.isEqualTo(mimes("text/turtle, text/plain\ttext/csv"));

		}

		@Test void testSortOnQuality() {

			assertThat(asList("text/plain", "text/turtle"))
					.as("sorted")
					.isEqualTo(mimes("text/turtle;q=0.1, text/plain;q=0.2"));

			assertThat(asList("text/plain", "text/turtle"))
					.as("sorted with default values")
					.isEqualTo(mimes("text/turtle;q=0.1, text/plain"));

			assertThat(asList("text/plain", "text/turtle"))
					.as("sorted with corrupt values")
					.isEqualTo(mimes("text/turtle;q=x, text/plain"));

		}

	}

}