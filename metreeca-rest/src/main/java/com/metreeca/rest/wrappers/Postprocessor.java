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
 * Response body preprocessor.
 *
 * @param <V> the type of the response {@linkplain Message#body(Format) body} representation to be preprocessed
 */
public final class Postprocessor<V> implements Wrapper {

	private final Format<V> format;
	private final BiFunction<Response, V, V> mapper;


	/**
	 * Creates a postprocessor.
	 *
	 * @param format the format of the response body representation to be postprocessed
	 * @param mapper the response body representation mapper; takes as argument a response and its body representation
	 *               for {@code format} and must return a non-null updated value
	 *
	 * @throws NullPointerException if either {@code format} or {@code mapper} is null
	 */
	public Postprocessor(final Format<V> format, final BiFunction<Response, V, V> mapper) {

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
		return request -> handler.handle(request).map(response -> response.success() ? response.body(format).fold(

				body -> response.body(format, requireNonNull(
						mapper.apply(response, body),
						"null mapper return value"
				)),

				response::map

		) : response);
	}

}
