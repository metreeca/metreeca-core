/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.core;

import static com.metreeca.core.Either.Left;
import static com.metreeca.core.MessageException.status;


/**
 * Message format {thread-safe}.
 *
 * <p>Decodes and encodes message bodies.</p>
 *
 * <p><strong>Warning</strong> / Concrete subclasses must be thread-safe.</p>
 *
 * @param <V> the type of the message body managed by the format
 */
public abstract class Format<V> {

	/**
	 * Decodes a message body.
	 *
	 * <p>The default implementation returns a {@linkplain MessageException#status() no-op message exception}.</p>
	 *
	 * <p>Concrete subclasses should report decoding issues using the following HTTP status codes:</p>
	 *
	 * <ul>
	 * <li>{@link Response#UnsupportedMediaType} for missing bodies;</li>
	 * <li>{@link Response#BadRequest} for malformed bodies, unless a more specific status code is available.</li>
	 * </ul>
	 *
	 * @param message the message whose body is to be decoded
	 *
	 * @return either a message exception reporting a decoding issue or the decoded {@code message} body
	 *
	 * @throws NullPointerException if {@code message} is null
	 */
	public Either<MessageException, V> decode(final Message<?> message) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		return Left(status());
	}

	/**
	 * Encodes a message body.
	 *
	 * <p>The default implementation has no effects.</p>
	 *
	 * @param message the message whose body is to be encoded
	 * @param value   the body being encoded into {@code message}
	 * @param <M>     the type of {@code message}
	 *
	 * @return the target {@code message} with the encoded {@code value} as body
	 *
	 * @throws NullPointerException if either {@code message} or {@code value} is null
	 */
	public <M extends Message<M>> M encode(final M message, final V value) {

		if ( message == null ) {
			throw new NullPointerException("null message");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return message;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * {@inheritDoc}
	 *
	 * <p>All formats in the same class are equal to each other.</p>
	 */
	@Override public boolean equals(final Object object) {
		return this == object || object != null && getClass().equals(object.getClass());
	}

	@Override public int hashCode() {
		return getClass().hashCode();
	}

}
