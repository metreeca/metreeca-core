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

import com.metreeca.rest.*;

import org.junit.jupiter.api.Test;

import static com.metreeca.rest.ResponseAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


final class RouterTest {

	private Request request(final String path) {
		return new Request().path(path);
	}

	private Handler handler() {
		return request -> request.reply(response -> response
				.status(Response.OK)
				.header("path", request.path())
		);
	}

	private Handler handler(final int id) {
		return request -> request.reply(response -> response.status(id));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testCheckPaths() {

		assertThatExceptionOfType(IllegalArgumentException.class).isThrownBy(() -> new Router()
				.path("one", handler(100))
		);

		assertThatExceptionOfType(IllegalStateException.class).isThrownBy(() -> new Router()
				.path("/one", handler(100))
				.path("/one", handler(100))
		);

	}

	@Test void testIgnoreUnknownPath() {
		new Router()

				.path("/one", handler(100))

				.handle(request("/two"))

				.accept(response -> assertThat(response)
						.as("request ignored").hasStatus(0));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testMatchesExactPath() {

		final Router router=new Router().path("/one", handler());

		router.handle(request("/one")).accept(response -> assertThat(response)
				.hasStatus(Response.OK).hasHeader("path", "/one"));

		router.handle(request("/one/")).accept(response -> assertThat(response)
				.hasStatus(Response.OK).hasHeader("path", "/one/"));

		router.handle(request("/one/two")).accept(response -> assertThat(response)
				.hasStatus(0));

	}

	@Test void testMatchesPrefixPath() {

		final Router router=new Router().path("/one/", handler());

		router.handle(request("/one")).accept(response -> assertThat(response)
				.hasStatus(Response.OK).hasHeader("path", "/one"));

		router.handle(request("/one/")).accept(response -> assertThat(response)
				.hasStatus(Response.OK).hasHeader("path", "/one/"));

		router.handle(request("/one/two")).accept(response -> assertThat(response)
				.hasStatus(Response.OK).hasHeader("path", "/one/two"));

	}

	@Test void testMatchesSubtreePath() {

		final Router router=new Router().path("/one/*", handler());

		router.handle(request("/one")).accept(response -> assertThat(response)
				.hasStatus(0));

		router.handle(request("/one/")).accept(response -> assertThat(response)
				.hasStatus(0));

		router.handle(request("/one/two")).accept(response -> assertThat(response)
				.hasStatus(Response.OK).hasHeader("path", "/one/two"));

	}


	@Test void testMatchesRootExactPath() {

		final Router router=new Router().path("/", handler());

		router.handle(request("/")).accept(response -> assertThat(response)
				.hasStatus(Response.OK).hasHeader("path", "/"));

		router.handle(request("/one")).accept(response -> assertThat(response)
				.hasStatus(0));

	}

	@Test void testMatchesRootSubtreePath() {

		final Router router=new Router().path("/*", handler());

		router.handle(request("/")).accept(response -> assertThat(response)
				.hasStatus(0));

		router.handle(request("/one")).accept(response -> assertThat(response)
				.hasStatus(Response.OK).hasHeader("path", "/one"));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testMatchExactPathIgnoringTrailingSlashes() {

		final Router router=new Router()

				.path("/one", handler(100))
				.path("/two/", handler(200));// trailing slash

		router.handle(request("/one")).accept(response ->
				assertThat(response).as("exact match without trailing slash").hasStatus(100));

		router.handle(request("/one/")).accept(response ->
				assertThat(response).as("exact match with trailing slash").hasStatus(100));

		router.handle(request("/two")).accept(response ->
				assertThat(response).as("exact match without trailing slash").hasStatus(200));

		router.handle(request("/two/")).accept(response ->
				assertThat(response).as("exact match with trailing slash").hasStatus(200));

		router.handle(request("/three/")).accept(response ->
				assertThat(response).as("no match").hasStatus(0));
	}

	@Test void testPreferExactMatch() {

		final int exact=100;
		final int prefix=200;

		final Router router=new Router()

				.path("/one", handler(exact))
				.path("/one/", handler(prefix));

		router.handle(request("/one")).accept(response -> assertThat(response).hasStatus(exact));
		router.handle(request("/one/")).accept(response -> assertThat(response).hasStatus(exact));
		router.handle(request("/one/two")).accept(response -> assertThat(response).hasStatus(prefix));

	}

	@Test void testPreferLongestPrefix() {

		final Router router=new Router()

				.path("/one/*", handler(100))
				.path("/one/two/*", handler(200));

		router.handle(request("/one/zero")).accept(response ->
				assertThat(response).as("prefix match").hasStatus(100));

		router.handle(request("/one/two/zero")).accept(response ->
				assertThat(response).as("longest prefix match").hasStatus(200));

	}

	@Test void testPreferLexicographicallyLesserPrefix() {

		final Router router=new Router()

				.path("/one/*", handler(100))
				.path("/two/*", handler(200));// trailing slash

		router.handle(request("/one/zero"))
				.accept(response -> assertThat(response.status()).isEqualTo(100));

		router.handle(request("/two/zero"))
				.accept(response -> assertThat(response.status()).isEqualTo(200));

	}

}
