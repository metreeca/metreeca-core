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

package com.metreeca.rest.formats;

import com.metreeca.rest.Codecs;
import com.metreeca.rest.MessageMock;
import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.ReaderFormat.reader;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


final class ReaderFormatTest {

	@Test void testReadFromInputUsingCharset() {

		final String text="ça va";
		final String charset="ISO-8859-1";

		new MessageMock()

				.header("Content-Type", "text/plain; charset="+charset)
				.body(input(), () -> Codecs.input(new StringReader(text), charset))

				.body(reader())

				.fold(

						value -> assertThat(Codecs.text(value.get()))
								.as("read using provided charset")
								.isEqualTo(text),

						error -> fail(error.toString())

				);
	}


}
