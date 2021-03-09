/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.rdf4j.assets;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class ScribeCodeTest {

	private String format(final CharSequence text) {
		return new ScribeCode(new StringBuilder(10)).append(text).toString();
	}


	@Test void testIndentBraceBlocks() {

		assertThat(format("{\nuno\n}\ndue"))
				.as("indented block")
				.isEqualTo("{\n    uno\n}\ndue");

		assertThat(format("{ {\nuno\n} }\ndue"))
				.as("inline block")
				.isEqualTo("{ {\n    uno\n} }\ndue");

	}

	@Test void testIgnoreLeadingSpaces() {

		assertThat(format("  {\n\tuno\n  due\n }"))
				.as("single")
				.isEqualTo("{\n    uno\n    due\n}");

	}

	@Test void testCollapseSpaces() {

		assertThat(format(" text"))
				.as("leading")
				.isEqualTo("text");

		assertThat(format("uno  due"))
				.as("inside")
				.isEqualTo("uno due");

	}

	@Test void testCollapseNewlines() {

		assertThat(format("uno\ndue"))
				.as("single")
				.isEqualTo("uno\ndue");

		assertThat(format("uno\n\n\n\ndue"))
				.as("multiple")
				.isEqualTo("uno\n\ndue");

	}


}