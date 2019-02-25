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

package com.metreeca.rest._multipart;

import com.metreeca.rest.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;

import static com.metreeca.rest.bodies.InputBody.input;


/**
 * @see <a href="https://tools.ietf.org/html/rfc2046#section-5.1">RFC 2046 - Multipurpose Internet Mail Extensions
 * (MIME) Part Two: Media Types - § 5.1.  Multipart Media Type</a>
 */
public final class MultipartBody implements Body<Map<String, Message<?>>> {

	private static final MultipartBody Instance=new MultipartBody();


	public static MultipartBody multipart() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private MultipartBody() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Result<Map<String, Message<?>>, Failure> get(final Message<?> message) {
		return message.body(input()).value(source -> {

			// !!! check for multipart/*

			final String boundary="boundary"; // !!! compute from content-type

			try {
				return new MultipartInput(message, source.get(), boundary).parse();
			} catch ( final IllegalStateException e ) {
				throw e; // !!! handle parsing errors
			} catch ( final IOException e ) {
				throw new UncheckedIOException(e);
			}

		});
	}

}
