/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest;

import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.MessageException.status;


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
	 * <p>All format objects in the same class are equal to each other.</p>
	 */
	@Override public final boolean equals(final Object object) {
		return this == object || object != null && getClass().equals(object.getClass());
	}

	@Override public final int hashCode() {
		return getClass().hashCode();
	}

}
