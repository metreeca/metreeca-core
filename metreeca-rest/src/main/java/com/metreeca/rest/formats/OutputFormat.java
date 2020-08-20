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

import com.metreeca.rest.Failure;
import com.metreeca.rest.Format;
import com.metreeca.rest.Message;
import com.metreeca.rest.Result;

import java.io.OutputStream;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.metreeca.rest.Result.Value;


/**
 * Raw binary output body format.
 */
public final class OutputFormat extends Format<Consumer<Supplier<OutputStream>>> {

	/**
	 * The default MIME type for binary raw message bodies ({@value}).
	 */
	public static final String MIME="application/octet-stream";


	/**
	 * Creates a raw binary output body format.
	 *
	 * @return the new raw binary output body format
	 */
	public static OutputFormat output() {
		return new OutputFormat();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private OutputFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a result providing access to a consumer taking no action on the supplied output stream provider
	 */
	@Override public Result<Consumer<Supplier<OutputStream>>, Failure> get(final Message<?> message) {
		return Value(target -> {});
	}

	/**
	 * Configures the {@code Content-Type} header of {@code message} to {@value #MIME}, unless already defined.
	 */
	@Override public <M extends Message<M>> M set(final M message, final Consumer<Supplier<OutputStream>> value) {
		return message.header("~Content-Type", MIME);
	}

}
