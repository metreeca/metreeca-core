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

package com.metreeca.link;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static com.metreeca.spec.things.Lists.list;
import static com.metreeca.spec.things.Maps.entry;
import static com.metreeca.spec.things.Maps.map;

import static org.junit.Assert.assertEquals;


public class RequestTest {

	@Test public void testGetParametersFromQuery() {

		assertEquals("empty", map(

		), parameters(""));

		assertEquals("singleton", map(
				entry("uno", list("one"))
		), parameters("uno=one"));

		assertEquals("multiple", map(
				entry("uno", list("one")),
				entry("due", list("two", "dos"))
		), parameters("uno=one&due=two&due=dos"));

		assertEquals("missing label", map(entry("", list("uno"))), parameters("=uno"));
		assertEquals("missing value", map(entry("uno", list(""))), parameters("uno"));

		assertEquals("encoded", map(entry("a b", list("a b"))), parameters("a+b=a%20b"));

	}

	@Test public void testClearCachedParametersOnQueryUpdate() {

		final Request request=new Request();

		assertEquals("original", map(entry("x", list("y"))), request.setQuery("x=y").getParameters());
		assertEquals("updated", map(entry("x", list("z"))), request.setQuery("x=z").getParameters());

	}

	@Test public void testGetParametersFromBody() {

		final Request post=new Request()
				.setMethod(Request.POST)
				.setHeader("Content-Type", "application/x-www-form-urlencoded")
				.setText("uno=one&due=two&due=dos");

		assertEquals("posted form", map(
				entry("uno", list("one")),
				entry("due", list("two", "dos"))
		), post.getParameters());

		assertEquals("updated query on post form", map(
				entry("uno", list("one"))
		), post.setQuery("uno=one").getParameters());

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<String, List<String>> parameters(final String query) {
		return new Request().setQuery(query).getParameters();
	}

}
