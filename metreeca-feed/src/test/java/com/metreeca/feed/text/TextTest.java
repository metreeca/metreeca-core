/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.feed.text;


import com.metreeca.feed.Feed;
import com.metreeca.rest.Context;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;


final class TextTest {

	private void exec(final Runnable task) {
		new Context().exec(task).clear();
	}


	@Test void test() {
		exec(() -> Feed.of("test")

				.flatMap(new Text<String>("{base}:{x}{y}")
						.parameter("base", Stream::of)
						.parameter("x", string -> Stream.of("1", "2"))
						.parameter("y", string -> Stream.of("2", "3"))
				)

				.pipe(items -> {

					assertThat(items).containsExactlyInAnyOrder(
							"test:12",
							"test:13",
							"test:22",
							"test:23"
					);

					return Stream.empty();

				})

				.sink()

		);
	}

}
