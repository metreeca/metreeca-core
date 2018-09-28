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

package com.metreeca.rest.formats;

import com.metreeca.rest.Request;

import org.junit.jupiter.api.Test;

import java.io.*;

import javax.json.Json;
import javax.json.JsonObject;

import static com.metreeca.rest.Result.value;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


final class JSONFormatTest {

	private static final JsonObject TestJSON=Json.createObjectBuilder()
			.add("one", 1)
			.add("two", 2)
			.build();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRetrieveJSON() {

		final Request request=new Request()
				.header("content-type", JSONFormat.MIME)
				.body(reader()).set(() -> new StringReader(TestJSON.toString()));

		assertEquals(TestJSON, request.body(json()).get().orElseGet(() -> fail("no json representation")));
	}

	@Test void testRetrieveJSONChecksContentType() {

		final Request request=new Request()
				.body(reader()).set(() -> new StringReader(TestJSON.toString()));

		assertFalse(request.body(json()).get().isPresent());
	}

	@Test void testConfigureJSON() {

		final Request request=new Request().body(json()).set(TestJSON);

		assertEquals(TestJSON, request.body(writer())

				.flatMap(client -> {
					try (final StringWriter writer=new StringWriter()) {

						client.accept(() -> writer);

						return value(writer.toString());

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				})

				.flatMap(test -> value(Json.createReader(new StringReader(test)).readObject()))

				.get()

				.orElseGet(() -> fail("missing outbound representation"))

		);
	}

	@Test void testConfigureJSONSetsContentType() {

		final Request request=new Request().body(json()).set(TestJSON);

		assertEquals(JSONFormat.MIME, request.header("content-type").orElseGet(() -> fail("no content-type header")));

	}

}
