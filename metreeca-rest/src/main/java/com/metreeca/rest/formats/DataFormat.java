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

import com.metreeca.rest.*;

import java.io.*;
import java.util.regex.Pattern;

import static com.metreeca.rest.Xtream.copy;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.OutputFormat.output;


/**
 * Binary body format.
 */
public final class DataFormat extends Format<byte[]> {

	/**
	 * The default MIME type for binary messages ({@value}).
	 */
	public static final String MIME="application/octet-stream";

	/**
	 * A pattern matching binary MIME types, for instance {@code application/zip or image/png}.
	 */
	public static final Pattern MIMEPattern=Pattern.compile("(?i)^(application|image)/.+$");


	/**
	 * Creates a binary body format.
	 *
	 * @return a new binary body format
	 */
	public static DataFormat data() {
		return new DataFormat();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static byte[] data(final InputStream input) {

		if ( input == null ) {
			throw new NullPointerException("null input");
		}

		try ( final ByteArrayOutputStream output=new ByteArrayOutputStream() ) {

			return copy(output, input).toByteArray();

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}

	public static <O extends OutputStream> O data(final O output, final byte[] value) {

		if ( output == null ) {
			throw new NullPointerException("null output");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		try {

			output.write(value);
			output.flush();

			return output;

		} catch ( final IOException e ) {

			throw new UncheckedIOException(e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private DataFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Decodes the binary {@code message} body from the input stream supplied by the {@code message}
	 * {@link InputFormat} body, if one is available
	 */
	@Override public Either<MessageException, byte[]> decode(final Message<?> message) {
		return message.body(input()).map(source -> {
			try ( final InputStream input=source.get() ) {

				return data(input);

			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}
		});
	}

	/**
	 * Configures {@code message} {@code Content-Type} header to {@value #MIME}, unless already defined, and encodes
	 * the binary {@code value} into the output stream accepted by the {@code message} {@link OutputFormat} body
	 */
	@Override public <M extends Message<M>> M encode(final M message, final byte... value) {
		return message

				.header("~Content-Type", MIME)

				.body(output(), output -> data(output, value));
	}

}
