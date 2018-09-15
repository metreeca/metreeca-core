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

import java.io.InputStream;
import java.util.Optional;
import java.util.function.Supplier;

import static com.metreeca.form.things.Transputs.input;


/**
 * Inbound raw body format.
 */
public final class _Input implements Format<Supplier<InputStream>> {

	/**
	 * The singleton inbound raw body format.
	 */
	public static final _Input Format=new _Input();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Input() {} // singleton


	@Override public Optional<Supplier<InputStream>> get(final Message<?> message) {
		return message.body(_Reader.Format).map(input -> () -> input(input.get())); // !!! use message charset
	}

	@Override public void set(final Message<?> message, final Supplier<InputStream> value) {
		if ( !message.header("content-type").isPresent() ) {
			message.header("content-type", "application/octet-stream");
		}
	}

}
