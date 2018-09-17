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
import com.metreeca.next.Wrapper;
import com.metreeca.next.formats.JSON;
import com.metreeca.next.formats._Writer;

import java.io.IOException;
import java.io.Writer;

import static com.metreeca.next.Rest.error;
import static com.metreeca.utow.Gateway.run;


public final class Work {

	public static void main(final String... args) {

		final Wrapper wrapper=handler -> request -> handler.handle(request)
				.map(response -> response.map(_Writer.Format, consumer -> writer -> consumer.accept(upper(writer))));

		final Handler handler=request -> request.reply(response -> response
				.status(200).body(JSON.Format, error("ciao", "babbo!"))
		);

		run(6800, "localhost", tray -> tray.get(() -> wrapper.wrap(handler)));
	}


	private static Writer upper(final Writer writer) {
		return new Writer() {

			@Override public void write(final char[] cbuf, final int off, final int len) throws IOException {

				for (int i=0; i < cbuf.length; i++) {
					cbuf[i]=Character.toUpperCase(cbuf[i]);
				}

				writer.write(cbuf, off, len);
			}

			@Override public void flush() throws IOException { writer.flush(); }

			@Override public void close() throws IOException { writer.close(); }

		};
	}

}
