/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.junit.jupiter.api.Test;

import static com.google.common.truth.Truth.assertThat;
import static com.google.common.truth.Truth.assertWithMessage;
import static org.junit.jupiter.api.Assertions.assertThrows;


final class RouterTest {

	private Handler handler(final int id) {
		return request -> request.reply(response -> response.status(id));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testCheckPaths() { // !!! migrate to truth

		assertThrows(IllegalArgumentException.class, () -> new Router()
				.path("one", handler(100))
		);

		assertThrows(IllegalStateException.class, () -> new Router()
				.path("/one", handler(100))
				.path("/one", handler(100))
		);

	}

	@Test void testIgnoreUnknownPath() {
		new Router()

				.path("/one", handler(100))

				.handle(new Request()
						.path("/two"))

				.accept(response -> assertWithMessage("request ignored")
						.that(response.status()).isEqualTo(0));
	}

	@Test void testRewriteBaseAndPath() {

		new Router()

				.path("/one", request -> { // exact match

					assertWithMessage("base not rewritten")
							.that(request.base()).isEqualTo("http://example.org/");

					assertWithMessage("path not rewritten")
							.that(request.path()).isEqualTo("/one/");

					return request.reply(response -> response.status(Response.OK));

				})

				.handle(new Request()
						.base("http://example.org/")
						.path("/one/"))

				.accept(response -> assertWithMessage("request processed")
						.that(response.status()).isEqualTo(Response.OK));

		new Router()

				.path("/one/*", request -> { // prefix match

					assertWithMessage("base rewritten")
							.that(request.base()).isEqualTo("http://example.org/one/");

					assertWithMessage("path rewritten")
							.that(request.path()).isEqualTo("/two/three");

					return request.reply(response -> response.status(Response.OK));

				})

				.handle(new Request()
						.base("http://example.org/")
						.path("/one/two/three"))

				.accept(response -> assertWithMessage("request processed")
						.that(response.status()).isEqualTo(Response.OK));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testMatchExactPathIgnoringTrailingSlashes() {

		final Router router=new Router()

				.path("/one", handler(100))
				.path("/two/", handler(200));// trailing slash

		router.handle(new Request().path("/one")).accept(response ->
				assertWithMessage("exact match without trailing slash")
						.that(response.status()).isEqualTo(100));

		router.handle(new Request().path("/one/")).accept(response ->
				assertWithMessage("exact match with trailing slash")
						.that(response.status()).isEqualTo(100));

		router.handle(new Request().path("/two")).accept(response ->
				assertWithMessage("exact match without trailing slash")
						.that(response.status()).isEqualTo(200));

		router.handle(new Request().path("/two/")).accept(response ->
				assertWithMessage("exact match with trailing slash")
						.that(response.status()).isEqualTo(200));

		router.handle(new Request().path("/three/")).accept(response ->
				assertWithMessage("no match")
						.that(response.status()).isEqualTo(0));
	}

	@Test void testMatchLongestPrefix() {

		final Router router=new Router()

				.path("/one/*", handler(100))
				.path("/one/two/*", handler(200));// trailing slash

		router.handle(new Request().path("/one/zero")).accept(response ->
				assertWithMessage("prefix match")
						.that(response.status()).isEqualTo(100));

		router.handle(new Request().path("/one/two/zero"))
				.accept(response -> assertWithMessage("longest prefix match")
						.that(response.status()).isEqualTo(200));

	}

	@Test void testManagesPrefixesOfEqualLength() {

		final Router router=new Router()

				.path("/one/*", handler(100))
				.path("/two/*", handler(200));// trailing slash

		router.handle(new Request().path("/one/zero"))
				.accept(response -> assertThat(response.status()).isEqualTo(100));

		router.handle(new Request().path("/two/zero"))
				.accept(response -> assertThat(response.status()).isEqualTo(200));

	}

	@Test void testNormalizePaths() {

		final Router router=new Router()

				.path("/one/", handler(100))
				.path("/one/*", handler(500));// trailing slash

		router.handle(new Request().path("/one"))
				.accept(response -> assertWithMessage("collection match")
						.that(response.status()).isEqualTo(100));

		router.handle(new Request().path("/one/"))
				.accept(response -> assertWithMessage("collection match trailing slash")
						.that(response.status()).isEqualTo(100));

		router.handle(new Request().path("/one/two"))
				.accept(response -> assertWithMessage("resource match")
						.that(response.status()).isEqualTo(500));

	}

}
