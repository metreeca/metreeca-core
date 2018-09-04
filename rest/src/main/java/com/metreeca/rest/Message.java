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

package com.metreeca.rest;

import java.util.regex.Pattern;


/**
 * Abstract HTTP message.
 *
 * <p>Handles shared state/behaviour for HTTP request/response mesages.</p>
 */
public abstract class Message {

	private static final Pattern HTMLPattern=Pattern.compile("\\btext/x?html\\b");


	/**
	 * Tests if a MIME header is indicative of an interactive exchange.
	 *
	 * @param header the value of an {@code Accept}/{@code Content-Type} or other MIME header associated with a HTTP
	 *               message
	 *
	 * @return {@code true} if {@code header} contains MIME types usually associated with an interactive browser-managed
	 * HTTP exchanges (e.g. {@code text/html}
	 */
	public static boolean interactive(final CharSequence header) {

		if ( header == null ) {
			throw new NullPointerException("null header");
		}

		return HTMLPattern.matcher(header).find();
	}

}
