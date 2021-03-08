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

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;

import org.junit.jupiter.api.Test;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.MovedPermanently;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.wrappers.Relocator.relocator;

final class RelocatorTest {

	private final Handler relocator=relocator()

			.rewrite("(http://example)(?:\\.\\w+)/(.*)", "$1.com/$2")
			.rewrite("http:(.*)", "https:$1")

			.wrap(request -> request.reply(status(OK)));


	@Test void testRelocate() {
		relocator

				.handle(new Request()
						.base("http://example.org/")
						.path("/path")
				)

				.accept(response -> assertThat(response)
						.hasStatus(MovedPermanently)
						.hasHeader("Location", "https://example.com/path")
				);
	}

	@Test void testForward() {
		relocator

				.handle(new Request()
						.base("https://example.com/")
						.path("/path")
				)

				.accept(response -> assertThat(response)
						.hasStatus(OK)
						.doesNotHaveHeader("Location")
				);
	}

}