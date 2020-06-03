package com.metreeca.rest.actions;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

final class CleanTest {

	@Test void testSpace() {

		final String text="  leading \t\u00A0trailing\n\r";

		assertThat(new Clean().space(true).apply(text)).isEqualTo("leading trailing");
		assertThat(new Clean().space(false).apply(text)).isEqualTo(text);
	}

	@Test void testMarks() {

		final String text="èé";

		assertThat(new Clean().marks(true).apply(text)).isEqualTo("ee");
		assertThat(new Clean().marks(false).apply(text)).isEqualTo(text);
	}

	@Test void testSmart() {

		final String text="‘’“”‹› ";

		assertThat(new Clean().smart(true).apply(text)).isEqualTo("''\"\"<> ");
		assertThat(new Clean().smart(false).apply(text)).isEqualTo(text);
	}

}