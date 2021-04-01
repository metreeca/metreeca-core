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

package com.metreeca.rest.actions;

import com.metreeca.rest.*;
import com.metreeca.rest.services.Logger;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Toolbox.service;


/**
 * Message body parsing.
 *
 * <p>Extracts a specific body representation from a message.</p>
 *
 * @param <R> the type of the message body to be extracted
 */
public final class Parse<R> implements Function<Message<?>, Optional<R>> {

	private final Format<R> format;

	private final Logger logger=service(Logger.logger());


	/**
	 * Creates a new message body parser.
	 *
	 * @param format the format of the message body to be extracted
	 *
	 * @throws NullPointerException if {@code format} is null
	 */
	public Parse(final Format<R> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		this.format=format;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Parses a message body representation.
	 *
	 * @param message the message whose body representation is to be parsed
	 *
	 * @return an optional body representation of the required format, if {@code message} was not null and its body
	 * representation successfully pased; an empty optional, otherwise, logging an error to the
	 * {@linkplain Logger#logger() shared event logger}
	 */
	@Override public Optional<R> apply(final Message<?> message) {
		return message == null ? Optional.empty() : message.body(format).fold(error -> {

			// !!! get cause directly from failure

			final Response parse=new Response(message.request()).map(error);
			final String media=format.getClass().getSimpleName();

			if ( parse.status() == Response.UnsupportedMediaType ) {

				logger.warning(this,
						String.format("no <%s> message body", media)
				);

			} else {

				// !!! review formatting >> avoid newlines in log

				logger.error(this,
						String.format("unable to parse message body as <%s>", media),
						new RuntimeException(error.toString(), parse.cause().orElse(null))
				);

			}

			return Optional.empty();

		}, Optional::of);
	}

}
