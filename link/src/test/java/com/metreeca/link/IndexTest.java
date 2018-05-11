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

import com.metreeca.tray.Tool;
import com.metreeca.tray.Tray;

import org.junit.Test;

import java.util.function.BiConsumer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;


public class IndexTest {

	@Test public void testMatchExactPathIgnoringTrailingSlashes() {

		final Index index=new Index(Tray.tray());

		final Handler one=handler("1");
		final Handler two=handler("2");

		index.insert("/one", one);
		index.insert("/two/", two); // trailing slash

		assertEquals("exact match one", one, index.get("/one"));
		assertEquals("exact match one (trailing slash)", one, index.get("/one/"));

		assertEquals("exact match two", two, index.get("/two"));
		assertEquals("exact match two (trailing slash)", two, index.get("/two/"));

		assertNull("no match", index.get("/zero"));

	}

	@Test public void testMatchLongestPrefix() {

		final Index index=new Index(Tray.tray());

		final Handler one=handler("1");
		final Handler two=handler("1.2");

		index.insert("/one/*", one);
		index.insert("/one/two/*", two);

		assertEquals("prefix match", one, index.get("/one/zero"));
		assertEquals("longest prefix match", two, index.get("/one/two/zero"));
	}

	@Test public void testManagesPrefixesOfEqualLength() {

		final Index index=new Index(Tray.tray());

		final Handler one=handler("1");
		final Handler two=handler("2");

		index.insert("/one/*", one);
		index.insert("/two/*", two);

		assertEquals("prefix match one", one, index.get("/one/zero"));
		assertEquals("prefix match two", two, index.get("/two/zero"));

	}

	@Test public void testNormalizePaths() {

		final Index index=new Index(Tray.tray());

		final Handler collection=handler("collection");
		final Handler resource=handler("resource");

		index.insert("/uno/", collection);
		index.insert("/uno/*", resource);

		assertEquals("collection match", collection, index.get("/uno"));
		assertEquals("collection match trailing slash", collection, index.get("/uno/"));

		assertEquals("resource match", resource, index.get("/uno/due"));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Handler handler(final String id) {
		return new Handler() {

			@Override public void handle(final Tool.Loader tools,
					final Request request, final Response response, final BiConsumer<Request, Response> sink) {
				sink.accept(request, response.setText(id));
			}

			@Override public String toString() {
				return "handler <"+id+">";
			}

		};
	}

}
