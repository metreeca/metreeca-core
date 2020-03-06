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

package com.metreeca.rdf;

import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.rio.RDFFormat;
import org.junit.jupiter.api.Test;

import java.nio.charset.Charset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;


final class FormatsTest {

	private static final TestService Turtle=new TestService(RDFFormat.TURTLE);
	private static final TestService RDFXML=new TestService(RDFFormat.RDFXML);
	private static final TestService Binary=new TestService(RDFFormat.BINARY);


	//// Types /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testTypesParseStrings() {

		Assertions.assertThat(emptyList()).as("empty").isEqualTo(Formats.types(""));
		Assertions.assertThat(singletonList("text/turtle")).as("single").isEqualTo(Formats.types("text/turtle"));
		Assertions.assertThat(asList("text/turtle", "text/plain")).as("multiple").isEqualTo(Formats.types("text/turtle, text/plain"));

		Assertions.assertThat(singletonList("*/*")).as("wildcard").isEqualTo(Formats.types("*/*"));
		Assertions.assertThat(singletonList("text/*")).as("type wildcard").isEqualTo(Formats.types("text/*"));

	}

	@Test void testTypesParseStringLists() {

		Assertions.assertThat(emptyList()).as("empty").isEqualTo(Formats.types(""));

		Assertions.assertThat(asList("text/turtle", "text/plain")).as("single").isEqualTo(Formats.types("text/turtle, text/plain"));

		Assertions.assertThat(asList("text/turtle", "text/plain", "text/csv")).as("multiple").isEqualTo(Formats.types("text/turtle, text/plain", "text/csv"));

	}

	@Test void testTypesParseLeniently() {

		Assertions.assertThat(singletonList("text/plain")).as("normalize case").isEqualTo(Formats.types("text/Plain"));
		Assertions.assertThat(singletonList("text/plain")).as("ignores spaces").isEqualTo(Formats.types(" text/plain ; q = 0.3"));

		Assertions.assertThat(asList("text/turtle", "text/plain", "text/csv")).as("lenient separators").isEqualTo(Formats.types("text/turtle, text/plain\ttext/csv"));

	}

	@Test void testSortOnQuality() {

		Assertions.assertThat(asList("text/plain", "text/turtle")).as("sorted").isEqualTo(Formats.types("text/turtle;q=0.1, text/plain;q=0.2"));

		Assertions.assertThat(asList("text/plain", "text/turtle")).as("sorted with default values").isEqualTo(Formats.types("text/turtle;q=0.1, text/plain"));

		Assertions.assertThat(asList("text/plain", "text/turtle")).as("sorted with corrupt values").isEqualTo(Formats.types("text/turtle;q=x, text/plain"));

	}


	//// Service ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testServiceScanMimeTypes() {

		Assertions.assertThat((Object)Binary).as("none matching").isSameAs(service(RDFFormat.BINARY, "text/none"));
		Assertions.assertThat((Object)Turtle).as("single matching").isSameAs(service(RDFFormat.BINARY, "text/turtle"));
		Assertions.assertThat((Object)Turtle).as("leading matching").isSameAs(service(RDFFormat.BINARY, "text/turtle", "text/plain"));
		Assertions.assertThat((Object)Turtle).as("trailing matching").isSameAs(service(RDFFormat.BINARY, "text/none", "text/turtle"));

		Assertions.assertThat((Object)Binary).as("wildcard").isSameAs(service(RDFFormat.BINARY, "*/*, text/plain;q=0.1"));
		Assertions.assertThat((Object)Turtle).as("type pattern").isSameAs(service(RDFFormat.BINARY, "text/*, text/plain;q=0.1"));

	}

	@Test void testServiceTrapUnknownFallback() {
		Assertions.assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
				service(new RDFFormat("None", "text/none", Charset.forName("UTF-8"), "", false, false), "text/none")
		);
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
