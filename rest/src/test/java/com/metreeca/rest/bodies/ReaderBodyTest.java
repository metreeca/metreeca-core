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

package com.metreeca.rest.bodies;

import com.metreeca.form.things.Codecs;
import com.metreeca.rest.MessageTest.TestMessage;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.ReaderBody.reader;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


final class ReaderBodyTest {

	@Test void testReadFromInputUsingCharset() {

		final String text="ça va";
		final String encoding="ISO-8859-1";

		new TestMessage()

				.header("Content-Type", "text/plain; charset="+encoding)
				.body(input(), () -> Codecs.input(new StringReader(text), encoding))

				.body(reader())

				.fold(

						value -> assertThat(Codecs.text(value.get()))
								.as("read using provided encoding")
								.isEqualTo(text),

						error -> fail(error.toString())

				);
	}


}
