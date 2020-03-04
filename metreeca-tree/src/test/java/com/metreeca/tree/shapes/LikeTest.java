/*
 * Copyright © 2019-2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.tree.shapes;

import org.junit.jupiter.api.Test;

import static com.metreeca.tree.shapes.Like.like;

import static org.assertj.core.api.Assertions.assertThat;


final class LikeTest {

	private String expression(final String keywords) {
		return like(keywords).toExpression();
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
