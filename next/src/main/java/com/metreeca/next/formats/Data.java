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

package com.metreeca.next.formats;

import com.metreeca.next.Format;
import com.metreeca.next.Message;
import com.metreeca.next.Target;

import java.io.*;
import java.util.Optional;

import static com.metreeca.form.things.Transputs.data;


/**
 * Binary body format.
 */
public final class Data implements Format<byte[]> {

	/**
	 * The singleton binary body format.
	 */
	public static final Format<byte[]> Format=new Data();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Data() {} // singleton


	/**
	 * @return the optional binary body representation of {@code message}, as retrieved from the input stream supplied
	 * by its {@link Inbound#Format} representation, if present; an empty optional, otherwise
	 */
	@Override public Optional<byte[]> get(final Message<?> message) {
		return message.body(Inbound.Format).map(source -> {
			try (final InputStream input=source.input()) {

				return data(input);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Configures the {@link Outbound#Format} representation of {@code message} to write the binary {@code value} to the
	 * output stream supplied by the accepted {@link Target}.
	 */
	@Override public void set(final Message<?> message, final byte... value) {
		message.body(Outbound.Format, target -> {
			try (final OutputStream output=target.output()) {

				output.write(value);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

}
