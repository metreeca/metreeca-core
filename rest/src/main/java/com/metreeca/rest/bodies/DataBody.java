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

package com.metreeca.rest.bodies;

import com.metreeca.form.things.Codecs;
import com.metreeca.rest.*;

import java.io.*;

import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.OutputBody.output;


/**
 * Binary body format.
 */
public final class DataBody implements Body<byte[]> {

	private static final byte[] empty=new byte[0];

	private static final DataBody Instance=new DataBody();


	/**
	 * Retrieves the binary body format.
	 *
	 * @return the singleton binary body format instance
	 */
	public static DataBody data() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private DataBody() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a result providing access to the binary representation of {@code message}, as retrieved from the input
	 * stream supplied by its {@link InputBody} body, if one is available; a failure describing the processing error,
	 * otherwise
	 */
	@Override public Result<byte[], Failure> get(final Message<?> message) {
		return message.body(input()).fold(

				source -> {
					try (final InputStream input=source.get()) {

						return Value(Codecs.data(input));

					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				},

				error -> error.equals(Body.Missing) ?  Value(empty) : Error(error)

		);
	}

	/**
	 * Configures the {@link OutputBody} body of {@code message} to write the binary {@code value} to the output
	 * stream supplied by the accepted output stream supplier.
	 */
	@Override public <T extends Message<T>> T set(final T message) {
		return message.body(output(), data().map(data -> target -> {
			try (final OutputStream output=target.get()) {

				output.write(data);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		}));
	}

}
