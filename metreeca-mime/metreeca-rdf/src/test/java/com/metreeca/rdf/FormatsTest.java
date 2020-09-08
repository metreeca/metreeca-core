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

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.metreeca.core.Message.types;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


final class FormatsTest {

	private static final TestService Turtle=new TestService(RDFFormat.TURTLE);
	private static final TestService RDFXML=new TestService(RDFFormat.RDFXML);
	private static final TestService Binary=new TestService(RDFFormat.BINARY);


	//// Service //////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testServiceScanMimeTypes() {

		assertThat((Object)Binary)
				.as("none matching")
				.isSameAs(service(RDFFormat.BINARY, types("text/none")));

		assertThat((Object)Turtle)
				.as("single matching")
				.isSameAs(service(RDFFormat.BINARY, types("text/turtle")));

		assertThat((Object)Turtle)
				.as("leading matching")
				.isSameAs(service(RDFFormat.BINARY, asList("text/turtle", "text/plain")));

		assertThat((Object)Turtle)
				.as("trailing matching")
				.isSameAs(service(RDFFormat.BINARY, asList("text/turtle", "text/none")));

		assertThat((Object)Binary)
				.as("wildcard")
				.isSameAs(service(RDFFormat.BINARY, types("*/*, text/plain;q=0.1")));

		assertThat((Object)Turtle)
				.as("type pattern")
				.isSameAs(service(RDFFormat.BINARY, types("text/*, text/plain;q=0.1")));

	}

	@Test void testServiceTrapUnknownFallback() {
		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
				service(new RDFFormat(
						"None", "text/none",
						StandardCharsets.UTF_8, "",
						RDFFormat.NO_NAMESPACES, RDFFormat.NO_CONTEXTS, RDFFormat.NO_RDF_STAR
				), types("text/none"))
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private TestService service(final RDFFormat fallback, final List<String> types) {

		final TestRegistry registry=new TestRegistry();

		registry.add(Binary); // no text/* MIME type
		registry.add(RDFXML); // no text/* MIME type
		registry.add(Turtle);

		return Formats.service(registry, fallback, types);
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
