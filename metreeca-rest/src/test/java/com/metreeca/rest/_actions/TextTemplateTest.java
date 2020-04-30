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

package com.metreeca.rest._actions;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.singletonMap;


final class TextTemplateTest {

	private String fill(final String template, final String name, final String value) {
		return new TextTemplate(template).fill(singletonMap(name, value));
	}


	@Test void testPlainVariables() {

		assertThat(fill("head name:{name} tail", "name", "value"))
				.as("matched")
				.isEqualTo("head name:value tail");

		assertThat(fill("head name:{name} tail", "none", "value"))
				.as("unmatched")
				.isEqualTo("head name: tail");

	}

	@Test void testMultipleVariables() {

		assertThat(fill("name:text", "name", "value"))
				.as("no variables")
				.isEqualTo("name:text");

		assertThat(fill("one:{one}, two:{two}", "one", "value"))
				.as("multiple variables")
				.isEqualTo("one:value, two:");

	}

	@Test void testModifiers() {

		assertThat(fill("http://{name}.com/?%{name}", "name", "a+b"))
				.as("encoded")
				.isEqualTo("http://a+b.com/?a%2Bb");

		assertThat(fill("\\{name}={name}", "name", "value"))
				.as("escaped")
				.isEqualTo("{name}=value");


	}


}
