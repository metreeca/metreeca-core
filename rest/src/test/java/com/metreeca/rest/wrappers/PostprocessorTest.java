/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import org.junit.jupiter.api.Test;

import java.util.function.BiFunction;

import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.TextFormat.text;

import static org.assertj.core.api.Assertions.assertThat;


final class PostprocessorTest {

	private void exec(final Runnable... tasks) {
		new Context()
				.exec(tasks)
				.clear();
	}


	private Handler handler(final int status) {
		return request -> request.reply(response -> response.status(status).body(text(), ""));
	}

	private BiFunction<Response, String, String> post(final String value) {
		return (response, text) -> String.format("%s{%s}", text, value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testProcessResponsePayload() {
		exec(() -> handler(Response.OK)

				.with(new Postprocessor<>(text(), post("two")))
				.with(new Postprocessor<>(text(), post("one"))) // multiple filters to test piping

				.handle(new Request())

				.accept(response -> assertThat(response)
						.hasBody(text(), text -> assertThat(text)
								.as("filters executed")
								.isEqualTo("{one}{two}")
						)
				));
	}

	@Test void testIgnoreUnsuccessfulResponses() {
		exec(() -> handler(Response.NotFound)

				.with(new Postprocessor<>(text(), post("one")))

				.handle(new Request())

				.accept(response -> assertThat(response)
						.hasBody(text(), text -> assertThat(text)
								.isEmpty()
						)
				));

	}

}
