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

package com.metreeca.rest;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.Response.Unauthorized;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.Wrapper.roles;


final class WrapperTest {

	private void exec(final Runnable... tasks) {
		new Toolbox()
				.exec(tasks)
				.clear();
	}


	private Request request() {
		return new Request();
	}

	private Handler handler() {
		return request -> request.reply(status(OK));
	}


	@Nested final class Restricted {

		@Test void testAccepted() {
			exec(() -> roles("x", "y")

					.wrap(handler())

					.handle(request().roles("x"))

					.accept(response -> assertThat(response).hasStatus(OK))
			);
		}

		@Test void testRejected() {
			exec(() -> roles("x", "y")

					.wrap(handler())

					.handle(request().roles("z"))

					.accept(response -> assertThat(response).hasStatus(Unauthorized))
			);
		}

		@Test void testRejectedEmpty() {
			exec(() -> roles()

					.wrap(handler())

					.handle(request())

					.accept(response -> assertThat(response).hasStatus(Unauthorized))
			);
		}

	}

}
