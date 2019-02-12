/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.engines;

import org.junit.jupiter.api.Test;

import java.io.*;

import static org.assertj.core.api.Assertions.assertThat;


final class WorkTest {

	private String indent(final CharSequence text) {
		try (final Writer writer=new StringWriter()) {

			return Work.indent(writer).append(text).toString();

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testIndentBraceBlocks() {

		assertThat(indent("{\nuno\n}\ndue"))
				.as("indented block")
				.isEqualTo("{\n    uno\n}\ndue");

		assertThat(indent("{ {\nuno\n} }\ndue"))
				.as("inline block")
				.isEqualTo("{ {\n    uno\n} }\ndue");

	}


	@Test void testIgnoreLeadingSpaces() {

		assertThat(indent("  {\n\tuno\n  due\n }"))
				.as("single")
				.isEqualTo("{\n    uno\n    due\n}");

	}

	@Test void testCollapseSpaces() {

		assertThat(indent(" text"))
				.as("leading")
				.isEqualTo("text");

		assertThat(indent("uno  due"))
				.as("inside")
				.isEqualTo("uno due");

	}

	@Test void testCollapseNewlines() {

		assertThat(indent("uno\ndue"))
				.as("single")
				.isEqualTo("uno\ndue");

		assertThat(indent("uno\n\n\n\ndue"))
				.as("multiple")
				.isEqualTo("uno\n\ndue");

	}

}
