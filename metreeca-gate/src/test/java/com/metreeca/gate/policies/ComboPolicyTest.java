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


package com.metreeca.gate.policies;


import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static com.metreeca.gate.policies.ComboPolicy.*;

import static org.assertj.core.api.Assertions.assertThat;

import static java.lang.Character.UnicodeBlock.BASIC_LATIN;


final class ComboPolicyTest {

	private boolean verify(final String secret) {
		return all(

				only(blocks(BASIC_LATIN)),

				between(8, 32, characters()),

				contains(uppercases()),
				contains(lowercases()),
				contains(digits()),
				contains(specials()),

				no(controls()),
				no(stopwords(user -> Pattern.compile("\\W+").splitAsStream("tino.faussone@example.com"))),

				no(sequences(3))

		).verify("/users/faussone", secret);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testCompliant() {

		assertThat(verify("qyp-ZVM-160"))
				.as("compliant")
				.isTrue();

	}

	@Test void testLength() {

		assertThat(verify(new String(new char[7])))
				.as("too short")
				.isFalse();

		assertThat(verify(new String(new char[33])))
				.as("too long")
				.isFalse();

	}

	@Test void testCategories() {

		assertThat(verify("qyp-zvm-160"))
				.as("missing uppercase")
				.isFalse();

		assertThat(verify("GYP-ZVM-160"))
				.as("missing lowercase")
				.isFalse();

		assertThat(verify("qyp-ZVM-ahl"))
				.as("missing digits")
				.isFalse();

		assertThat(verify("qypZVM160"))
				.as("missing specials")
				.isFalse();

		assertThat(verify("qyp-ZVM\u0001160"))
				.as("control character")
				.isFalse();

		assertThat(verify("qyp\u0100ZVM-160"))
				.as("non basic latin")
				.isFalse();

	}

	@Test void testSequences() {

		assertThat(verify("aaa-ZVM-160"))
				.as("repetition")
				.isFalse();

		assertThat(verify("qyp-ABC-160"))
				.as("upward")
				.isFalse();

		assertThat(verify("qyp-ZVM-321"))
				.as("downward")
				.isFalse();

	}

	@Test void testStopWords() {

		assertThat(verify("tino-ZVM-160"))
				.as("forename included")
				.isFalse();

		assertThat(verify("faussone-ZVM-160"))
				.as("surname included")
				.isFalse();

		assertThat(verify("qyp-examPLe-160"))
				.as("company included")
				.isFalse();

	}

}
