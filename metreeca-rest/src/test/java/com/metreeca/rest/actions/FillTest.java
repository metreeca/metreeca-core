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

package com.metreeca.rest.actions;


import com.metreeca.rest.Toolbox;
import com.metreeca.rest.Xtream;

import org.junit.jupiter.api.Test;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toList;


final class FillTest {

	private void exec(final Runnable task) {
		new Toolbox().exec(task).clear();
	}


	private Stream<String> fill(final String template, final String name, final String value) {
		return new Fill<>()

				.model(template)
				.value(name, value)

				.apply(value);
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
        exec(() -> {
                    assertThat

                            (Xtream
		                            .of("test")

		                            .flatMap(new Fill<String>()

				                            .model("{base}:{x}{y}")
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
                            );
                }

		);
	}

}
