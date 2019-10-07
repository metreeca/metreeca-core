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

package com.metreeca.rest.handlers;


import com.metreeca.rest.Format;
import com.metreeca.rest.Request;
import com.metreeca.rest.Wrapper;
import com.metreeca.tree.Shape;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static java.util.UUID.randomUUID;


/**
 * Model-driven resource creator.
 */
public final class Creator extends Actor { // !!! tbd

	/**
	 * Creates a resource creator with a UUID-based slug generator.
	 */
	public Creator() {
		this(request -> randomUUID().toString());
	}

	/**
	 * Creates a resource creator.
	 *
	 * @param slug a function mapping from the creation request to the identifier to be assigned to the newly created
	 *             resource; must return a non-null non-clashing value
	 *
	 * @throws NullPointerException if {@code slug} is null
	 */
	public Creator(final Function<Request, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		delegate(wrapper(slug).wrap(creator()) // chain slug immediately before handler after custom wrappers

				.with(connector())
				.with(throttler(Shape.Create, Shape.Detail))
				.with(validator())

		);
	}

	/**
	 * Creates a resource creator.
	 *
	 * @param <T>    the type of the message body to be inspected during slug generation
	 * @param format the format of the message body to be inspected during slug generation
	 * @param slug   a function mapping from the creation request and its payload to the identifier to be assigned to
	 *               the newly created resource; must return a non-null non-clashing value
	 *
	 * @throws NullPointerException if either {@code format} or {@code slug} is null
	 */
	public <T> Creator(final Format<T> format, final BiFunction<Request, T, String> slug) {
		this(request -> request.body(format).fold(value -> slug.apply(request, value), failure -> ""));
	}


	private Wrapper wrapper(final Function<Request, String> slug) {
		return handler -> request -> consumer -> {
			synchronized ( creator() ) { // attempt to serialize slug operations from multiple snapshot txns
				handler.handle(request.header("Slug",

						Objects.requireNonNull(slug.apply(request), "null resource name")

				)).accept(consumer);
			}
		};
	}

}
