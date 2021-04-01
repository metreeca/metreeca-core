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

import com.metreeca.rest.Request;
import com.metreeca.rest.services.Logger;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.Toolbox.service;

import static java.lang.String.format;


/**
 * Request generation.
 *
 * <p>Maps textual resource URIs to optional resource requests.</p>
 */
public final class Query implements Function<String, Optional<Request>> {

	private final Function<Request, Request> customizer;

	private final Logger logger=service(Logger.logger());


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
