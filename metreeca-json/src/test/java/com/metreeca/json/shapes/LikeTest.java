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

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;


final class LikeTest {

	private String keywords(final CharSequence keywords) {
		return Like.keywords(keywords, true);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testCompileExpression() {
		assertThat(" one two three ")

				.as("no keywords").matches(keywords(""))
				.as("junk only").matches(keywords("--"))

				.as("single stem").matches(keywords("tw"))
				.as("multiple stems").matches(keywords("tw th"))

				.as("leading junk").matches(keywords("--tw"))
				.as("trailing junk").matches(keywords("tw--"))
				.as("middle junk").matches(keywords("tw--th"))

				.as("unknown stem").doesNotMatch(keywords("x"))
				.as("missorted stems").doesNotMatch(keywords("th tw"));
	}

}
