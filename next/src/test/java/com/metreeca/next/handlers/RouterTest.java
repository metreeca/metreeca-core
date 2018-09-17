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

package com.metreeca.next.handlers;

import com.metreeca.next.Handler;
import com.metreeca.next.formats.Text;

import org.junit.jupiter.api.Test;


final class RouterTest {

	@Test void testMatchExactPathIgnoringTrailingSlashes() {

		//final String one="1";
		//final String two="2";
		//
		//final Router router=new Router()
		//
		//		.path("/one", handler(one))
		//		.path("/two/", handler(two));// trailing slash
		//
		//router.handle(new Request().path("/one")).accept(response ->
		//		assertEquals(one, response., "exact match one")
		//);


		//assertEquals("exact match one (trailing slash)", Optional.of(one), index.lookup("/one/"));

		//assertEquals("exact match two", Optional.of(two), index.lookup("/two"));
		//assertEquals("exact match two (trailing slash)", Optional.of(two), index.lookup("/two/"));

		//assertFalse("no match", index.lookup("/zero").isPresent());
	}

	//@Test void testMatchLongestPrefix() {
	//
	//
	//	final Handler one=handler("1");
	//	final Handler two=handler("1.2");
	//
	//	index.insert("/one/*", one);
	//	index.insert("/one/two/*", two);
	//
	//	assertEquals("prefix match", Optional.of(one), index.lookup("/one/zero"));
	//	assertEquals("longest prefix match", Optional.of(two), index.lookup("/one/two/zero"));
	//
	//}

	//@Test void testManagesPrefixesOfEqualLength() {
	//
	//	final Handler one=handler("1");
	//	final Handler two=handler("2");
	//
	//	index.insert("/one/*", one);
	//	index.insert("/two/*", two);
	//
	//	assertEquals("prefix match one", Optional.of(one), index.lookup("/one/zero"));
	//	assertEquals("prefix match two", Optional.of(two), index.lookup("/two/zero"));
	//
	//}

	//@Test public void testNormalizePaths() {
	//
	//	final Handler collection=handler("collection");
	//	final Handler resource=handler("resource");
	//
	//	index.insert("/uno/", collection);
	//	index.insert("/uno/*", resource);
	//
	//	assertEquals("collection match", Optional.of(collection), index.lookup("/uno"));
	//	assertEquals("collection match trailing slash", Optional.of(collection), index.lookup("/uno/"));
	//
	//	assertEquals("resource match", Optional.of(resource), index.lookup("/uno/due"));
	//
	//}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Handler handler(final String id) {
		return request -> request.reply(response -> response.body(Text.Format, id));
	}

}
