/*
 * Copyright © 2013-2020 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
