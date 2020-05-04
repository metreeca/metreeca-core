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

import com.metreeca.rest.Request;
import com.metreeca.rest.services.Logger;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Pattern;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;
import static java.lang.String.format;


/**
 * Request generation.
 *
 * <p>Maps textual resource URLs to optional resource requests.</p>
 */
public final class Query implements Function<String, Optional<Request>> {

    private static final Pattern ItemPattern=Pattern.compile("(?<base>https?+://[^/]*)/?(?<path>.*)");


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final Function<Request, Request> customizer;


    private final Logger logger=service(logger());


    /**
     * Creates a new default request generator.
     */
    public Query() {
        this(request -> request);
    }

    /**
     * Creates a new customized request generator.
     *
     * @param customizer the request customizer
     *
     * @throws NullPointerException if {@code customizer} is null
     */
    public Query(final Function<Request, Request> customizer) {

        if ( customizer == null ) {
            throw new NullPointerException("null customizer");
        }

        this.customizer=customizer;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Generates a resource request
     *
     * @param url the textual URL of the target resource
     *
     * @return a optional GET or possibly customized request for the given resource {@code url}, if it was not null
     * and successfully parsed into a {@linkplain Request#base() base} and a {@linkplain Request#path() path}; an
     * empty optional, otherwise, logging an error to the {@linkplain Logger#logger() shared event logger}
     */
    @Override public Optional<Request> apply(final String url) {
        return Optional.ofNullable(url)

                .map(ItemPattern::matcher)

                .filter(matcher -> {

                    final boolean matches=matcher.matches();

                    if ( !matches ) {

                        logger.error(this, format("unable to parse resource URL <%s>", url));

                    }

                    return matches;
                })

                .map(matcher -> new Request()

                        .method(Request.GET)

                        .base(matcher.group("base")+'/')
                        .path('/'+matcher.group("path"))

                )

                .map(customizer);
    }

}
