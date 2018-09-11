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

	/**
	 * The {@linkplain #body(Function, Object) format function} for the textual body representation.
	 */
	public static final Function<String, Consumer<Target>> TextFormat=text -> target -> {
		try (final Writer writer=target.writer()) {

			writer.write(text);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	};

	/**
	 * The {@linkplain #body(Function, Object) format function} for the binary body representation.
	 */
	public static final Function<byte[], Consumer<Target>> DataFormat=data -> target -> {
		try (final OutputStream output=target.output()) {

			output.write(data);

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	};


	private static final Consumer<Target> EmptyBody=target -> {};


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Consumer<Target> body=EmptyBody;

	private final Map<Object, Object> views=new HashMap<>(); // structured body representations (at most one item)


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Retrieves the body generator of this message.
	 *
	 * @return a consumer able to write the body of this message to a writer or an output stream retrieved from the
	 * supplied content target
	 */
	public Consumer<Target> body() {
		try {

			return body;

		} finally {

			body=EmptyBody;

		}
	}

	/**
	 * Configures the body generator of this message.
	 *
	 * <p>The current structured representation, if already {@linkplain #body(Function, Object) defined}, is
	 * overwritten.</p>
	 *
	 * @param body a consumer able to write the body of this message to a writer or an output stream retrieved from the
	 *             supplied content target
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code body} is {@code null}
	 */
	public T body(final Consumer<Target> body) {

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		this.body=body;

		views.clear();

		return self();
	}

	/**
	 * Filters the body of this message.
	 *
	 * <p>Replaces the current {@linkplain #body() body generator} of this message with a new generator obtained by
	 * filtering it with a mapping function. The current structured representation, if already {@linkplain
	 * #body(Function, Object) defined}, is overwritten.</p>
	 *
	 * @param filter the mapping function to be applied to the current message body generator
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code filter} is {@code null}
	 */
	public T filter(final UnaryOperator<Consumer<Target>> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		return body(filter.apply(body));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the structured representation of the body of this message.
	 *
	 * @param format a function able to convert a structured representation of the body into a {@linkplain #body() body
	 *               generator}
	 * @param <V>    the type of the structured representation to be retrieved
	 *
	 * @return an optional structured representation of the body of this message, if previously defined with the same
	 * {@code format} function; an empty optional otherwise
	 *
	 * @throws NullPointerException if {@code format} is {@code null}
	 */
	public <V> Optional<V> body(final Function<V, Consumer<Target>> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		return Optional.ofNullable((V)views.get(format));
	}

	/**
	 * Configures the structured representation of the body of this message.
	 *
	 * <p>The current body generator and the current structured representation, if already defined, are overwritten.</p>
	 *
	 * @param format a function able to convert a structured representation of the body into a {@linkplain #body() body
	 *               generator}
	 * @param body   the structured representation of the body to be configured using {@code format}
	 * @param <V>    the type of the structured representation to be configured
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code format} or {@code body} is {@code null}
	 */
	public <V> T body(final Function<V, Consumer<Target>> format, final V body) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		this.body=format.apply(body);

		views.clear();
		views.put(format, body);

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Retrieves the textual representation of the body of this message.
	 *
	 * @return the optional textual representation of the body of this message, if previously {@linkplain #text(String)
	 * configured}; an empty optional otherwise
	 */
	public Optional<String> text() {
		return body(TextFormat);
	}

	/**
	 * Configures the textual representation of the body of this message.
	 *
	 * @param text the textual representation of the body of this message
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code text} is {@code null}
	 */
	public T text(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		return body(TextFormat, text);
	}


	/**
	 * Retrieves the binary representation of the body of this message.
	 *
	 * @return the optional binary representation of the body of this message, if previously {@linkplain #data(byte[])
	 * configured}; an empty optional otherwise
	 */
	public Optional<byte[]> data() {
		return body(DataFormat);
	}

	/**
	 * Configures the binary representation of the body of this message.
	 *
	 * @param data the binary representation of the body of this message
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code data} is {@code null}
	 */
	public T data(final byte... data) {

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		return body(DataFormat, data);
	}

}
