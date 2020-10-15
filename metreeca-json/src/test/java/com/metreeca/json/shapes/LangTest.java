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

package com.metreeca.json.shapes;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.shapes.Lang.lang;
import static com.metreeca.json.shapes.Or.or;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

final class LangTest {

	@Test void testRejectEmptyTags() {
		assertThatIllegalArgumentException().isThrownBy(() -> lang(""));
	}

	@Nested final class Optimization {

		@Test void testIgnoreEmptyValueSet() {
			assertThat(lang()).isEqualTo(or());
		}

		@Test void testCollapseDuplicates() {
			assertThat(lang("en", "it", "en")).isEqualTo(lang("en", "it"));
		}

	}

}