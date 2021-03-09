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

import static com.metreeca.rdf4j.assets.Scribe.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;


final class ScribeTest {

	@Test void testNothing() {

		final Object x=new Object();
		final Object y=new Object();

		assertThat(code(nothing(id(x, y))))
				.as("no code generated")
				.isEmpty();

		assertThat(code(nothing(id(x, y)), id(x), text("="), id(y)))
				.as("snippets evaluated with side effects")
				.matches("(\\d+)=\\1");

	}

	@Test void testId() {

		final Object x=new Object();
		final Object y=new Object();
		final Object z=new Object();

		assertThat(code(id(x)))
				.as("formatted")
				.matches("\\d+");

		assertThat(code(id(x), text("="), id(x)))
				.as("idempotent")
				.matches("(\\d+)=\\1");

		assertThat(code(id(x), text("!="), id(y)))
				.as("unique")
				.doesNotMatch("(\\d+)!=\\1");

		assertThat(code(id(x, y, z), text("="), id(y), text("="), id(z)))
				.as("aliased")
				.matches("(\\d+)=\\1=\\1");

		assertThatIllegalStateException().isThrownBy(() -> code(id(x, z), id(y, z)))
				.as("clashes trapped");
	}

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
