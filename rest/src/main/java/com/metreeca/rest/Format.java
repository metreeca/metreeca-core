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


import static com.metreeca.rest.Result.Error;


/**
 * Message body format.
 *
 * <p>Manages the conversion between raw and structured message bodies.</p>
 *
 * @param <V> the type of the structured message body managed by the format
 */
public interface Format<V> {

	/**
	 * Retrieves a structured body from a message.
	 *
	 * <p>Processing failure should be reported using the following HTTP status codes:</p>
	 *
	 * <ul>
	 * <li>{@link Response#UnsupportedMediaType} for missing bodies;</li>
	 * <li>{@link Response#BadRequest} for malformed bodies, unless a more specific status code is available.</li>
	 * </ul>
	 *
	 * <p>The default implementation returns a failure reporting the {@link Response#UnsupportedMediaType} status,
	 * unless explicitly {@linkplain Message.Body#set(Object) overridden}</p>
	 *
	 * @param message the message the structured body managed by this format is to be retrieved from
	 *
	 * @return a value result providing access to the structured body managed by this format, if it was possible to
	 * derive one from {@code message}; an error result providing access to the processing failure, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public default Result<V, Failure> get(final Message<?> message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return Error(new Failure().status(Response.UnsupportedMediaType));
	}

	/**
	 * Configures a message to hold a structured body.
	 *
	 * <p>The default implementation has no effect.</p>
	 *
	 * <p>The default implementation has no effects.</p>
	 *
	 * @param message the message to be configured to hold a structured body managed by this format
	 * @param <T>     the type of {@code message}
	 *
	 * @return the configured {@code message}
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public default <T extends Message<T>> T set(final T message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return message;
	}

}
