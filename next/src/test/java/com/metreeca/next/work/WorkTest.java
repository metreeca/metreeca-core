/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.next.work;

import com.metreeca.next.*;

import org.junit.jupiter.api.Test;

import java.io.*;
import java.util.function.UnaryOperator;

import static org.junit.jupiter.api.Assertions.assertEquals;


final class WorkTest {

	@Test void test() {

		final Handler handler=request -> (Source<Response>)consumer -> consumer.accept(request.response().status(200)

				.text(sink -> {
					try (final Writer writer=sink.get()) {
						writer.write("ciao");
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				}));

		final UnaryOperator<Writer> emphasis=writer -> {

			System.err.println("writer filtered");

			return new FilterWriter(writer) {
				@Override public void close() throws IOException {
					write("!");
					super.close();
				}
			};
		};


		handler

				.after(response -> response.text(emphasis))

				.handle(new Request().method("GET"))

				.accept(response -> {

					assertEquals(200, response.status());

					final StringWriter writer=new StringWriter(1000);
					final ByteArrayOutputStream output=new ByteArrayOutputStream(1000);

					response.text().accept(() -> writer);
					response.data().accept(() -> output);

					System.out.println(writer);
					System.out.println(output);

				});
	}

}
