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

package com.metreeca.feed.net;

import com.metreeca.rest.Format;
import com.metreeca.rest.Message;
import com.metreeca.rest.Response;
import com.metreeca.rest.services.Logger;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;


public final class Parse<R> implements Function<Message<?>, Optional<R>> {

	private final Format<R> format;

	private final Logger logger=service(logger());


	public Parse(final Format<R> format) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		this.format=format;
	}

	@Override public Optional<R> apply(final Message<?> message) {
		return message.body(format).fold(Optional::of, error -> {

			final Response parse=new Response(message.request()).map(error); // !!! get directly from failure

			logger.error(this,
					String.format("unable to parse message body as <%s>", format.getClass().getName()),
					new RuntimeException(error.toString(), parse.cause().orElse(null)) // !!! review formatting // !!! avoid newlines in log
			);

			return Optional.empty();

		});
	}
}
