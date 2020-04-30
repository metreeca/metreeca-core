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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.Context;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.rest.ResponseAssert.assertThat;


final class AliaserTest {

	private void exec(final Runnable... tasks) {
		new Context()
				.exec(tasks)
				.clear();
	}

	private Aliaser aliaser(final String canonical) {
		return new Aliaser(request ->
				request.path().equals("/alias") ? Optional.of(canonical) : Optional.empty()
		);
	}

	private Request request(final String path) {
		return new Request()
				.base("http://example.com/")
				.path(path);
	}

	private Handler handler() {
		return (Request request) -> request.reply(response -> response.status(Response.OK));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class AsWrapper {

		@Test void testRedirectAliasedItem() {
			exec(() -> aliaser("/canonical")

					.wrap(handler())

					.handle(request("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.SeeOther)
							.hasHeader("Location", "/canonical")
					)
			);
		}

		@Test void testForwardIdempotentItemsToHandler() {
			exec(() -> aliaser("/alias")

					.wrap(handler())

					.handle(request("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)
			);
		}

		@Test void testReportUnknownItems() {
			exec(() -> aliaser("/canonical")

					.wrap(handler())

					.handle(request("/unknown"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
					)
			);
		}

	}

	@Nested final class AsHandler {

		@Test void testRedirectAliasedItem() {
			exec(() -> aliaser("/canonical")

					.handle(request("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.SeeOther)
							.hasHeader("Location", "/canonical")
					)
			);
		}

		@Test void testAcceptIdempotentItems() {
			exec(() -> aliaser("/alias")

					.wrap(handler())

					.handle(request("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)
			);
		}

		@Test void testReportUnknownItems() {
			exec(() -> aliaser("/canonical")

					.handle(request("/unknown"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
					)
			);
		}

	}

}
