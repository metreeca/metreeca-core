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

import com.metreeca.rest.Context;
import com.metreeca.rest.Request;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static java.util.Collections.emptyList;

final class JSONLDFormatTest {

	private void exec(final Runnable... tasks) {
		new Context().exec(tasks).clear();
	}


	@Nested final class Encoder {

		@Test void testHandleGenericRequests() {
			exec(() -> new Request()

					.reply(response -> response.status(OK)
							.body(jsonld(), emptyList())
					)

					.accept(response -> assertThat(response)
							.hasHeader("Content-Type", JSONFormat.MIME)
					)
			);
		}

		@Test void testHandlePlainJSONRequests() {
			exec(() -> new Request()

					.header("Accept", JSONFormat.MIME)

					.reply(response -> response.status(OK)
							.body(jsonld(), emptyList())
					)

					.accept(response -> assertThat(response)
							.hasHeader("Content-Type", JSONFormat.MIME)
					)
			);
		}

		@Test void testHandleJSONLDRequests() {
			exec(() -> new Request()

					.header("Accept", JSONLDFormat.MIME)

					.reply(response -> response.status(OK)
							.body(jsonld(), emptyList())
					)

					.accept(response -> assertThat(response)
							.hasHeader("Content-Type", JSONLDFormat.MIME)
					)
			);
		}

	}

}