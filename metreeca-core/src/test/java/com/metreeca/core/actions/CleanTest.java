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

package com.metreeca.core.actions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class CleanTest {

	@Test void testSpace() {

		final String text="  leading \t\u00A0trailing\n\r";

		assertThat(new Clean().space(true).apply(text)).isEqualTo("leading trailing");
		assertThat(new Clean().space(false).apply(text)).isEqualTo(text);
	}

	@Test void testMarks() {

		final String text="èé";

		assertThat(new Clean().marks(true).apply(text)).isEqualTo("ee");
		assertThat(new Clean().marks(false).apply(text)).isEqualTo(text);
	}

	@Test void testSmart() {

		final String text="‘’“”‹› ";

		assertThat(new Clean().smart(true).apply(text)).isEqualTo("''\"\"<> ");
		assertThat(new Clean().smart(false).apply(text)).isEqualTo(text);
	}

}