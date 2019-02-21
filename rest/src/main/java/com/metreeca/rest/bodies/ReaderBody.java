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

package com.metreeca.rest.bodies;

import com.metreeca.form.things.Codecs;
import com.metreeca.rest.*;

import java.io.Reader;
import java.util.function.Supplier;

import static com.metreeca.rest.bodies.InputBody.input;


/**
 * Textual input body format.
 */
public final class ReaderBody implements Body<Supplier<Reader>> {

	private static final ReaderBody Instance=new ReaderBody();


	/**
	 * Retrieves the textual input body format.
	 *
	 * @return the singleton textual input body format instance
	 */
	public static ReaderBody reader() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ReaderBody() {}

	/**
	 * @return a value providing access to a reader supplier converting from the {@linkplain InputBody raw binary input
	 * body} of {@code message} using the character encoding specified in its {@code Content-Type} header or the
	 * {@linkplain Codecs#UTF8 default charset} if none is specified; an error  providing access to the processing
	 * failure, otherwise
	 */
	@Override public Result<Supplier<Reader>, Failure> get(final Message<?> message) {
		return message.body(input()).value(supplier -> () ->
				Codecs.reader(supplier.get(), message.charset().orElse(Codecs.UTF8.name()))
		);
	}

}
