/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf._engine;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static com.metreeca.rdf._engine.Snippets.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.Arrays.asList;


final class SnippetsTest {

	@Test void testNothing() {

		final Object x=new Object();
		final Object y=new Object();

		Assertions.assertThat(source(nothing(id(x, y))))
				.as("no code generated")
				.isEmpty();

		Assertions.assertThat(source(nothing(id(x, y)), id(x), "=", id(y)))
				.as("snippets evaluated with side effects")
				.matches("(\\d+)=\\1");

	}

	@Test void testSnippet() {

		Assertions.assertThat(source(snippet((Object)null))).isEqualTo("");
		Assertions.assertThat(source(snippet(1, 2, 3))).isEqualTo("123");

		Assertions.assertThat(source(snippet(asList("an", " ", "iterable")))).isEqualTo("an iterable");
		Assertions.assertThat(source(snippet(Stream.of("a", " ", "stream")))).isEqualTo("a stream");

		Assertions.assertThat(source(snippet(123))).isEqualTo("123");
		Assertions.assertThat(source(snippet("string"))).isEqualTo("string");

	}

	@Test void testTemplate() {

		Assertions.assertThat(source(snippet(null, "text"))).isEqualTo("text");
		Assertions.assertThat(source(snippet("verbatim", " ", "text"))).isEqualTo("verbatim text");

		Assertions.assertThat(source(snippet(
				"<< {article} {object} >>", "a", snippet("string")
		))).isEqualTo("<< a string >>");

		Assertions.assertThat(source(snippet(
				"<< {reused} {missing} {reused} >>", "text"
		))).isEqualTo("<< text text >>");

	}


	//// Identifiers ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testId() {

		final Object x=new Object();
		final Object y=new Object();
		final Object z=new Object();

		Assertions.assertThat(source(id(x)))
				.as("formatted")
				.matches("\\d+");

		Assertions.assertThat(source(id(x), "=", id(x)))
				.as("idempotent")
				.matches("(\\d+)=\\1");

		Assertions.assertThat(source(id(x), "!=", id(y)))
				.as("unique")
				.doesNotMatch("(\\d+)!=\\1");

		Assertions.assertThat(source(id(x, y, z), "=", id(y), "=", id(z)))
				.as("aliased")
				.matches("(\\d+)=\\1=\\1");

		Assertions.assertThatThrownBy(() -> source(id(x, z), id(y, z)))
				.as("clashes trapped")
				.isInstanceOf(IllegalStateException.class);

	}


	//// Indenting /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testIndentBraceBlocks() {

		Assertions.assertThat(source("{\nuno\n}\ndue"))
				.as("indented block")
				.isEqualTo("{\n    uno\n}\ndue");

		Assertions.assertThat(source("{ {\nuno\n} }\ndue"))
				.as("inline block")
				.isEqualTo("{ {\n    uno\n} }\ndue");

	}

	@Test void testIgnoreLeadingSpaces() {

		Assertions.assertThat(source("  {\n\tuno\n  due\n }"))
				.as("single")
				.isEqualTo("{\n    uno\n    due\n}");

	}

	@Test void testCollapseSpaces() {

		Assertions.assertThat(source(" text"))
				.as("leading")
				.isEqualTo("text");

		Assertions.assertThat(source("uno  due"))
				.as("inside")
				.isEqualTo("uno due");

	}

	@Test void testCollapseNewlines() {

		Assertions.assertThat(source("uno\ndue"))
				.as("single")
				.isEqualTo("uno\ndue");

		Assertions.assertThat(source("uno\n\n\n\ndue"))
				.as("multiple")
				.isEqualTo("uno\n\ndue");

	}


}
