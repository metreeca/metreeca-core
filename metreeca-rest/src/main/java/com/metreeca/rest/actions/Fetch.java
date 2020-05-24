/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */

package com.metreeca.rest.actions;


import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.services.Fetcher;
import com.metreeca.rest.services.Logger;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Context.service;
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

                        logger.warning(this, format("%d %s", response.status(), response.item()));

                    }

                    return success;

                });
    }

}
