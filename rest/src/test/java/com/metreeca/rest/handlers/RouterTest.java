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

import static com.metreeca.rest.ResponseAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


final class RouterTest {

	private Handler handler(final int id) {
		return request -> request.reply(response -> response.status(id));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testCheckPaths() {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy( () -> new Router()
				.path("one", handler(100))
		);

		assertThatExceptionOfType(IllegalStateException.class).isThrownBy( () -> new Router()
				.path("/one", handler(100))
				.path("/one", handler(100))
		);

	}

	@Test void testIgnoreUnknownPath() {
		new Router()

				.path("/one", handler(100))

				.handle(new Request()
						.path("/two"))

				.accept(response -> assertThat(response)
						.as("request ignored").hasStatus(0));
	}

	@Test void testRewriteBaseAndPath() {

		new Router()

				.path("/one", request -> { // exact match


					assertThat(request.base()).as("base not rewritten").isEqualTo("http://example.org/");
					assertThat(request.path()).as("path not rewritten").isEqualTo("/one/");

					return request.reply(response -> response.status(Response.OK));

				})

				.handle(new Request()
						.base("http://example.org/")
						.path("/one/"))

				.accept(response -> assertThat(response)
								.as("request processed").hasStatus(Response.OK));

		new Router()

				.path("/one/*", request -> { // prefix match


					assertThat(request.base()).as("base rewritten").isEqualTo("http://example.org/one/");
					assertThat(request.path()).as("path rewritten").isEqualTo("/two/three");

					return request.reply(response -> response.status(Response.OK));

				})

				.handle(new Request()
						.base("http://example.org/")
						.path("/one/two/three"))

				.accept(response -> assertThat(response.status())
						.as("request processed").isEqualTo(Response.OK));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testMatchExactPathIgnoringTrailingSlashes() {

		final Router router=new Router()

				.path("/one", handler(100))
				.path("/two/", handler(200));// trailing slash

		router.handle(new Request().path("/one")).accept(response ->
				assertThat(response).as("exact match without trailing slash").hasStatus(100));

		router.handle(new Request().path("/one/")).accept(response ->
				assertThat(response).as("exact match with trailing slash").hasStatus(100));

		router.handle(new Request().path("/two")).accept(response ->
				assertThat(response).as("exact match without trailing slash").hasStatus(200));

		router.handle(new Request().path("/two/")).accept(response ->
				assertThat(response).as("exact match with trailing slash").hasStatus(200));

		router.handle(new Request().path("/three/")).accept(response ->
				assertThat(response).as("no match").hasStatus(0));
	}

	@Test void testMatchLongestPrefix() {

		final Router router=new Router()

				.path("/one/*", handler(100))
				.path("/one/two/*", handler(200));// trailing slash

		router.handle(new Request().path("/one/zero")).accept(response ->
				assertThat(response).as("prefix match").hasStatus(100));

		router.handle(new Request().path("/one/two/zero")).accept(response ->
						assertThat(response).as("longest prefix match").hasStatus(200));

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

		router.handle(new Request().path("/one")).accept(response ->
						assertThat(response).as("collection match").hasStatus(100));

		router.handle(new Request().path("/one/")).accept(response ->
						assertThat(response).as("collection match trailing slash").hasStatus(100));

		router.handle(new Request().path("/one/two")).accept(response ->
						assertThat(response).as("resource match").hasStatus(500));

	}

}
