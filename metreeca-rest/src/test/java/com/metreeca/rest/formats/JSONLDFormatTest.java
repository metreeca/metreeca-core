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

package com.metreeca.rest.formats;

import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import javax.json.Json;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Shape.required;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Localized.localized;
import static com.metreeca.rest.JSONAssert.assertThat;
import static com.metreeca.rest.Response.*;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.JSONLDFormat.*;
import static com.metreeca.rest.formats.OutputFormat.output;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

final class JSONLDFormatTest {

	private static final String base="http://example.com/";

	private final IRI direct=iri(base, "/direct");
	private final IRI nested=iri(base, "/nested");
	private final IRI reverse=iri(base, "/reverse");
	private final IRI outlier=iri(base, "/outlier");


	private void exec(final Runnable task) {
		new Toolbox().exec(task).clear();
	}


	@Nested final class Decoder {

		private Request request(final String json) {
			return new Request().base(base)

					.header("Content-Type", MIME)

					.set(shape(), field(direct, required()))

					.body(input(), () -> new ByteArrayInputStream(json.getBytes(UTF_8)));
		}

		private Future<Response> response(final Request request) {
			return request.reply(response -> request.body(jsonld()).fold(
					response::map,
					model -> response.status(OK).set(shape(), request.get(shape())).body(jsonld(), model)
			));
		}


		@Test void testReportMalformedPayload() {
			exec(() -> request("{")

					.map(this::response)

					.accept(response -> assertThat(response)
							.hasStatus(BadRequest)
					)
			);
		}

		@Test void testReportInvalidPayload() {
			exec(() -> request("{}")

					.map(this::response)

					.accept(response -> assertThat(response)
							.hasStatus(UnprocessableEntity)
					)
			);
		}

	}

	@Nested final class Encoder {

		private Request request() {
			return new Request().base(base);
		}


		private Response response(final Response response) {

			final IRI item=iri(response.item());
			final BNode bnode=bnode();

			return response.status(OK)

					.set(shape(), and(

							field(direct, required(),
									field(nested, required())
							),

							field("reverse", inverse(reverse), required())

					))

					.body(jsonld(), frame(item, asList(

							statement(item, direct, bnode),
							statement(bnode, nested, item),
							statement(bnode, reverse, item),
							statement(item, outlier, bnode)

					)));
		}


		@Test void testHandleGenericRequests() {
			exec(() -> request()

					.reply(this::response)

					.accept(response -> assertThat(response)
							.hasHeader("Content-Type", JSONFormat.MIME)
							.hasBody(json(), json -> assertThat(json)
									.doesNotHaveField("@context")
							)
					)

			);
		}

		@Test void testHandlePlainJSONRequests() {
			exec(() -> request()

					.header("Accept", JSONFormat.MIME)

					.reply(this::response)

					.accept(response -> assertThat(response)
							.hasHeader("Content-Type", JSONFormat.MIME)
							.hasBody(json(), json -> assertThat(json)
									.doesNotHaveField("@context")
							)
					)

			);
		}

		@Test void testHandleJSONLDRequests() {
			exec(() -> request()

					.header("Accept", MIME)

					.reply(this::response)

					.accept(response -> assertThat(response)
							.hasHeader("Content-Type", MIME)
							.hasBody(json(), json -> assertThat(json)
									.hasField("@context")
							)
					)

			);
		}

		@Test void testGenerateJSONLDContextObjects() {
			new Toolbox()

					.set(keywords(), () -> singletonMap("@id", "id"))

					.exec(() -> request()

							.header("Accept", MIME)

							.reply(this::response)

							.accept(response -> assertThat(response)

									.hasHeader("Content-Type", MIME)

									.hasBody(json(), json -> assertThat(json)

											.hasField("@context", context -> assertThat(context)

													.hasField("id", "@id") // keywords at top level

													.hasField("direct", direct.stringValue())
													.hasField("reverse", Json.createObjectBuilder()
															.add("@reverse", reverse.stringValue())
													)

											)

											.hasField("direct", value -> assertThat(value)

													.hasField("@context", context -> assertThat(context)

															.doesNotHaveField("id") // keywords only at top level

															.hasField("nested", nested.stringValue())

													)

											)

									)
							)
					)

					.clear();
		}


		@Test void testTrimPayload() {
			exec(() -> request()

					.reply(this::response)

					.accept(response -> assertThat(response)
							.hasBody(json(), json -> assertThat(json)
									.doesNotHaveField("outlier")
							)
					)

			);
		}

		@Test void testLocalizePayload() {
			exec(() -> new Request()

					.base(base)
					.header("Accept-Language", "en")

					.reply(response -> {

						final IRI item=iri(response.item());

						return response.status(OK)

								.set(shape(), field(direct, localized()))

								.body(jsonld(), frame(item, asList(

										statement(item, direct, literal("one", "en")),
										statement(item, direct, literal("uno", "it")),
										statement(item, direct, literal("ein", "de"))

								)));
					})

					.accept(response -> assertThat(response)
							.hasBody(json(), json -> assertThat(json)
									.hasField("direct", "one")
							)
					)
			);
		}

		@Test void testReportInvalidPayload() {
			exec(() -> assertThatExceptionOfType(RuntimeException.class).isThrownBy(() -> request()

					.reply(response -> response(response).body(jsonld(), frame(iri(response.item()))))

					.accept(response -> response.body(output()).accept(e -> {},
							target -> target.accept(new ByteArrayOutputStream())
					)))

			);

		}

	}

}