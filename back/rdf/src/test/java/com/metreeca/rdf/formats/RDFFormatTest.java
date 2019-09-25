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

package com.metreeca.rdf.formats;

import com.metreeca.rdf.Form;
import com.metreeca.rdf.ValuesTest;
import com.metreeca.rest.*;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.*;

import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.truths.ModelAssert.assertThat;
import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.ValuesTest.decode;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;
import static com.metreeca.rest.formats.TextFormat.text;

import static org.junit.jupiter.api.Assertions.fail;

import static java.nio.charset.StandardCharsets.UTF_8;


final class RDFFormatTest {

	private static final String external="http://example.com/";
	private static final String internal="app://local/";


	@Nested final class Getter {

		@Test void testHandleMissingInput() {
			new Request()

					.body(rdf())

					.value(value -> fail("unexpected RDF body {"+value+"}"))
					.error(error -> Assertions.assertThat(error).isEqualTo(Body.Missing));
		}

		@Test void testHandleEmptyInput() {
			new Request()

					.body(input(), Codecs::input)

					.body(rdf())

					.value(value -> assertThat(value).isEmpty())
					.error(error -> fail("unexpected error {"+error+"}"));
		}

		@Test void testRewriteRequestBody() {
			new Request()

					.base(internal)
					.path("/")
					.header(RDFFormat.ExternalBase, external)
					.body(input(), () -> Codecs.input(new StringReader(
							"<http://example.com/container> ldp:contains <http://example.com/resource> ."
					)))

					.body(rdf())

					.value(statements -> assertThat(statements)
							.isIsomorphicTo(decode("<app://local/container> ldp:contains <app://local/resource> ."))
					)

					.error(error -> fail("unexpected error {"+error+"}"));
		}

	}

	@Nested final class Setter {

		@Test void testConfigureWriterBaseIRI() {
			new Request()

					.base(ValuesTest.Base+"context/")
					.path("/container/")

					.reply(response -> response
							.status(Response.OK)
							.shape(field(LDP.CONTAINS, datatype(Form.IRIType)))
							.body(rdf(), decode("</context/container/> ldp:contains </context/container/x>."))
					)

					.accept(response -> ResponseAssert.assertThat(response)
							.hasBody(text(), text -> Assertions.assertThat(text)
									.contains("@base <"+ValuesTest.Base+"context/container/"+">")
							)
					);
		}


		@Test void testRewriteResponseBody() {
			new Response(new Request().base(internal))

					.header(RDFFormat.ExternalBase, external)
					.body(rdf(), decode("<app://local/container> ldp:contains <app://local/resource> ."))

					.body(output())

					.value(consumer -> {

						try (final ByteArrayOutputStream stream=new ByteArrayOutputStream()) {

							consumer.accept(() -> stream);

							assertThat(decode(new String(stream.toByteArray(), UTF_8)))
									.isIsomorphicTo(decode("<http://example.com/container> ldp:contains <http://example.com/resource> ."));

							return consumer;

						} catch ( final IOException e ) {
							throw new UncheckedIOException(e);
						}

					})

					.error(error -> fail("unexpected error {"+error+"}"));
		}

	}

}
