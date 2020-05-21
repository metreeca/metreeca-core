/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rest;

import org.junit.jupiter.api.Test;

import static com.metreeca.rest.Codecs.normalize;
import static org.assertj.core.api.Assertions.assertThat;

final class CodecsTest {

	@Test void testNormalize() {
		assertThat(normalize("  leading \t\u00A0trailing\n\r"))
				.isEqualTo("leading trailing");
	}

}