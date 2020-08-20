/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Demo.
 *
 * Metreeca/Demo is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Demo is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Demo.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import static java.util.Objects.requireNonNull;


/**
 * Resource aliaser.
 *
 * <p>Redirects request for alias resources to the canonical resource they {@linkplain #Aliaser(Function) resolve}
 * to.</p>
 *
 * <p>May be used as either a {@linkplain Wrapper wrapper} or a {@linkplain Handler handler}; empty or idempotent
 * requests, that is requests whose {@link Request#item() focus item} is resolved to an e empty optional, to an empty
 * string or to itself, are:</p>
 *
 * <ul>
 * <li>delegated to the wrapped handler, if used as a wrapper;</li>
 * <li>reported with a {@link Response#NotFound} status code, if used as a handler.</li>
 * </ul>
 */
public final class Aliaser implements Wrapper, Handler {

    private final Function<Request, Optional<String>> resolver;


    /**
     * Creates a resource aliaser.
     *
     * @param resolver the resource resolving function; takes as argument a request and returns the canonical IRI for
     *                 the aliased request {@linkplain Request#item() item}, if one was identified, or an empty
     *                 optional, otherwise
     *
     * @throws NullPointerException if {@code resolver} is null or returns a null value
     */
    public Aliaser(final Function<Request, Optional<String>> resolver) {

        if ( resolver == null ) {
            throw new NullPointerException("null resolver");
        }

        this.resolver=resolver;
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    @Override public Handler wrap(final Handler handler) {
        return request -> alias(request).orElseGet(() -> handler.handle(request));
    }

    @Override public Future<Response> handle(final Request request) {
        return alias(request).orElseGet(() -> request.reply(Response.NotFound));
    }


    ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private Optional<Future<Response>> alias(final Request request) {
        return requireNonNull(resolver.apply(request), "null resolver return value")

                .filter(resource -> !resource.isEmpty())
                .filter(resource -> !idempotent(request.item(), resource))

                .map(resource -> request.reply(response -> response
                        .status(Response.SeeOther)
                        .header("Location", resource)
                ));
    }


    private boolean idempotent(final String item, final String resource) {
        return item.equals(URI.create(item).resolve(resource).toString());
    }

}
