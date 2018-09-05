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

package com.metreeca.utow;

import com.metreeca.next.Handler;
import com.metreeca.next.Request;
import com.metreeca.tray.Tray;

import io.undertow.Undertow;
import io.undertow.util.Headers;

import java.io.*;


public final class Work {

	public static void main(final String[] args) {

		final Tray tray=new Tray();

		final Handler handler=tray.get(() -> request -> request.response()

				.status(200)

				.text(supplier -> {
					try (final Writer writer=supplier.get()) {

						writer.write("Ciao babbo!");

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				}));

		Undertow.builder()

				.addHttpListener(6800, "localhost")

				.setHandler(exchange -> handler

						// !!! handle multi-part forms (https://stackoverflow.com/questions/37839418/multipart-form-data-example-using-undertow)

						.handle(new Request().method(exchange.getRequestMethod().toString()))

						.accept(response -> {

							final StringWriter writer=new StringWriter(1000);

							response.text().accept(() -> writer);

							exchange.setStatusCode(response.status());

							exchange.getResponseHeaders().put(Headers.CONTENT_TYPE, "text/plain");
							exchange.getResponseSender().send(writer.toString()); // !!! stream

							// !!! handle binary data

						}))

				.build()
				.start();

		Runtime.getRuntime().addShutdownHook(new Thread(tray::clear));

	}

}
