/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */


package com.metreeca.gate;

import org.junit.jupiter.api.Test;

import static com.metreeca.gate.Policy.*;

import static org.assertj.core.api.Assertions.assertThat;

import static java.lang.Character.UnicodeBlock.BASIC_LATIN;
import static java.lang.Math.abs;


final class PolicyTest {

	private boolean verify(final String secret) {
		return all(

				only(block(BASIC_LATIN)),

				between(8, 32, characters()),

				contains(uppercases()),
				contains(lowercases()),
				contains(digits()),
				contains(specials()),

				no(controls()),
				no(stopwords()),

				no(sequences(3))

		).verify("tino.faussone@example.com", secret);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


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
