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

package com.metreeca.link;

import org.junit.Test;

import java.util.Optional;

import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.Tray.tray;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;


public class IndexTest {

	@Test public void testMatchExactPathIgnoringTrailingSlashes() {
		tray().exec(() -> {

			final Index index=tool(Index.Tool);

			final Handler one=handler("1");
			final Handler two=handler("2");

			index.insert("/one", one);
			index.insert("/two/", two); // trailing slash

			assertEquals("exact match one", Optional.of(one), index.lookup("/one"));
			assertEquals("exact match one (trailing slash)", Optional.of(one), index.lookup("/one/"));

			assertEquals("exact match two", Optional.of(two), index.lookup("/two"));
			assertEquals("exact match two (trailing slash)", Optional.of(two), index.lookup("/two/"));

			assertFalse("no match", index.lookup("/zero").isPresent());

		}).clear();
	}

	@Test public void testMatchLongestPrefix() {
		tray().exec(() -> {

			final Index index=tool(Index.Tool);

			final Handler one=handler("1");
			final Handler two=handler("1.2");

			index.insert("/one/*", one);
			index.insert("/one/two/*", two);

			assertEquals("prefix match", Optional.of(one), index.lookup("/one/zero"));
			assertEquals("longest prefix match", Optional.of(two), index.lookup("/one/two/zero"));

		}).clear();
	}

	@Test public void testManagesPrefixesOfEqualLength() {
		tray().exec(() -> {

			final Index index=tool(Index.Tool);

			final Handler one=handler("1");
			final Handler two=handler("2");

			index.insert("/one/*", one);
			index.insert("/two/*", two);

			assertEquals("prefix match one", Optional.of(one), index.lookup("/one/zero"));
			assertEquals("prefix match two", Optional.of(two), index.lookup("/two/zero"));

		}).clear();
	}

	@Test public void testNormalizePaths() {
		tray().exec(() -> {

			final Index index=tool(Index.Tool);

			final Handler collection=handler("collection");
			final Handler resource=handler("resource");

			index.insert("/uno/", collection);
			index.insert("/uno/*", resource);

			assertEquals("collection match", Optional.of(collection), index.lookup("/uno"));
			assertEquals("collection match trailing slash", Optional.of(collection), index.lookup("/uno/"));

			assertEquals("resource match", Optional.of(resource), index.lookup("/uno/due"));

		}).clear();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Handler handler(final String id) {
		return new Handler() {

			@Override public void handle(final Request request, final Response response) {
				response.text(id);
			}

			@Override public String toString() {
				return "handler <"+id+">";
			}

		};
	}

}
