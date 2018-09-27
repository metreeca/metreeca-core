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

package com.metreeca.rest.formats;

import com.metreeca.form.Result;
import com.metreeca.rest.Failure;
import com.metreeca.rest.Format;
import com.metreeca.rest.Message;

import java.io.*;

import static com.metreeca.form.Result.value;
import static com.metreeca.form.things.Transputs.data;


/**
 * Binary body format.
 */
public final class DataFormat implements Format<byte[]> {

	/**
	 * The singleton binary body format.
	 */
	public static final Format<byte[]> asData=new DataFormat();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private DataFormat() {} // singleton


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return the optional binary body representation of {@code message}, as retrieved from the input stream supplied
	 * by its {@link InputFormat} representation, if one is present; an empty optional, otherwise
	 */
	@Override public Result<byte[], Failure> get(final Message<?> message) {
		return message.body(InputFormat.asInput).value(source -> {
			try (final InputStream input=source.get()) {

				return value(data(input));

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Configures the {@link OutputFormat} representation of {@code message} to write the binary {@code value} to the
	 * output stream supplied by the accepted output stream.
	 */
	public <T extends Message<T>> T set(final T message, final byte... value) {
		return message.body(OutputFormat.asOutput, target -> {
			try (final OutputStream output=target.get()) {

				output.write(value);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

}
