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

package com.metreeca.rest.wrappers;

import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.rest.HandlerTest.echo;
import static com.metreeca.rest.ResponseAssert.assertThat;


final class AliaserTest {

	private void exec(final Runnable... tasks) {
		new Tray().exec(tasks).clear();
	}

	private Aliaser aliaser(final IRI canonical) {
		return new Aliaser((connection, request) ->
				request.path().equals("/alias") ? Optional.of(canonical) : Optional.empty()
		);
	}

	private Request request(final String path) {
		return new Request()
				.base(ValuesTest.Base)
				.path(path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class AsWrapper {

		@Test void testRedirectAliasedItem() {
			exec(() -> aliaser(item("/canonical"))

					.wrap(echo())

					.handle(request("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.SeeOther)
							.hasHeader("Location", item("/canonical").stringValue())
					)
			);
		}

		@Test void testForwardIdempotentItemsToHandler() {
			exec(() -> aliaser(item("/alias"))

					.wrap(echo())

					.handle(request("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)
			);
		}

		@Test void testReportUnknownItems() {
			exec(() -> aliaser(item("/canonical"))

					.wrap(echo())

					.handle(request("/unknown"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
					)
			);
		}

	}

	@Nested final class AsHandler {

		@Test void testRedirectAliasedItem() {
			exec(() -> aliaser(item("/canonical"))

					.handle(request("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.SeeOther)
							.hasHeader("Location", item("/canonical").stringValue())
					)
			);
		}

		@Test void testAcceptIdempotentItems() {
			exec(() -> aliaser(item("/alias"))

					.wrap(echo())

					.handle(request("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)
			);
		}

		@Test void testReportUnknownItems() {
			exec(() -> aliaser(item("/canonical"))

					.handle(request("/unknown"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
					)
			);
		}

	}

}
