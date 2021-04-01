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

import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;


final class AliaserTest {

	private void exec(final Runnable... tasks) {
		new Toolbox()
				.exec(tasks)
				.clear();
	}

	private Aliaser aliaser(final String canonical) {
		return Aliaser.aliaser(request ->
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
