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

package com.metreeca.rest.handlers;


import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;
import com.metreeca.rest.Response;
import com.metreeca.rest.formats._Text;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;


final class WorkerTest {

	private Responder handler(final Request request) {
		return request.reply(response -> response.status(Response.OK).body(_Text.Format, "body"));
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

	@Test @Disabled void testHandleHEADByDefault() {
		new Worker()

				.get(this::handler)

				.handle(new Request().method(Request.HEAD))

				.accept(response -> {

					assertThat(response.status()).isEqualTo(Response.OK);

					// !!! empty body
					//assertThat(response.body(_Output.Format).value()).isEmpty();
					//assertThat(response.body(_Writer.Format).value()).isEmpty();

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
