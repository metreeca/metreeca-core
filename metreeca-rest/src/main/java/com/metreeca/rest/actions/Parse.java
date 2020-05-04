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

package com.metreeca.rest.actions;

import com.metreeca.rest.*;
import com.metreeca.rest.services.Logger;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;


/**
 * Message body parsing.
 *
 * <p>Extracts a specific body representation from a message.</p>
 *
 * @param <R> the type of the message body to be extracted
 */
public final class Parse<R> implements Function<Message<?>, Optional<R>> {

    private final Format<R> format;

    private final Logger logger=service(logger());


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
        return message == null ? Optional.empty() : message.body(format).fold(Optional::of, error -> {

            // !!! get cause directly from failure

            final Response parse=new Response(message.request()).map(error);

            // !!! review formatting >> avoid newlines in log

            logger.error(this,
                    String.format("unable to parse message body as <%s>", format.getClass().getName()),
                    new RuntimeException(error.toString(), parse.cause().orElse(null))
            );

            return Optional.empty();

        });
    }

}
