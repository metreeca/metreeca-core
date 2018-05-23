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

package com.metreeca.tray.sys;

import com.metreeca.tray.IO;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.IOException;
import java.io.StringReader;

import static com.metreeca.spec.things.Values.iri;

import static org.junit.Assert.assertEquals;


public final class _CacheTest {

	@Rule public TemporaryFolder tmp=new TemporaryFolder();


	private _Cache cache() throws IOException {
		return new _Cache(tmp.newFolder());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testRetrieveSetContentForOpaqueURLs() throws IOException {

		final _Cache cache=cache();
		final String url=iri(iri());

		final String expected="text!";

		cache.set(url, new StringReader(expected));

		final String actual=IO.text(cache.get(url).reader());

		assertEquals("", expected, actual);
	}

}
