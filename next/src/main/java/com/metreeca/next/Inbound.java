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
import java.util.function.UnaryOperator;


/**
 * Abstract inbound HTTP message.
 *
 * <p>Handles shared state/behaviour for inbound HTTP messages and message parts.</p>
 */
public abstract class Inbound<T extends Inbound<T>> extends Message<T> {

	public static final Function<Inbound<?>, Optional<String>> TextFormat=inbound -> {
		try (Reader reader=inbound.body().reader()) {

			return Optional.of(Transputs.text(reader));

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	};


	private static final Source Empty=new Source() {

		@Override public Reader reader() { throw new IllegalStateException("undefined text source"); }

		@Override public InputStream input() { throw new IllegalStateException("undefined data source"); }

	};


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Source body=Empty;

	private final Map<Object, Object> views=new HashMap<>();



	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Source body() {
		try {

			return body;

		} finally {

			body=Empty;

		}
	}

	public T body(final Source body) {

		if ( body == null ) {
			throw new NullPointerException("null body");
		}

		this.body=body;

		views.clear();

		return self();
	}


	public <V> Optional<V> body(final Function<Inbound<?>, Optional<V>> format) {
		return Optional.ofNullable((V)views.computeIfAbsent(format, key -> format.apply(self()).orElse(null)));
	}

	public <V> T body(final Function<Inbound<?>, Optional<V>> format, final V body) {

		this.body=Empty;

		views.clear();
		views.put(format, body);

		return self();
	}


	public T filter(final UnaryOperator<Source> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		return body(filter.apply(body));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<String> text() {
		return body(TextFormat);
	}

	public T text(final String text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		return body(TextFormat, text);
	}

}
