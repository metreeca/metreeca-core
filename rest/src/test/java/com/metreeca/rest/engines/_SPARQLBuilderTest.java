/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.engines;

import org.junit.jupiter.api.Test;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;


final class _SPARQLBuilderTest {

	@Test void testIgnoreNullArguments() {

		assertThat((Object)"").as("null argument").isEqualTo(write(null));

	}


	@Test void testWriteLiterals() {

		assertThat((Object)"string").as("textual literal").isEqualTo(write("string"));
		assertThat((Object)"123").as("numeric literal").isEqualTo(write(123));

	}

	@Test void testWriteStructures() {

		assertThat((Object)"abc").as("array").isEqualTo(write(new String[] {"a", "b", "c"}));
		assertThat((Object)"abc").as("iterable").isEqualTo(write(asList("a", "b", "c")));
		assertThat((Object)"abc").as("stream").isEqualTo(write(Stream.of("a", "b", "c")));
		assertThat((Object)"abc").as("supplier").isEqualTo(write((Supplier<Object>)() -> "abc"));

	}

	@Test void testWriteNestedStructures() {

		assertThat((Object)"abcde").as("nested stream").isEqualTo(write(Stream.of("a", Stream.of("b", "c", "d"), "e")));
		assertThat((Object)"abcde").as("nested iterable").isEqualTo(write(asList("a", asList("b", "c", "d"), "e")));
		assertThat((Object)"abcde").as("nested array").isEqualTo(write(asList("a", new String[] {"b", "c", "d"}, "e")));

	}


	@Test void testIndentBraceBlocks() {

		assertThat((Object)"{\n    uno\n}\ndue").as("indented block").isEqualTo(write("{\nuno\n}\ndue"));
		assertThat((Object)"{ {\n    uno\n} }\ndue").as("inline block").isEqualTo(write("{ {\nuno\n} }\ndue"));

	}

	@Test void testIndentMarkedBlocks() {

		assertThat((Object)"<\n    uno\n>\ndue").as("indented block").isEqualTo(write("<\n\tuno\n\b>\ndue"));
		assertThat((Object)"< <\n    uno\n> >\ndue").as("inline block").isEqualTo(write("<\t <\nuno\b\n> >\ndue"));

	}


	@Test void testCollapseSpaces() {

		assertThat((Object)"text").as("leading").isEqualTo(write(" text"));
		assertThat((Object)"uno due").as("inside").isEqualTo(write("uno  due"));

	}

	@Test void testCollapseNewlines() {

		assertThat((Object)"uno\ndue").as("single").isEqualTo(write("uno\ndue"));
		assertThat((Object)"uno\n\ndue").as("multiple").isEqualTo(write("uno\n\n\ndue"));
		assertThat((Object)"uno\n\ndue").as("leading feed").isEqualTo(write("uno\f\ndue"));
		assertThat((Object)"uno\n\ndue").as("trailing feed").isEqualTo(write("uno\n\fdue"));

	}

	@Test void testInsertNewlines() {

		assertThat((Object)"uno\n\ndue").as("single").isEqualTo(write("uno\fdue"));
		assertThat((Object)"uno\n\ndue").as("multiple").isEqualTo(write("uno\f\f\fdue"));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String write(final Object object) {
		return new _SPARQLBuilder().text(object).text();
	}

}
