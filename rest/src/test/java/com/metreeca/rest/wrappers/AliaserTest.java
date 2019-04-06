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

import com.metreeca.rest.*;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.Resource;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.rest.HandlerTest.echo;
import static com.metreeca.rest.ResponseAssert.assertThat;


final class AliaserTest {

	private void exec(final Runnable... tasks) {
		new Tray().exec(tasks).clear();
	}

	private Aliaser aliaser(final Resource canonical) {
		return new Aliaser((connection, request) ->
				request.path().equals("/alias") ? Optional.of(canonical) : Optional.empty()
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class AsWrapper {

		@Test void testRedirectAliasedItem() {
			exec(() -> aliaser(item("/canonical"))

					.wrap(echo())

					.handle(new Request().path("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.SeeOther)
							.hasHeader("Location", item("/canonical").stringValue())
					)
			);
		}

		@Test void testDelegateUnknownItemsToHandler() {
			exec(() -> aliaser(item("/canonical"))

					.wrap(echo())

					.handle(new Request().path("/unknown"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)
			);
		}

		@Test void testIgnoreBNodes() {
			exec(() -> aliaser(bnode())

					.wrap(echo())

					.handle(new Request().path("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)

			);
		}

	}

	@Nested final class AsHandler {

		@Test void testRedirectAliasedItem() {
			exec(() -> aliaser(item("/canonical"))

					.handle(new Request().path("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.SeeOther)
							.hasHeader("Location", item("/canonical").stringValue())
					)
			);
		}

		@Test void testReportUnknownItems() {
			exec(() -> aliaser(item("/canonical"))

					.handle(new Request().path("/unknown"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
					)
			);
		}

		@Test void testIgnoreBNodes() {
			exec(() -> aliaser(bnode())

					.handle(new Request().path("/alias"))

					.accept(response -> assertThat(response)
							.hasStatus(Response.NotFound)
					)

			);
		}

	}

}
