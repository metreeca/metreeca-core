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

package com.metreeca.rest.handlers;


import com.metreeca.rest.*;

import org.junit.jupiter.api.Test;

import java.io.*;

import static com.metreeca.rest.formats.OutputFormat.output;
import static org.assertj.core.api.Assertions.assertThat;


final class WorkerTest {

	private Future<Response> handler(final Request request) {
		return request.reply(response -> response

				.status(Response.OK)

				.body(output(), output -> {
					try {
						output.write("body".getBytes());
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				})

		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testHandleOPTIONSByDefault() {
		new Worker()

				.get(this::handler)

				.handle(new Request().method(Request.OPTIONS))

				.accept(response -> {

					assertThat(response.status()).isEqualTo(Response.OK);
					assertThat(response.headers("Allow")).containsExactly(Request.OPTIONS, Request.HEAD, Request.GET);

				});
	}

	@Test void testIncludeAllowHeaderOnUnsupportedMethods() {
		new Worker()

				.get(this::handler)

				.handle(new Request().method(Request.POST))

				.accept(response -> {

					assertThat(response.status()).isEqualTo(Response.MethodNotAllowed);
					assertThat(response.headers("Allow")).containsExactly(Request.OPTIONS, Request.HEAD, Request.GET);

				});
	}

	@Test void testHandleHEADByDefault() {
		new Worker()

				.get(this::handler)

				.handle(new Request().method(Request.HEAD))

				.accept(response -> {

					assertThat(response.status()).isEqualTo(Response.OK);

					assertThat(response.body(output()).<byte[]>fold(
							e -> new byte[0], v -> {

								final ByteArrayOutputStream output=new ByteArrayOutputStream();

								v.accept(output);

								return output.toByteArray();

							}
					)).isEmpty();


				});
	}

	@Test void testRejectHEADIfGetIsNotSupported() {
		new Worker()

				.handle(new Request().method(Request.HEAD))

				.accept(response -> {

					assertThat(response.status()).isEqualTo(Response.MethodNotAllowed);

				});
	}

}
