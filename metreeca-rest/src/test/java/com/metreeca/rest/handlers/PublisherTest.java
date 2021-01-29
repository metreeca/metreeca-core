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

package com.metreeca.rest.handlers;

import com.metreeca.rest.Request;

import org.junit.jupiter.api.Test;

import static com.metreeca.rest.Request.POST;
import static com.metreeca.rest.Response.MethodNotAllowed;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.handlers.Publisher.publisher;
import static com.metreeca.rest.handlers.Publisher.variants;
import static org.assertj.core.api.Assertions.assertThat;

final class PublisherTest {

	@Test void testVariants() {

		assertThat(variants("link")).containsExactly("link", "link.html");
		assertThat(variants("link.html")).containsExactly("link.html");

		assertThat(variants("path/link")).containsExactly("path/link", "path/link.html");
		assertThat(variants("path/link.html")).containsExactly("path/link.html");

		assertThat(variants("path/link#hash")).containsExactly("path/link#hash", "path/link.html#hash");
		assertThat(variants("path/link.html#hash")).containsExactly("path/link.html#hash");

		assertThat(variants(".")).containsExactly("index.html");
		assertThat(variants("path/")).containsExactly("path/index.html");
		assertThat(variants("path/#hash")).containsExactly("path/index.html#hash");

		assertThat(variants("reindex")).containsExactly("reindex", "reindex.html");

	}

	@Test void testAcceptOnlyGETRequests() {
		publisher(getClass().getResource("/"))

				.handle(new Request().method(POST))

				.accept(response -> assertThat(response)
						.hasStatus(MethodNotAllowed)
				);
	}

}