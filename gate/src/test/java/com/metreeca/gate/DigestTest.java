/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.gate;


import com.metreeca.tray.Tray;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


public abstract class DigestTest {

	protected abstract Digest digest();


	private void exec(final Runnable ...tasks) {
		new Tray()
				.exec(tasks)
				.clear();
	}


	@Test void testRandomize() {
		exec(() -> assertThat(digest().digest("secret"))
				.as("randomized").isNotEqualTo(digest().digest("secret"))
		);
	}

	@Test void testRecognize() {
		exec(() -> assertThat(digest().verify("secret", digest().digest("secret")))
				.as("recognized").isTrue()
		);
	}

	@Test void testReject() {
		exec(() -> assertThat(digest().verify("public", digest().digest("secret")))
				.as("rejected").isFalse()
		);
	}

	@Test void testMalformed() {
		exec(() -> assertThatExceptionOfType(IllegalArgumentException.class)
				.isThrownBy(() -> digest().verify("secret", "malformed"))
		);
	}

}
