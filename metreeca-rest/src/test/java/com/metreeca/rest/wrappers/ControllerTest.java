/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static com.metreeca.rest.ResponseAssert.assertThat;


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
		exec(() -> Controller.controller()

				.wrap(handler())

				.handle(request())

				.accept(response -> assertThat(response).hasStatus(Response.OK))
		);
	}

	@Test void testAcceptedControlled() {
		exec(() -> Controller.controller("x", "y")

				.with((Wrapper)handler -> request -> {

					Assertions.assertThat(request.roles()).containsOnly("x");

					return handler.handle(request);

				})

				.wrap(handler())

				.handle(request().roles("x"))

				.accept(response -> assertThat(response).hasStatus(Response.OK))
		);
	}

	@Test void testUnauthorized() {
		exec(() -> Controller.controller("x", "y")

				.wrap(handler())

				.handle(request().roles("z"))

				.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))
		);
	}

}
