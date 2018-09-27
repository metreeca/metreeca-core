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

import java.io.Writer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static com.metreeca.form.Result.value;


/**
 * Textual output body format.
 */
public final class WriterFormat implements Format<Consumer<Supplier<Writer>>> {

	/**
	 * The default MIME type for textual outbound raw message bodies.
	 */
	private static final String MIME="text/plain";

	/**
	 * Creates a textual output body format.
	 *
	 * @return a new textual output body format
	 */
	public static WriterFormat writer() {
		return new WriterFormat();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private WriterFormat() {}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * @return a result providing access to a consumer taking no action on the supplied writer provider
	 */
	@Override public Result<Consumer<Supplier<Writer>>, Failure> get(final Message<?> message) {
		return value(target -> {});
	}

	/**
	 * Configures the {@code Content-Type} header of {@code message} to {@value #MIME}, unless already defined
	 */
	@Override public <T extends Message<T>> T set(final T message) {
		return message.header("~Content-Type", MIME);
	}

}
