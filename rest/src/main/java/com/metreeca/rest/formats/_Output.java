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

import com.metreeca.rest.Format;
import com.metreeca.rest.Message;

import java.io.OutputStream;
import java.util.function.Consumer;


/**
 * Inbound raw body format.
 */
public final class _Output implements Format<Consumer<OutputStream>> {

	/**
	 * The singleton inbound raw body format.
	 */
	public static final _Output Format=new _Output();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private _Output() {} // singleton


	@Override public void set(final Message<?> message, final Consumer<OutputStream> value) {
		if ( !message.header("content-type").isPresent() ) {
			message.header("content-type", "application/octet-stream");
		}
	}

}
