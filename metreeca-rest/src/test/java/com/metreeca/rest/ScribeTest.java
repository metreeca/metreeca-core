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

import static com.metreeca.json.Values.iri;
import static com.metreeca.rest.Scribe.code;
import static com.metreeca.rest.Scribe.text;

import static org.assertj.core.api.Assertions.assertThat;

final class ScribeTest {

	@Nested final class Assembling {

		@Test void testText() {
			assertThat(code(text(100))).isEqualTo("100");
			assertThat(code(text(iri("test:iri")))).isEqualTo("<test:iri>");
			assertThat(code(text("verbatim"))).isEqualTo("verbatim");
		}

	}

	@Nested final class Formatting {

		private String format(final CharSequence text) {
			return code(text(text));
		}


		@Test void testCollapseFeeds() {
			assertThat(format("x\fy")).isEqualTo("x\n\ny");
			assertThat(format("x\n\f\n\fy")).isEqualTo("x\n\ny");
		}

		@Test void testCollapseFolds() {
			assertThat(format("x\ry")).isEqualTo("x y");
			assertThat(format("x\r\r\ry")).isEqualTo("x y");
			assertThat(format("x\n\ry")).isEqualTo("x\n\ny");
			assertThat(format("x\n\r\r\ry")).isEqualTo("x\n\ny");
		}

		@Test void testCollapseNewlines() {
			assertThat(format("x\ny")).isEqualTo("x\ny");
			assertThat(format("x\n\n\n\ny")).isEqualTo("x\ny");
		}

		@Test void testCollapseSpaces() {
			assertThat(format("x y")).isEqualTo("x y");
			assertThat(format("x    y")).isEqualTo("x y");
		}


		@Test void testIgnoreLeadingWhitespace() {
			assertThat(format(" {}")).isEqualTo("{}");
			assertThat(format("\n{}")).isEqualTo("{}");
			assertThat(format("\r{}")).isEqualTo("{}");
			assertThat(format("\f{}")).isEqualTo("{}");
			assertThat(format("\f \n\r{}")).isEqualTo("{}");
		}

		@Test void testIgnoreTrailingWhitespace() {
			assertThat(format("{} ")).isEqualTo("{}");
			assertThat(format("{}\n")).isEqualTo("{}");
			assertThat(format("{}\r")).isEqualTo("{}");
			assertThat(format("{}\f")).isEqualTo("{}");
			assertThat(format("{} \f\n\r")).isEqualTo("{}");
		}


		@Test void testIgnoreLineLeadingWhitespace() {
			assertThat(format("x\n  x")).isEqualTo("x\nx");
		}

		@Test void testIgnoreLineTrailingWhitespace() {
			assertThat(format("x  \nx")).isEqualTo("x\nx");
		}


		@Test void tesExpandFolds() {
			assertThat(format("x\rx\n\rx")).isEqualTo("x x\n\nx");
		}

		@Test void testStripWhitespaceInsidePairs() {
			assertThat(format("( x )")).isEqualTo("(x)");
			assertThat(format("[ x ]")).isEqualTo("[x]");
			assertThat(format("{ x }")).isEqualTo("{ x }");
		}


		@Test void testIndentBraceBlocks() {
			assertThat(format("{\nx\n}\ny")).isEqualTo("{\n    x\n}\ny");
			assertThat(format("{\f{ x }\f}")).isEqualTo("{\n\n    { x }\n\n}");
		}

		@Test void testInlineBraceBlocks() {
			assertThat(format("{ {\nx\n} }\ny")).isEqualTo("{ {\n    x\n} }\ny");
		}

		@Test void test() {
			System.out.println(code(text("\rwhere {\f{\rselect {\f@\f} limit }\f}")));
		}

	}

}