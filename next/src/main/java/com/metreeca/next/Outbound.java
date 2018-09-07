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

package com.metreeca.next;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.UnaryOperator;


/**
 * Abstract outbound HTTP message.
 *
 * <p>Handles shared state/behaviour for outbound HTTP messages and message parts.</p>
 */
public abstract class Outbound<T extends Outbound<T>> extends Message<T> {

	public static final Function<String, Consumer<Target>> TextFormat=text -> target -> {
		try (final Writer writer=target.writer()) {

			writer.write(text);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	};

	public static final Function<byte[], Consumer<Target>> DataFormat=data -> target -> {
		try (final OutputStream output=target.output()) {

			output.write(data);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	};


	private static final Consumer<Target> Empty=target -> {};


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Consumer<Target> body=Empty;

	private final Map<Object, Object> views=new HashMap<>();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public Consumer<Target> body() {
		try {

			return body;

		} finally {

			body=Empty;

		}
	}

	public T body(final Consumer<Target> body) {

		this.body=body;

		views.clear();

		return self();
	}


	public <V> Optional<V> body(final Function<V, Consumer<Target>> format) {
		return Optional.ofNullable((V)views.get(format));
	}

	public <V> T body(final Function<V, Consumer<Target>> format, final V body) {

		this.body=format.apply(body);

		views.clear();
		views.put(format, body);

		return self();
	}


	public T filter(final UnaryOperator<Consumer<Target>> filter) {
		return body(filter.apply(body));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<String> text() {
		return body(TextFormat);
	}

	public T text(final String text) {
		return body(TextFormat, text);
	}


	public Optional<byte[]> data() {
		return body(DataFormat);
	}

	public T data(final byte[] data) {
		return body(DataFormat, data);
	}

}
