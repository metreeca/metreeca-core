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

package com.metreeca.rest.actions;


import com.metreeca.rest.Context;
import com.metreeca.rest.Xtream;
import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static org.assertj.core.api.Assertions.assertThat;


final class StampTest {

	private void exec(final Runnable task) {
		new Context().exec(task).clear();
	}


	private Stream<String> fill(final String template, final String name, final String value) {
		return new Stamp<String>(template).value(name, value).apply(value);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testPlainVariables() {

		assertThat(fill("head name:{name} tail", "name", "value"))
				.as("matched")
				.containsExactly("head name:value tail");

		assertThat(fill("head name:{name} tail", "none", "value"))
				.as("unmatched")
				.containsExactly("head name: tail");

	}

	@Test void testMultipleVariables() {

		assertThat(fill("name:text", "name", "value"))
				.as("no variables")
				.containsExactly("name:text");

		assertThat(fill("one:{one}, two:{two}", "one", "value"))
				.as("multiple variables")
				.containsExactly("one:value, two:");

	}

	@Test void testModifiers() {

		assertThat(fill("http://{name}.com/?%{name}", "name", "a+b"))
				.as("encoded")
				.containsExactly("http://a+b.com/?a%2Bb");

		assertThat(fill("\\{name}={name}", "name", "value"))
				.as("escaped")
				.containsExactly("{name}=value");


	}


	@Test void testGenerateCartesianProduct() {
		exec(() -> assertThat

				(Xtream
						.of("test")

						.flatMap(new Stamp<String>("{base}:{x}{y}")
								.values("base", Stream::of)
								.values("x", string -> Stream.of("1", "2"))
								.values("y", string -> Stream.of("2", "3"))
						)

						.collect(toList())
				)

				.containsExactlyInAnyOrder(
						"test:12",
						"test:13",
						"test:22",
						"test:23"
				)

		);
	}

}
