/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest;


import static com.metreeca.rest.Result.Error;


/**
 * Message body format.
 *
 * <p>Manages the conversion between raw and structured message bodies.</p>
 *
 * @param <V> the type of the structured message body managed by the body format
 */
public interface Body<V> {

	/**
	 * The default failure reporting missing message bodies.
	 */
	public static final Failure Missing=new Failure().status(Response.UnsupportedMediaType);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

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
	 * unless explicitly {@linkplain Message#body(Body, Object) overridden}.</p>
	 *
	 * @param message the message the structured body managed by this body format is to be retrieved from
	 *
	 * @return a value providing access to the structured body managed by this body format, if it was possible to derive
	 * one from {@code message}; an error providing access to the processing failure, otherwise
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public default Result<V, Failure> get(final Message<?> message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return Error(Missing);
	}

	/**
	 * Configures a message to hold a structured value.
	 *
	 * <p>The default implementation has no effects.</p>
	 *
	 * @param message the message to be configured to hold a structured body managed by this body format
	 * @param value the structured body being configured for {@code message} by this message body format
	 * @param <M>     the type of {@code message}
	 *
	 * @return the configured {@code message}
	 *
	 * @throws NullPointerException if either {@code message} or {@code value} is null
	 */
	public default <M extends Message<M>> M set(final M message, final V value) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return message;
	}

}
