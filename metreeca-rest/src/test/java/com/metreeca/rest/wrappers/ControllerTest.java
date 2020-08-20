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

import com.metreeca.rest.*;
import org.junit.jupiter.api.Test;

import static com.metreeca.rest.ResponseAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;


final class ControllerTest {

	private void exec(final Runnable... tasks) {
		new Context()
				.exec(tasks)
				.clear();
	}


	private Request request() {
		return new Request();
	}

	private Handler handler() {
		return request -> request.reply(response -> response.status(Response.OK));
	}


	@Test void testAcceptedPublic() {
		exec(() -> new Controller()

				.wrap(handler())

				.handle(request())

				.accept(response -> assertThat(response).hasStatus(Response.OK))
		);
	}

	@Test void testAcceptedControlled() {
		exec(() -> new Controller("x", "y")

				.wrap((Wrapper)handler -> request -> {

					assertThat(request.roles()).containsOnly("x");

					return handler.handle(request);

				})

				.wrap(handler())

				.handle(request().roles("x"))

				.accept(response -> assertThat(response).hasStatus(Response.OK))
		);
	}

	@Test void testUnauthorized() {
		exec(() -> new Controller("x", "y")

				.wrap(handler())

				.handle(request().roles("z"))

				.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))
		);
	}

}
