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

package com.metreeca.core.wrappers;

import com.metreeca.core.*;

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.Response.OK;
import static com.metreeca.core.ResponseAssert.assertThat;


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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRedirectAliasedItem() {
		exec(() -> aliaser("/canonical")

				.wrap((Request request) -> request.reply(status(OK)))

				.handle(request("/alias"))

				.accept(response -> assertThat(response)
						.hasStatus(Response.SeeOther)
						.hasHeader("Location", "/canonical")
				)
		);
	}

	@Test void testForwardIdempotentItems() {
		exec(() -> aliaser("/alias")

				.wrap((Request request) -> request.reply(status(OK)))

				.handle(request("/alias"))

				.accept(response -> assertThat(response)
						.hasStatus(OK)
				)
		);
	}

	@Test void testForwardOtherItems() {
		exec(() -> aliaser("/canonical")

				.wrap((Request request) -> request.reply(status(OK)))

				.handle(request("/other"))

				.accept(response -> assertThat(response)
						.hasStatus(OK)
				)
		);
	}

}
