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
			return new Request()

					.base(base);
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