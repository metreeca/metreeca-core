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

package com.metreeca.next.formats;

import com.metreeca.next.Format;
import com.metreeca.next.Message;

import java.io.Reader;
import java.util.Optional;
import java.util.function.Supplier;

import static com.metreeca.form.things.Transputs.reader;


/**
 * Inbound raw body format.
 */
public final class _Reader implements Format<Supplier<Reader>> {

	/**
	 * The singleton inbound raw body format.
	 */
	public static final _Reader Format=new _Reader();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Reader() {} // singleton


	@Override public Optional<Supplier<Reader>> get(final Message<?> message) {
		return message.body(_Input.Format).map(input -> () -> reader(input.get())); // !!! use message charset
	}

	@Override public void set(final Message<?> message, final Supplier<Reader> value) {
		if ( !message.header("content-type").isPresent() ) {
			message.header("content-type", "text/plain");
		}
	}

}
