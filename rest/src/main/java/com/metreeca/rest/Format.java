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

import com.metreeca.form.Result;


/**
 * Message body format.
 *
 * <p>Manages the conversion between raw and structured  message body representations.</p>
 *
 * @param <V> the type of the structured message body representation managed by the format
 */
@FunctionalInterface public interface Format<V> {

	/**
	 * Retrieves a structured body representation from a message.
	 *
	 * <p>Processing failure should be reported using the following HTTP status codes:</p>
	 *
	 * <ul>
	 * <li>{@link Response#UnsupportedMediaType} for missing representations;</li>
	 * <li>{@link Response#BadRequest} for malformed representations, unless a more specific status code is
	 * available.</li>
	 * </ul>
	 *
	 * @param message the message the structured body representation managed by this format is to be retrieved
	 *                from
	 *
	 * @return a result providing access to the structured body representation managed by this format, if it was
	 * possible to derive one from {@code message}; a result providing access to the processing failure, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public Result<V, Failure> get(final Message<?> message);

}
