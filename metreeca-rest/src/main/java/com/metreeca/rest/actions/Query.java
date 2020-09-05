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

import com.metreeca.rest.Context;
import com.metreeca.rest.Request;
import com.metreeca.rest.assets.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.assets.Logger.logger;
import static java.lang.String.format;


/**
 * Request generation.
 *
 * <p>Maps textual resource URIs to optional resource requests.</p>
 */
public final class Query implements Function<String, Optional<Request>> {

	private final Function<Request, Request> customizer;

	private final Logger logger=Context.asset(logger());


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
	 * @param resource the textual URI of the target resource
	 *
	 * @return a optional GET or possibly customized request for the given resource {@code resource}, if it was not
	 * null and successfully parsed into absolute {@linkplain Request#base() base}, {@linkplain Request#path() path}
	 * and {@linkplain Request#query() query} components; an empty optional, otherwise, logging an error to the
	 * {@linkplain Logger#logger() shared event logger}
	 */
	@Override public Optional<Request> apply(final String resource) {
		return Optional.ofNullable(resource)

				.map(uri -> {
					try {

						return new URI(uri).normalize();

					} catch ( final URISyntaxException e ) {

						logger.error(this, format("unable to parse resource URI <%s>", uri));

						return null;

					}
				})

				.filter(URI::isAbsolute)

				.map(uri -> new Request()

						.method(Request.GET)

						.base(uri.getScheme()+":"+Optional
								.ofNullable(uri.getRawAuthority())
								.map(s -> "//"+s+"/")
								.orElse("/")
						)

						.path(Optional.ofNullable(uri.getRawPath()).orElse("/"))
						.query(Optional.ofNullable(uri.getRawQuery()).orElse(""))

				)

				.map(customizer);
	}

}
