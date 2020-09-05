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

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;

import static com.metreeca.rest.formats.InputFormat.input;


/**
 * Textual input body format.
 */
public final class ReaderFormat extends Format<Supplier<Reader>> {

	/**
	 * Creates a textual input body format.
	 *
	 * @return a new textual input body format
	 */
	public static ReaderFormat reader() {
		return new ReaderFormat();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ReaderFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a value providing access to a reader supplier converting from the {@linkplain InputFormat raw binary
	 * input
	 * body} of {@code message} using the character encoding specified in its {@code Content-Type} header or the
	 * {@linkplain StandardCharsets#UTF_8 default charset} if none is specified; an error  providing access to
	 * the
	 * processing failure, otherwise
	 */
	@Override public Result<Supplier<Reader>, UnaryOperator<Response>> get(final Message<?> message) {
		return message.body(input()).value(input -> () ->
				Codecs.reader(input.get(), message.charset())
		);
	}

}
