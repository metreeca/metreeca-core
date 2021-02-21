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

package com.metreeca.rdf.formats;

import com.metreeca.json.Values;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.common.lang.service.FileFormatServiceRegistry;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.List;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.statement;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rest.Format.mimes;
import static com.metreeca.rest.Response.UnsupportedMediaType;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.JSONLDFormat.shape;
import static com.metreeca.rest.formats.TextFormat.text;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.junit.jupiter.api.Assertions.fail;


final class RDFFormatTest {

	private void exec(final Runnable... tasks) {
		new Context().exec(tasks).clear();
	}


	@Nested final class Services {

		private final TestService Turtle=new TestService(org.eclipse.rdf4j.rio.RDFFormat.TURTLE);
		private final TestService RDFXML=new TestService(org.eclipse.rdf4j.rio.RDFFormat.RDFXML);
		private final TestService Binary=new TestService(org.eclipse.rdf4j.rio.RDFFormat.BINARY);


		@Test void testServiceScanMimeTypes() {

			assertThat((Object)Binary)
					.as("none matching")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, mimes("text/none")));

			assertThat((Object)Turtle)
					.as("single matching")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, mimes("text/turtle")));

			assertThat((Object)Turtle)
					.as("leading matching")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, asList("text/turtle", "text/plain")));

			assertThat((Object)Turtle)
					.as("trailing matching")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, asList("text/turtle", "text/none")));

			assertThat((Object)Binary)
					.as("wildcard")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, mimes("*/*, text/plain;q=0.1")));

			assertThat((Object)Turtle)
					.as("type pattern")
					.isSameAs(service(org.eclipse.rdf4j.rio.RDFFormat.BINARY, mimes("text/*, text/plain;q=0.1")));

		}

		@Test void testServiceTrapUnknownFallback() {
			assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() ->
					service(new org.eclipse.rdf4j.rio.RDFFormat(
							"None", "text/none",
							StandardCharsets.UTF_8, "",
							org.eclipse.rdf4j.rio.RDFFormat.NO_NAMESPACES, org.eclipse.rdf4j.rio.RDFFormat.NO_CONTEXTS,
							org.eclipse.rdf4j.rio.RDFFormat.NO_RDF_STAR
					), mimes("text/none"))
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

					.base("http://example.com/base/")
					.path("/")

					.reply(response -> response
							.status(Response.OK)
							.attribute(shape(), field(LDP.CONTAINS, datatype(Values.IRIType)))
							.body(rdf(), singletonList(statement(
									iri("http://example.com/base/"), LDP.CONTAINS, iri("http://example.com/base/x")
							)))
					)

					.accept(response -> assertThat(response)
							.hasBody(text(), text -> assertThat(text)
									.contains("@base <"+"http://example.com/base/"+">")
							)
					)
			);
		}

	}

}
