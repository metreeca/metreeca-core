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

import com.metreeca.rest.MessageTest.TestMessage;

import org.apache.commons.io.output.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

import java.io.*;

import static com.metreeca.rest.bodies.OutputBody.output;
import static com.metreeca.rest.bodies.WriterBody.writer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.fail;


final class WriterBodyTest {

	@Test void testWriteToOutputUsingCharset() {

		final String text="ça va";
		final String charset="ISO-8859-1";

		new TestMessage()

				.header("Content-Type", "text/plain; charset="+charset)

				.body(writer(), supplier -> {
					try (final Writer writer=supplier.get()) {
						writer.write(text);
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				})

				.body(output())

				.fold(

						value -> {
							try (final ByteArrayOutputStream buffer=new ByteArrayOutputStream()) {

								value.accept(() -> buffer);

								return assertThat(buffer.toByteArray())
										.as("written using provided charset")
										.isEqualTo(text.getBytes(charset));

							} catch ( final IOException unexpected ) {
								throw new UncheckedIOException(unexpected);
							}
						},

						error -> fail(error.toString())

				);
	}

}
