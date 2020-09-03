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

import com.metreeca.rest.Request;

import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.*;

import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static com.metreeca.rest.formats.WriterFormat.writer;
import static org.assertj.core.api.Assertions.assertThat;


final class JSONFormatTest {

	private static final JsonObject TestJSON=Json.createObjectBuilder()
			.add("one", 1)
			.add("two", 2)
			.build();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRetrieveJSON() {

		final Request request=new Request()
				.header("content-type", JSONFormat.MIME)
				.body(reader(), () -> new StringReader(TestJSON.toString()));

		assertThat(request.body(json()).value())
				.isPresent()
				.contains(TestJSON);
	}

	@Test void testRetrieveJSONChecksContentType() {

		final Request request=new Request()
				.body(reader(), () -> new StringReader(TestJSON.toString()));

		assertThat(request.body(json()).value())
				.isNotPresent();
	}

	@Test void testConfigureJSON() {

		final Request request=new Request().body(json(), TestJSON);

		assertThat

				(request

						.body(writer())

						.value(client -> {
							try ( final StringWriter writer=new StringWriter() ) {

								client.accept(writer);

								return writer.toString();

							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}
						})

						.value(test -> Json.createReader(new StringReader(test)).readObject())

						.value()
				)

				.isPresent()
				.contains(TestJSON);

	}

	@Test void testConfigureJSONSetsContentType() {

		final Request request=new Request().body(json(), TestJSON);

		assertThat(request.header("content-type"))
				.isPresent()
				.contains(JSONFormat.MIME);

	}

}
