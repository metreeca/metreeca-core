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

import java.io.OutputStream;
import java.io.Writer;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.function.UnaryOperator;


public abstract class Writable<T extends Writable<T>> extends Message<T> {

	private Consumer<Supplier<Writer>> text=supplier -> {};
	private Consumer<Supplier<OutputStream>> data=supplier -> {};


	public Consumer<Supplier<Writer>> text() {
		return text;
	}

	public T text(final Consumer<Supplier<Writer>> text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		this.text=text;

		return self();
	}

	public T text(final UnaryOperator<Writer> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		final Consumer<Supplier<Writer>> text=this.text;

		return text(sink -> { text.accept(() -> filter.apply(sink.get())); });
	}


	public Consumer<Supplier<OutputStream>> data() {
		return data;
	}

	public T data(final Consumer<Supplier<OutputStream>> data) {

		if ( data == null ) {
			throw new NullPointerException("null data");
		}

		this.data=data;

		return self();
	}

	public T data(final UnaryOperator<OutputStream> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		final Consumer<Supplier<OutputStream>> data=this.data;

		return data(sink -> { data.accept(() -> filter.apply(sink.get())); });
	}

}
