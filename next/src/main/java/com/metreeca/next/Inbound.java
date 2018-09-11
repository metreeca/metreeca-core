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

import com.metreeca.form.things.Transputs;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;


/**
 * Abstract inbound HTTP message.
 *
 * <p>Handles shared state/behaviour for inbound HTTP messages and message parts.</p>
 */
public abstract class Inbound<T extends Inbound<T>> extends Message<T> {

	/**
	 * The {@linkplain #body(Function, Object) format function} for textual body representations.
	 *
	 * <p>Retrieves the textual content of the reader provided by the {@linkplain #body() body supplier} of the target
	 * inbound message.</p>
	 */
	public static final Function<Inbound<?>, Optional<String>> TextFormat=inbound -> {
		try (Reader reader=inbound.body().get().reader()) {

			return Optional.of(Transputs.text(reader));

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	};

	/**
	 * The {@linkplain #body(Function, Object) format function} for binary body representations.
	 *
	 * <p>Retrieves the binary content of the inputs tream provided by the {@linkplain #body() body supplier} of the
	 * target inbound message.</p>
	 */
	public static final Function<Inbound<?>, Optional<byte[]>> DataFormat=inbound -> {
		try (InputStream input=inbound.body().get().input()) {

			return Optional.of(Transputs.data(input));

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	};


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Supplier<Source> body=() -> new Source() {

		@Override public Reader reader() { return Transputs.reader(); }

		@Override public InputStream input() { return  Transputs.input(); }

	};

	private final Map<Object, Object> views=new HashMap<>(); // structured body representations


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the body supplier of this message.
	 *
	 * @return a content source supplier able to generate either a reader or an input stream for the body of this message
	 *
	 * @throws IllegalStateException if the body supplier was already retrieved
	 */
	public Supplier<Source> body() throws IllegalStateException {

		if ( body == null ) {
			throw new IllegalStateException("undefined body");
		}

		try {

			return body;

		} finally {

			body=null;

		}
	}

	/**
	 * Configures the body supplier of this message.
	 *
	 * <p>Current structured representations, if already {@linkplain #body(Function, Object) defined}, are cleared.</p>
	 *
	 * @param body a content source supplier able to generate either a reader or an input stream for the body of this
	 *             message
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code body} is {@code null}
	 */
	public T body(final Supplier<Source> body) {

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
	 * <p>Replaces the current {@linkplain #body() body supplier} of this message with a new body obtained by filtering
	 * it with
	 * a mapping function. Current structured representations, if already {@linkplain #body(Function, Object) defined},
	 * are cleared.</p>
	 *
	 * @param filter the mapping function to be applied to the current message body
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if {@code filter} is {@code null}
	 */
	public T filter(final UnaryOperator<Supplier<Source>> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		return body(filter.apply(body()));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves a structured representation of the body of this message.
	 *
	 * @param format a function able to convert an inbound message into a structured representation of its body, relying
	 *               on its {@linkplain #body() body supplier} and other structured representations
	 * @param <V>    the type of the structured representation to be retrieved
	 *
	 * @return an optional structured representation of the body of this message, if one was already retrieved for the
	 * same {@code format} or if {@code format} is able to generate one from the current mesage state; an empty optional
	 * otherwise
	 *
	 * @throws NullPointerException if {@code format} is {@code null}
	 */
	public <V> Optional<V> body(final Function<Inbound<?>, Optional<V>> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		return Optional.ofNullable((V)views.computeIfAbsent(format, key -> format.apply(self()).orElse(null)));
	}

	/**
	 * Configures the structured representation of this message.
	 *
	 * <p>The current body supplier and current structured representations, if already defined, are cleared.</p>
	 *
	 * @param format a function able to convert an inbound message into a structured representation of its body, relying
	 *               on its {@linkplain #body() body supplier} and other structured representations
	 * @param body   the structured representation of the body to be configured using {@code format}
	 * @param <V>    the type of the structured representation to be configured
	 *
	 * @return this message
	 *
	 * @throws NullPointerException if either {@code format} of {@code body} is {@code null}
	 */
	public <V> T body(final Function<Inbound<?>, Optional<V>> format, final V body) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		this.body=null;

		views.clear();
		views.put(format, body);

		return self();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the textual representation of the body of this message.
	 *
	 * @return the optional textual representation of the body of this message, as retrieved using {@link #TextFormat}
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
	 * @return the optional binary representation of the body of this message, as retrieved using {@link #DataFormat}
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
	public T text(final byte... data) {

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		return body(DataFormat, data);
	}

}
