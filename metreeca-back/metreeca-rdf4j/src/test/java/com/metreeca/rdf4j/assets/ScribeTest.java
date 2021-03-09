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

import static com.metreeca.rdf4j.assets.Scribe.code;
import static com.metreeca.rdf4j.assets.Scribe.text;

import static org.assertj.core.api.Assertions.assertThat;


final class ScribeTest {

	@Test void testTemplate() { // !!! empty placeholders // missing args // redundant args

		assertThat(code(text("verbatim"))).isEqualTo("verbatim");

		//assertThat(code(Snippets.text(
		//		"<< {article} a {object} >>",  Scribe.text("string")
		//))).isEqualTo("<< a string >>");

		assertThat(code(text(
				"<< {reused} {reused} >>", text("text")
		))).isEqualTo("<< text text >>");

	}

}
