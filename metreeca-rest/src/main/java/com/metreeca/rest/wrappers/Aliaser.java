/*
 * Copyright © 2013-2020 Metreeca srl
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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import java.net.URI;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.SeeOther;
import static java.util.Objects.requireNonNull;


/**
 * Resource aliaser.
 *
 * <p>Redirects request for alias resources to the canonical resource they {@linkplain #aliaser(Function) resolve}
 * to.</p>
 *
 * <p>Empty or idempotent requests, that is requests whose {@link Request#item() focus item} is resolved to an empty
 * optional, to an empty string or to itself, are delegated to the wrapped handler.</p>
 */
public final class Aliaser implements Wrapper {

	/**
	 * Creates a resource aliaser.
	 *
	 * @param resolver the resource resolving function; takes as argument a request and returns the canonical IRI for
	 *                 the aliased request {@linkplain Request#item() item}, if one was identified, or an empty
	 *                 optional, otherwise
	 *
	 * @return a new resource aliaser
	 *
	 * @throws NullPointerException if {@code resolver} is null or returns a null value
	 */
	public static Aliaser aliaser(final Function<Request, Optional<String>> resolver) {

		if ( resolver == null ) {
			throw new NullPointerException("null resolver");
		}

		return new Aliaser(resolver);
	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Function<Request, Optional<String>> resolver;


	private Aliaser(final Function<Request, Optional<String>> resolver) {
		this.resolver=resolver;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> alias(request).orElseGet(() -> handler.handle(request));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<Future<Response>> alias(final Request request) {
		return requireNonNull(resolver.apply(request), "null resolver return value")

				.filter(resource -> !resource.isEmpty())
				.filter(resource -> !idempotent(request.item(), resource))

				.map(resource -> request.reply(status(SeeOther, resource)));
	}


	private boolean idempotent(final String item, final String resource) {
		return item.equals(URI.create(item).resolve(resource).toString());
	}

}
