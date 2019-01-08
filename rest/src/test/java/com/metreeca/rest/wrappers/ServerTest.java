/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.tray.Tray;

import org.junit.jupiter.api.Test;

import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.formats.TextFormat.text;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;


final class ServerTest {

	//// Parameter Parsing /////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPreprocessQueryParameters() {
		new Tray().get(Server::new)

				.wrap((Handler)request -> {

					assertThat(request.parameters()).containsExactly(
							entry("one", singletonList("1")),
							entry("two", asList("2", "2"))
					);

					return request.reply(response -> response.status(OK));

				})

				.handle(new Request()
						.method(Request.GET)
						.query("one=1&two=2&two=2"))

				.accept(response -> assertThat(response.status()).isEqualTo(OK));
	}

	@Test void testPreprocessBodyParameters() {
		new Tray().get(Server::new)

				.wrap((Handler)request -> {

					assertThat(request.parameters()).containsExactly(
							entry("one", singletonList("1")),
							entry("two", asList("2", "2"))
					);

					return request.reply(response -> response.status(OK));

				})

				.handle(new Request()
						.method(Request.POST)
						.header("Content-Type", "application/x-www-form-urlencoded")
						.body(text(), "one=1&two=2&two=2"))

				.accept(response -> assertThat(response.status()).isEqualTo(OK));
	}

	@Test void testPreprocessDontOverwriteExistingParameters() {
		new Tray().get(Server::new)

				.wrap((Handler)request -> {

					assertThat(request.parameters()).containsExactly(
							entry("existing", singletonList("true"))
					);

					return request.reply(response -> response.status(OK));

				})

				.handle(new Request()
						.method(Request.GET)
						.query("one=1&two=2&two=2")
						.parameter("existing", "true"))

				.accept(response -> assertThat(response.status()).isEqualTo(OK));
	}

	@Test void testPreprocessQueryOnlyOnGET() {
		new Tray().get(Server::new)

				.wrap((Handler)request -> {

					assertThat(request.parameters()).isEmpty();

					return request.reply(response -> response.status(OK));

				})

				.handle(new Request()
						.method(Request.PUT)
						.query("one=1&two=2&two=2"))

				.accept(response -> assertThat(response.status()).isEqualTo(OK));
	}

	@Test void testPreprocessBodyOnlyOnPOST() {
		new Tray().get(Server::new)

				.wrap((Handler)request -> {

					assertThat(request.parameters()).isEmpty();

					return request.reply(response -> response.status(OK));

				})

				.handle(new Request()
						.method(Request.PUT)
						.header("Content-Type", "application/x-www-form-urlencoded")
						.body(text(), "one=1&two=2&two=2"))

				.accept(response -> assertThat(response.status()).isEqualTo(OK));
	}

}
