/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.wrappers;


import com.metreeca.rest.*;

import java.util.function.BiFunction;

import static java.util.Objects.requireNonNull;


/**
 * Request body preprocessor.
 *
 * @param <V> the type of the request {@linkplain Message#body(Format) body} representation to be preprocessed
 */
public final class Preprocessor<V> implements Wrapper {

	private final Format<V> format;
	private final BiFunction<Request, V, V> mapper;


	/**
	 * Creates a preprocessor.
	 *
	 * @param format the format of the request body representation to be preprocessed
	 * @param mapper the request body representation mapper; takes as argument a request and its body representation for
	 *               {@code format} and must return a non-null updated value
	 *
	 * @throws NullPointerException if either {@code format} or {@code mapper} is null
	 */
	public Preprocessor(final Format<V> format, final BiFunction<Request, V, V> mapper) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		if ( mapper == null ) {
			throw new NullPointerException("null mapper");
		}

		this.format=format;
		this.mapper=mapper;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return request -> request.body(format).fold(

				body -> handler.handle(request.body(format, requireNonNull(
						mapper.apply(request, body),
						"null mapper return value"
				))),

				request::reply

		);
	}

}
