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

package com.metreeca.rdf4j.assets;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.metreeca.rdf4j.assets.Snippets.*;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;


final class SnippetsTest {

	@Test void testNothing() {

		final Object x=new Object();
		final Object y=new Object();

		assertThat(source(nothing(id(x, y))))
				.as("no code generated")
				.isEmpty();

		assertThat(source(nothing(id(x, y)), id(x), "=", id(y)))
				.as("snippets evaluated with side effects")
				.matches("(\\d+)=\\1");

	}

	@Test void testSnippet() {

		assertThat(source(snippet((Object)null))).isEqualTo("");
		assertThat(source(snippet(1, 2, 3))).isEqualTo("123");

		assertThat(source(snippet(asList("an", " ", "iterable")))).isEqualTo("an iterable");
		assertThat(source(snippet(Stream.of("a", " ", "stream")))).isEqualTo("a stream");

		assertThat(source(snippet(123))).isEqualTo("123");
		assertThat(source(snippet("string"))).isEqualTo("string");

	}

	@Test void testTemplate() {

		assertThat(source(snippet(null, "text"))).isEqualTo("text");
		assertThat(source(snippet("verbatim", " ", "text"))).isEqualTo("verbatim text");

		assertThat(source(snippet(
				"<< {article} {object} >>", "a", snippet("string")
		))).isEqualTo("<< a string >>");

		assertThat(source(snippet(
				"<< {reused} {missing} {reused} >>", "text"
		))).isEqualTo("<< text text >>");

	}


	//// Identifiers ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testId() {

		final Object x=new Object();
		final Object y=new Object();
		final Object z=new Object();

		assertThat(source(id(x)))
				.as("formatted")
				.matches("\\d+");

		assertThat(source(id(x), "=", id(x)))
				.as("idempotent")
				.matches("(\\d+)=\\1");

		assertThat(source(id(x), "!=", id(y)))
				.as("unique")
				.doesNotMatch("(\\d+)!=\\1");

		assertThat(source(id(x, y, z), "=", id(y), "=", id(z)))
				.as("aliased")
				.matches("(\\d+)=\\1=\\1");

		Assertions.assertThatThrownBy(() -> source(id(x, z), id(y, z)))
				.as("clashes trapped")
				.isInstanceOf(IllegalStateException.class);

	}


	//// Indenting /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testIndentBraceBlocks() {

		assertThat(source("{\nuno\n}\ndue"))
				.as("indented block")
				.isEqualTo("{\n    uno\n}\ndue");

		assertThat(source("{ {\nuno\n} }\ndue"))
				.as("inline block")
				.isEqualTo("{ {\n    uno\n} }\ndue");

	}

	@Test void testIgnoreLeadingSpaces() {

		assertThat(source("  {\n\tuno\n  due\n }"))
				.as("single")
				.isEqualTo("{\n    uno\n    due\n}");

	}

	@Test void testCollapseSpaces() {

		assertThat(source(" text"))
				.as("leading")
				.isEqualTo("text");

		assertThat(source("uno  due"))
				.as("inside")
				.isEqualTo("uno due");

	}

	@Test void testCollapseNewlines() {

		assertThat(source("uno\ndue"))
				.as("single")
				.isEqualTo("uno\ndue");

		assertThat(source("uno\n\n\n\ndue"))
				.as("multiple")
				.isEqualTo("uno\n\ndue");

	}


}
