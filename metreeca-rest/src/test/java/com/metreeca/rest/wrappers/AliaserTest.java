/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Demo.
 *
 * Metreeca/Demo is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Demo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Demo.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class AsWrapper {

		@Test void testRedirectAliasedItem() {
			exec(() -> aliaser("/canonical")

                    .wrap((Request request) -> request.reply(OK))

                    .handle(request("/alias"))

                    .accept(response -> assertThat(response)
                            .hasStatus(Response.SeeOther)
                            .hasHeader("Location", "/canonical")
                    )
            );
        }

        @Test void testForwardIdempotentItems() {
            exec(() -> aliaser("/alias")

                    .wrap((Request request) -> request.reply(OK))

                    .handle(request("/alias"))

                    .accept(response -> assertThat(response)
                            .hasStatus(OK)
                    )
            );
        }

        @Test void testForwardOtherItems() {
            exec(() -> aliaser("/canonical")

                    .wrap((Request request) -> request.reply(OK))

                    .handle(request("/other"))

                    .accept(response -> assertThat(response)
                            .hasStatus(OK)
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

        @Test void testReportIdempotentItems() {
            exec(() -> aliaser("/alias")

                    .handle(request("/alias"))

                    .accept(response -> assertThat(response)
                            .hasStatus(NotFound)
                    )
            );
        }

        @Test void testReportOtherItems() {
            exec(() -> aliaser("/canonical")

                    .handle(request("/other"))

                    .accept(response -> assertThat(response)
                            .hasStatus(NotFound)
                    )
            );
        }

	}

}
