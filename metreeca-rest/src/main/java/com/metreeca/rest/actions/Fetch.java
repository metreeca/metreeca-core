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
import com.metreeca.rest.services.Cache;
import com.metreeca.rest.services.Fetcher;
import com.metreeca.rest.services.Logger;

import java.io.*;
import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Request.GET;
import static com.metreeca.rest.formats.DataFormat.data;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.TextFormat.text;
import static com.metreeca.rest.services.Fetcher.fetcher;
import static com.metreeca.rest.services.Logger.logger;
import static java.lang.String.format;


public final class Fetch implements Function<Request, Optional<Response>> {

	private final Function<Request, Request> limit=new Limit<>(0);

	private final Fetcher fetcher=service(fetcher());
	private final Logger logger=service(logger());


	@Override public Optional<Response> apply(final Request request) {
		return Optional.ofNullable(request)

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
