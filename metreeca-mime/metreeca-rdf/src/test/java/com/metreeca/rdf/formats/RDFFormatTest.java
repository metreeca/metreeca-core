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

package com.metreeca.rdf.formats;

import com.metreeca.core.*;
import com.metreeca.json.Shape;
import com.metreeca.rdf.Values;
import com.metreeca.rdf.ValuesTest;

import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.JsonException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.metreeca.core.Message.types;
import static com.metreeca.core.Response.UnsupportedMediaType;
import static com.metreeca.core.formats.InputFormat.input;
import static com.metreeca.core.formats.TextFormat.text;
import static com.metreeca.json.Shape.shape;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.ValuesTest.decode;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static java.util.Arrays.asList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.fail;


final class RDFFormatTest {

	private static final String external="http://example.com/";
	private static final String internal="app://local/";


	private void exec(final Runnable... tasks) {
		new Context().exec(tasks).clear();
	}


	@Nested final class Path { // !!! remove

		private final Shape shape=and(
				field(RDF.FIRST, field(RDF.REST)),
				field(inverse(RDF.FIRST), field(RDF.REST))
		);


		@Test void testParsePaths() {
			exec(() -> {

				assertThat(JSONLDFormat.path("app:/", shape, ""))
						.as("empty")
						.isEmpty();

				assertThat(JSONLDFormat.path("app:/", shape, "<"+RDF.FIRST+">"))
						.as("direct iri")
						.containsExactly(RDF.FIRST);

				assertThat(JSONLDFormat.path("app:/", shape, "^<"+RDF.FIRST+">"))
						.as("inverse iri")
						.containsExactly(inverse(RDF.FIRST));

				assertThat(JSONLDFormat.path("app:/", shape, "<"+RDF.FIRST+">/<"+RDF.REST+">"))
						.as("iri slash path")
						.containsExactly(RDF.FIRST, RDF.REST);

				assertThat(JSONLDFormat.path("app:/", shape, "first"))
						.as("direct alias")
						.containsExactly(RDF.FIRST);

				assertThat(JSONLDFormat.path("app:/", shape, "firstOf"))
						.as("inverse alias")
						.containsExactly(inverse(RDF.FIRST));

				assertThat(JSONLDFormat.path("app:/", shape, "first/rest"))
						.as("alias slash path")
						.containsExactly(RDF.FIRST, RDF.REST);

				assertThat(JSONLDFormat.path("app:/", shape, "firstOf.rest"))
						.as("alias dot path")
						.containsExactly(inverse(RDF.FIRST), RDF.REST);

			});
		}


		@Test void testRejectUnknownPathSteps() {
			exec(() -> assertThatExceptionOfType(JsonException.class)
					.isThrownBy(() -> JSONLDFormat.path("app:/", shape, "first/unknown")));
		}

		@Test void testRejectMalformedPaths() {
			exec(() -> assertThatExceptionOfType(JsonException.class)
					.isThrownBy(() -> JSONLDFormat.path("app:/", shape, "---")));
		}

	}

	@Nested final class Services {

		private final TestService Turtle=new TestService(org.eclipse.rdf4j.rio.RDFFormat.TURTLE);
		private final TestService RDFXML=new TestService(org.eclipse.rdf4j.rio.RDFFormat.RDFXML);
		private final TestService Binary=new TestService(org.eclipse.rdf4j.rio.RDFFormat.BINARY);


		@Test void testServiceScanMimeTypes() {

			assertThat((Object)Binary)
					.as("none matching")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, types("text/none")));

			assertThat((Object)Turtle)
					.as("single matching")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, types("text/turtle")));

			assertThat((Object)Turtle)
					.as("leading matching")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, asList("text/turtle", "text/plain")));

			assertThat((Object)Turtle)
					.as("trailing matching")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, asList("text/turtle", "text/none")));

			assertThat((Object)Binary)
					.as("wildcard")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, types("*/*, text/plain;q=0.1")));

			assertThat((Object)Turtle)
					.as("type pattern")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, types("text/*, text/plain;q=0.1")));

		}

		@Test void testServiceTrapUnknownFallback() {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
					service(new org.eclipse.rdf4j.rio.RDFFormat(
							"None", "text/none",
							StandardCharsets.UTF_8, "",
							org.eclipse.rdf4j.rio.RDFFormat.NO_NAMESPACES, org.eclipse.rdf4j.rio.RDFFormat.NO_CONTEXTS,
							org.eclipse.rdf4j.rio.RDFFormat.NO_RDF_STAR
					), types("text/none"))
			);
		}


		private TestService service(final org.eclipse.rdf4j.rio.RDFFormat fallback, final List<String> types) {

			final TestRegistry registry=new TestRegistry();

			registry.add(Binary); // no text/* MIME type
			registry.add(RDFXML); // no text/* MIME type
			registry.add(Turtle);

			return com.metreeca.rdf.formats.RDFFormat.service(registry, fallback, types);
		}


		private final class TestRegistry
				extends FileFormatServiceRegistry<org.eclipse.rdf4j.rio.RDFFormat, TestService> {

			private TestRegistry() {
				super(TestService.class);
			}

			@Override protected org.eclipse.rdf4j.rio.RDFFormat getKey(final TestService service) {
				return service.getFormat();
			}

		}

		private final class TestService {

			private final org.eclipse.rdf4j.rio.RDFFormat format;


			private TestService(final org.eclipse.rdf4j.rio.RDFFormat format) {
				this.format=format;
			}

			private org.eclipse.rdf4j.rio.RDFFormat getFormat() {
				return format;
			}

			@Override public String toString() {
				return format.getDefaultMIMEType();
			}

		}

	}

	@Nested final class Decoder {

		@Test void testHandleMissingInput() {
			exec(() -> new Request().body(rdf()).fold(
					error -> assertThat(error.getStatus()).isEqualTo(UnsupportedMediaType),
					value -> fail("unexpected RDF body {"+value+"}")
			));
		}

		@Test void testHandleEmptyInput() {
			exec(() -> new Request().body(input(), Xtream::input).body(rdf()).fold(
					error -> fail("unexpected error {"+error+"}"),
					value -> assertThat(value).isEmpty()
			));
		}

	}

	@Nested final class Encoder {

		@Test void testConfigureWriterBaseIRI() {
			exec(() -> new Request()

					.base(ValuesTest.Base+"context/")
					.path("/container/")

					.reply(response -> response
							.status(Response.OK)
							.attribute(shape(), field(LDP.CONTAINS, datatype(Values.IRIType)))
							.body(rdf(), decode("</context/container/> ldp:contains </context/container/x>."))
					)

					.accept(response -> ResponseAssert.assertThat(response)
							.hasBody(text(), text -> assertThat(text)
									.contains("@base <"+ValuesTest.Base+"context/container/"+">")
							)
					)
			);
		}

	}

}
