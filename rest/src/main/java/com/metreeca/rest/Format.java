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

import static com.metreeca.form.Result.error;


/**
 * Message body format.
 *
 * <p>Manages the conversion between structured and raw message body representations.</p>
 *
 * @param <V> the type of the structured message body representation managed by the format
 */
public interface Format<V> {

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
	 * <p>The default implementation reports a failure with the {@link Response#UnsupportedMediaType} status code.</p>
	 *
	 * @param message the message the structured body representation associated with this format is to be retrieved from
	 *
	 * @return a result providing access to the structured body representation associated with this format, if it was
	 * possible to derive one from {@code message}; a result providing access to the processing failure, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public default Result<V, Failure> get(final Message<?> message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return error(new Failure().status(Response.UnsupportedMediaType));
	}

	/**
	 * Configures a message to hold a structured body representation.
	 *
	 * <p>The default implementation has no effect.</p>
	 *
	 * @param message the message to be configured to hold a structured body representation associated with this format
	 * @param value   the structured body representation {@code message} is to be configured to hold
	 *
	 * @return the configured {@code message}
	 * @throws NullPointerException if either {@code message} or {@code value} is null
	 */
	public default <T extends Message<T>> T set(final T message, final V value) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return message;
	}

}
