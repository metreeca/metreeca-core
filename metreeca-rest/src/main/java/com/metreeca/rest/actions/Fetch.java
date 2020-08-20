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
import com.metreeca.rest.Response;
import com.metreeca.rest.services.Fetcher;
import com.metreeca.rest.services.Logger;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.rest.services.Logger.logger;
import static java.lang.String.format;


/**
 * Resource fetching.
 *
 * <p>Maps resource requests to optional responses.</p>
 */
public final class Fetch implements Function<Request, Optional<Response>> {

    private Function<Request, Request> limit=new Limit<>(0);

    private Fetcher fetcher=service(Fetcher.fetcher());


    private final Logger logger=service(logger());


    /**
     * Configures the rate limit (default to no limit)
     *
     * @param limit the request processing rate limit
     *
     * @return this action
     *
     * @throws NullPointerException if {@code limit} is null
     */
    public Fetch limit(final Function<Request, Request> limit) {

        if ( limit == null ) {
            throw new NullPointerException("null limit");
        }

        this.limit=limit;

        return this;
    }

    /**
     * Configures the resource fetcher (defaults to the {@linkplain Fetcher#fetcher() shared resource fetcher})
     *
     * @param fetcher the resource fetcher
     *
     * @return this action
     *
     * @throws NullPointerException if {@code fetcher} is null
     */
    public Fetch fetcher(final Fetcher fetcher) {

        if ( fetcher == null ) {
            throw new NullPointerException("null fetcher");
        }

        this.fetcher=fetcher;

        return this;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    /**
     * Fetches a resource.
     *
     * @param request the request to be used for fetching the request; ignored if null
     *
     * @return an optional response, if the {@code request} was not null and successfully processed; an empty optional,
     * otherwise, logging an error to the {@linkplain Logger#logger() shared event logger}
     */
    @Override public Optional<Response> apply(final Request request) {
        return Optional

                .ofNullable(request)

                .map(limit)
                .map(fetcher)

                .filter(response -> {

                    final boolean success=response.success();

                    if ( !success ) {

                        logger.error(this, format("unable to retrieve data from <%s> : status %d (%s)",
                                response.item(), response.status(), response.body(text()).value().orElse("")
                        ));

                    }

                    return success;

                });
    }

}
