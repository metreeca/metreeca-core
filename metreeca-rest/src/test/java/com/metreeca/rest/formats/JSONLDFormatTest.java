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

import javax.json.Json;

import static com.metreeca.json.Shape.required;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.rest.JSONAssert.assertThat;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.JSONLDFormat.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonMap;

final class JSONLDFormatTest {

	@Nested final class Encoder {

		private static final String base="http://example.com/";

		private final IRI direct=iri(base, "/direct");
		private final IRI nested=iri(base, "/nested");
		private final IRI reverse=iri(base, "/reverse");


		private Request request() {
			return new Request().base(base);
		}

		private Response response(final Response response) {

			final IRI item=iri(response.item());
			final BNode bnode=bnode();

			return response.status(OK)

					.attribute(shape(), and(

							field(direct, required(),
									field(nested, required())
							),

							field(inverse(reverse), alias("reverse"), required())

					))

					.body(jsonld(), asList(

							statement(item, direct, bnode),
							statement(bnode, nested, item),
							statement(bnode, reverse, item)

					));
		}


		@Test void testHandleGenericRequests() {
			new Context().exec(() -> request()

					.reply(this::response)

					.accept(response -> assertThat(response)
							.hasHeader("Content-Type", JSONFormat.MIME)
							.hasBody(json(), json -> assertThat(json)
									.doesNotHaveField("@context")
							)
					)

			).clear();
		}

		@Test void testHandlePlainJSONRequests() {
			new Context().exec(() -> request()

					.header("Accept", JSONFormat.MIME)

					.reply(this::response)

					.accept(response -> assertThat(response)
							.hasHeader("Content-Type", JSONFormat.MIME)
							.hasBody(json(), json -> assertThat(json)
									.doesNotHaveField("@context")
							)
					)

			).clear();
		}

		@Test void testHandleJSONLDRequests() {
			new Context().exec(() -> request()

					.header("Accept", MIME)

					.reply(this::response)

					.accept(response -> assertThat(response)
							.hasHeader("Content-Type", MIME)
							.hasBody(json(), json -> assertThat(json)
									.hasField("@context")
							)
					)

			).clear();
		}

		@Test void testGenerateJSONLDContextObjects() {
			new Context()

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

	}

}