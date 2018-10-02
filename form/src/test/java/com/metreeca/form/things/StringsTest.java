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

import org.junit.jupiter.api.Test;

import static com.metreeca.form.things.Strings.normalize;
import static com.metreeca.form.things.Strings.title;

import static org.assertj.core.api.Assertions.assertThat;


final class StringsTest {

	@Test void testTitle() {

		assertThat((Object)"One-Two").as("words capitalized").isEqualTo(title("one-two"));
		assertThat((Object)"WWW-Two").as("acronyms preserved").isEqualTo(title("WWW-two"));

	}

	@Test void testNormalize() {

		assertThat((Object)"head").as("leading whitespaces trimmed").isEqualTo(normalize("\t \nhead"));
		assertThat((Object)"tail").as("trailing whitespaces trimmed").isEqualTo(normalize("tail\t \n"));
		assertThat((Object)"head tail").as("embedded whitespaces compacted").isEqualTo(normalize("head\t \ntail"));

	}

}
