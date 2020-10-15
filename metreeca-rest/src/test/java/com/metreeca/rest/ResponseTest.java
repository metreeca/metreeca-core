/*
 * Copyright © 2013-2020 Metreeca srl
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

import org.junit.jupiter.api.Test;

import static com.metreeca.rest.ResponseAssert.assertThat;


final class ResponseTest {

	@Test void testResolveLocationURL() {

		final Request request=new Request().base("http://example.com/base/").path("/path/");

		assertThat(new Response(request).header("Location", "http://absolute.com/name"))
				.as("absolute")
				.hasItem("http://absolute.com/name");

		assertThat(new Response(request).header("Location", "name"))
				.as("relative")
				.hasItem("http://example.com/base/path/name");

	}

}
