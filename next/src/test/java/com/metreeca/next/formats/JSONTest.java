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

package com.metreeca.next.formats;

import com.metreeca.next.Request;
import com.metreeca.next.Source;
import com.metreeca.next.Target;

import org.junit.jupiter.api.Test;

import java.io.*;

import javax.json.Json;
import javax.json.JsonObject;

import static org.junit.Assert.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;


final class JSONTest {

	private static final JsonObject TestJSON=Json.createObjectBuilder()
			.add("one", 1)
			.add("two", 2)
			.build();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRetrieveJSON() {

		final Request request=new Request().header("content-type", JSON.MIME).body(In.Format, new Source() {
			@Override public Reader reader() { return new StringReader(TestJSON.toString()); }
		});

		assertEquals(TestJSON, request.body(JSON.Format).orElseGet(() -> fail("no json representation")));
	}

	@Test void testRetrieveJSONChecksContentType() {

		final Request request=new Request().body(In.Format, new Source() {
			@Override public Reader reader() { return new StringReader(TestJSON.toString()); }
		});

		assertFalse(request.body(JSON.Format).isPresent());
	}

	@Test void testConfigureJSON() {

		final Request request=new Request().body(JSON.Format, TestJSON);

		assertEquals(TestJSON, request.body(Out.Format)

				.map(consumer -> {
					try (final StringWriter writer=new StringWriter()) {

						consumer.accept(new Target() {
							@Override public Writer writer() { return writer; }
						});

						return writer.toString();

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				})

				.map(test -> Json.createReader(new StringReader(test)).readObject())

				.orElseGet(() -> fail("missing outbound representation"))

		);
	}

	@Test void testConfigureJSONSetsContentType() {

		final Request request=new Request().body(JSON.Format, TestJSON);

		assertEquals(JSON.MIME, request.header("content-type").orElseGet(() -> fail("no content-type header")));

	}

}
