/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.mill;

import org.junit.Test;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.assertEquals;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonMap;


public class _TemplateTest {

	@Test public void testPlainVariables() {

		assertEquals("matched", singleton("head name:value tail"),
				cast("head name:{name} tail", singletonMap("name", "value")));

		assertEquals("unmatched", singleton("head name: tail"),
				cast("head name:{name} tail", singletonMap("none", "value")));

	}

	@Test public void testMultipleVariables() {

		assertEquals("no variables", singleton("name:text"),
				cast("name:text", singletonMap("name", "value")));

		assertEquals("multiple", singleton("one:value, two:"),
				cast("one:{one}, two:{two}", singletonMap("one", "value")));

	}

	@Test public void testEncodedVariables() {

		assertEquals("encoded", singleton("http://a+b.com/?a%2Bb&a%2Bb=a%2Bb"),
				cast("http://{name}.com/?{name}&{name}={name}", singletonMap("name", "a+b")));

	}


	private Collection<String> cast(final String template, final Map<String, String> parameters) {
		return singleton(new _Template(template).fill(parameters::get));
	}

}
