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

import java.io.InputStream;
import java.io.Reader;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;


/**
 * Abstract inbound HTTP message.
 *
 * <p>Handles shared state/behaviour for inbound HTTP messages and message parts.</p>
 */
public abstract class Inbound<T extends Inbound<T>> extends Message<T> {

	private Supplier<Reader> text=Transputs::reader;
	private Supplier<InputStream> data=Transputs::input;


	public Supplier<Reader> text() {
		return text;
	}

	public T text(final Supplier<Reader> text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		this.text=text;

		return self();
	}

	public T text(final UnaryOperator<Reader> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		final Supplier<Reader> text=this.text;

		return text(() -> filter.apply(text.get()));
	}


	public Supplier<InputStream> data() {
		return data;
	}

	public T data(final Supplier<InputStream> data) {

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		this.data=data;

		return self();
	}

	public T data(final UnaryOperator<InputStream> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		final Supplier<InputStream> data=this.data;

		return data(() -> filter.apply(data.get()));
	}

}
