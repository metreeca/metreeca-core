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

package com.metreeca.tree.shapes;

import org.junit.jupiter.api.Test;

import static com.metreeca.tree.shapes.Like.like;
import static org.assertj.core.api.Assertions.assertThat;


final class LikeTest {

	private String expression(final String keywords) {
		return like(keywords, true).toExpression();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testCompileExpression() {
		assertThat(" one two three ")

				.as("no keywords").matches(expression(""))
				.as("junk only").matches(expression("--"))

				.as("single stem").matches(expression("tw"))
				.as("multiple stems").matches(expression("tw th"))

				.as("leading junk").matches(expression("--tw"))
				.as("trailing junk").matches(expression("tw--"))
				.as("middle junk").matches(expression("tw--th"))

				.as("unknown stem").doesNotMatch(expression("x"))
				.as("missorted stems").doesNotMatch(expression("th tw"));
	}

}
