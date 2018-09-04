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

package com.metreeca.form.sparql;

import org.junit.Test;

import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.junit.Assert.assertEquals;

import static java.util.Arrays.asList;


public final class SPARQLBuilderTest {

	@Test public void testIgnoreNullArguments() {

		assertEquals("null argument", "", write(null));

	}


	@Test public void testWriteLiterals() {

		assertEquals("textual literal", "string", write("string"));
		assertEquals("numeric literal", "123", write(123));

	}

	@Test public void testWriteStructures() {

		assertEquals("array", "abc", write(new String[] {"a", "b", "c"}));
		assertEquals("iterable", "abc", write(asList("a", "b", "c")));
		assertEquals("stream", "abc", write(Stream.of("a", "b", "c")));
		assertEquals("supplier", "abc", write((Supplier<Object>)() -> "abc"));

	}

	@Test public void testWriteNestedStructures() {

		assertEquals("nested stream", "abcde", write(Stream.of("a", Stream.of("b", "c", "d"), "e")));
		assertEquals("nested iterable", "abcde", write(asList("a", asList("b", "c", "d"), "e")));
		assertEquals("nested array", "abcde", write(asList("a", new String[] {"b", "c", "d"}, "e")));

	}


	@Test public void testIndentBraceBlocks() {

		assertEquals("indented block", "{\n    uno\n}\ndue", write("{\nuno\n}\ndue"));
		assertEquals("inline block", "{ {\n    uno\n} }\ndue", write("{ {\nuno\n} }\ndue"));

	}

	@Test public void testIndentMarkedBlocks() {

		assertEquals("indented block", "<\n    uno\n>\ndue", write("<\n\tuno\n\b>\ndue"));
		assertEquals("inline block", "< <\n    uno\n> >\ndue", write("<\t <\nuno\b\n> >\ndue"));

	}


	@Test public void testCollapseSpaces() {

		assertEquals("leading", "text", write(" text"));
		assertEquals("inside", "uno due", write("uno  due"));

	}

	@Test public void testCollapseNewlines() {

		assertEquals("single", "uno\ndue", write("uno\ndue"));
		assertEquals("multiple", "uno\n\ndue", write("uno\n\n\ndue"));
		assertEquals("leading feed", "uno\n\ndue", write("uno\f\ndue"));
		assertEquals("trailing feed", "uno\n\ndue", write("uno\n\fdue"));

	}

	@Test public void testInsertNewlines() {

		assertEquals("single", "uno\n\ndue", write("uno\fdue"));
		assertEquals("multiple", "uno\n\ndue", write("uno\f\f\fdue"));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String write(final Object object) {
		return new SPARQLBuilder().text(object).text();
	}

}
