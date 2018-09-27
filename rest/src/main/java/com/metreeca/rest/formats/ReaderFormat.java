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

import java.io.Reader;
import java.util.function.Supplier;


/**
 * Binary inbound raw body format.
 */
public final class ReaderFormat implements Format<Supplier<Reader>> {

	/**
	 * The singleton binary inbound raw body format.
	 */
	public static final ReaderFormat asReader=new ReaderFormat();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Result<Supplier<Reader>, Failure> get(final Message<?> message) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	private ReaderFormat() {} // singleton

}
