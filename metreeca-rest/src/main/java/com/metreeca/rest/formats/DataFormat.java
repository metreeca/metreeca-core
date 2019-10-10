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

package com.metreeca.rest.formats;

import com.metreeca.rest.*;
import com.metreeca.rest.Codecs;

import java.io.*;

import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;


/**
 * Binary body format.
 */
public final class DataFormat implements Format<byte[]> {

	private static final byte[] empty=new byte[0];

	private static final DataFormat Instance=new DataFormat();


	/**
	 * Retrieves the binary body format.
	 *
	 * @return the singleton binary body format instance
	 */
	public static DataFormat data() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private DataFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a result providing access to the binary representation of {@code message}, as retrieved from the input
	 * stream supplied by its {@link InputFormat} body, if one is available; a failure describing the processing error,
	 * otherwise
	 */
	@Override public Result<byte[], Failure> get(final Message<?> message) {
		return message.body(input()).value(

				source -> {
					try (final InputStream input=source.get()) {

						return Codecs.data(input);

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				}

		);
	}

	/**
	 * Configures the {@link OutputFormat} body of {@code message} to write the binary {@code value} to the output
	 * stream supplied by the accepted output stream supplier.
	 */
	@Override public <M extends Message<M>> M set(final M message, final byte[] value) {
		return message.body(output(), target -> {
			try (final OutputStream output=target.get()) {

				output.write(value);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

}