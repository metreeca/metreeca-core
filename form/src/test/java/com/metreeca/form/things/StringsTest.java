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

package com.metreeca.form.things;

import org.junit.Test;

import static com.metreeca.form.things.Strings.normalize;
import static com.metreeca.form.things.Strings.title;

import static org.junit.Assert.assertEquals;


public final class StringsTest {

	@Test public void testTitle() {

		assertEquals("words capitalized", "One-Two", title("one-two"));
		assertEquals("acronyms preserved", "WWW-Two", title("WWW-two"));

	}

	@Test public void testNormalize() {

		assertEquals("leading whitespaces trimmed", "head", normalize("\t \nhead"));
		assertEquals("trailing whitespaces trimmed", "tail", normalize("tail\t \n"));
		assertEquals("embedded whitespaces compacted", "head tail", normalize("head\t \ntail"));

	}

}
