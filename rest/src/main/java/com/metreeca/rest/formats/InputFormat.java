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
import com.metreeca.rest.*;

import java.io.InputStream;
import java.util.function.Supplier;

import static com.metreeca.form.Result.error;


/**
 * Raw binary input body format.
 */
public final class InputFormat implements Format<Supplier<InputStream>> {

	/**
	 * Creates a raw binary input body format.
	 *
	 * @return a new raw binary input body format
	 */
	public static InputFormat input() {
		return new InputFormat();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private InputFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a result providing access to the raw binary input body of {@code message}, if one was explicitly set for
	 * {@code message}; an error describing the processing failure, otherwise
	 */
	@Override public Result<Supplier<InputStream>, Failure> get(final Message<?> message) {
		return error(new Failure().status(Response.UnsupportedMediaType));
	}

	@Override public <T extends Message<T>> T set(final T message) {
		return message;
	}

}
