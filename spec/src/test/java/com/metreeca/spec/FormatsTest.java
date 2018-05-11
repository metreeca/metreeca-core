/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec;

import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.Test;

import java.nio.charset.Charset;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;


public final class FormatsTest {

	private static final TestService Turtle=new TestService(RDFFormat.TURTLE);
	private static final TestService RDFXML=new TestService(RDFFormat.RDFXML);
	private static final TestService Binary=new TestService(RDFFormat.BINARY);


	//// Types /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testTypesParseStrings() {

		assertEquals("empty", emptyList(), Formats.types(""));
		assertEquals("single", singletonList("text/turtle"), Formats.types("text/turtle"));
		assertEquals("multiple", asList("text/turtle", "text/plain"), Formats.types("text/turtle, text/plain"));

		assertEquals("wildcard", singletonList("*/*"), Formats.types("*/*"));
		assertEquals("type wildcard", singletonList("text/*"), Formats.types("text/*"));

	}

	@Test public void testTypesParseStringLists() {

		assertEquals("empty", emptyList(), Formats.types(""));

		assertEquals("single", asList("text/turtle", "text/plain"),
				Formats.types("text/turtle, text/plain"));

		assertEquals("multiple", asList("text/turtle", "text/plain", "text/csv"),
				Formats.types("text/turtle, text/plain", "text/csv"));

	}

	@Test public void testTypesParseLeniently() {

		assertEquals("normalize case", singletonList("text/plain"), Formats.types("text/Plain"));
		assertEquals("ignores spaces", singletonList("text/plain"), Formats.types(" text/plain ; q = 0.3"));

		assertEquals("lenient separators", asList("text/turtle", "text/plain", "text/csv"),
				Formats.types("text/turtle, text/plain\ttext/csv"));

	}

	@Test public void testSortOnQuality() {

		assertEquals("sorted", asList("text/plain", "text/turtle"),
				Formats.types("text/turtle;q=0.1, text/plain;q=0.2"));

		assertEquals("sorted with default values", asList("text/plain", "text/turtle"),
				Formats.types("text/turtle;q=0.1, text/plain"));

		assertEquals("sorted with corrupt values", asList("text/plain", "text/turtle"),
				Formats.types("text/turtle;q=x, text/plain"));

	}


	//// Service ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testServiceScanMimeTypes() {

		assertSame("none matching", Binary, service(RDFFormat.BINARY, "text/none"));
		assertSame("single matching", Turtle, service(RDFFormat.BINARY, "text/turtle"));
		assertSame("leading matching", Turtle, service(RDFFormat.BINARY, "text/turtle", "text/plain"));
		assertSame("trailing matching", Turtle, service(RDFFormat.BINARY, "text/none", "text/turtle"));

		assertSame("wildcard", Binary, service(RDFFormat.BINARY, "*/*, text/plain;q=0.1"));
		assertSame("type pattern", Turtle, service(RDFFormat.BINARY, "text/*, text/plain;q=0.1"));

	}

	@Test(expected=IllegalArgumentException.class)
	public void testServiceTrapUnknownFallback() {
		service(new RDFFormat("None", "text/none", Charset.forName("UTF-8"), "", false, false), "text/none");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private TestService service(final RDFFormat fallback, final String... mimes) {

		final TestRegistry registry=new TestRegistry();

		registry.add(Binary); // no text/* MIME type
		registry.add(RDFXML); // no text/* MIME type
		registry.add(Turtle);

		return Formats.service(registry, fallback, mimes);
	}


	private static final class TestRegistry extends FileFormatServiceRegistry<RDFFormat, TestService> {

		private TestRegistry() {
			super(TestService.class);
		}

		@Override protected RDFFormat getKey(final TestService service) {
			return service.getFormat();
		}

	}

	private static final class TestService {

		private final RDFFormat format;


		private TestService(final RDFFormat format) {
			this.format=format;
		}

		private RDFFormat getFormat() {
			return format;
		}

		@Override public String toString() {
			return format.getDefaultMIMEType();
		}

	}

}
