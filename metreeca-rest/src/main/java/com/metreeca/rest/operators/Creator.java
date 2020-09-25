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

package com.metreeca.rest.operators;


import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.*;
import com.metreeca.rest.assets.Engine;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.handlers.Delegator;

import org.eclipse.rdf4j.model.IRI;

import javax.json.JsonObject;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;

import static com.metreeca.json.shapes.Guard.Create;
import static com.metreeca.json.shapes.Guard.Detail;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.assets.Engine.*;
import static java.util.UUID.randomUUID;


/**
 * Model-driven resource creator.
 *
 * <p>Performs:</p>
 *
 * <ul>
 *
 * <li>{@linkplain Guard#Role role}-based request shape redaction and shape-based
 * {@linkplain Engine#throttler(Object, Object...) authorization}, considering shapes enabled by the
 * {@linkplain Guard#Create} task and the {@linkplain Guard#Detail} area;</li>
 *
 * <li>engine-assisted request payload {@linkplain JSONLDFormat#validate(IRI, Shape, JsonObject) validation};</li>
 *
 * <li>resource {@linkplain #creator(Function) slug} generation;</li>
 *
 * <li>engine assisted resource {@linkplain Engine#create(Request) creation}.</li>
 *
 * </ul>
 *
 * <p>All operations are executed inside a single {@linkplain Engine engine transaction}.</p>
 */
public final class Creator extends Delegator {

	private static final Object monitor=new Object();

	/**
	 * Creates a resource creator with a UUID-based slug generator.
	 *
	 * @return a new resource creator
	 */
	public static Creator creator() {
		return creator(request -> randomUUID().toString());
	}

	/**
	 * Creates a resource creator.
	 *
	 * @param slug a function mapping from the creation request to the identifier to be assigned to the newly created
	 *             resource; must return a non-null non-clashing value
	 *
	 * @return a new resource creator
	 *
	 * @throws NullPointerException if {@code slug} is null
	 */
	public static Creator creator(final Function<Request, String> slug) {

		if ( slug == null ) {
			throw new NullPointerException("null slug");
		}

		return new Creator(slug);
	}

	/**
	 * Creates a resource creator.
	 *
	 * @param <T>    the type of the message body to be inspected during slug generation
	 * @param format the format of the message body to be inspected during slug generation
	 * @param slug   a function mapping from the creation request and its payload to the identifier to be assigned to
	 *               the newly created resource; must return a non-null non-clashing value
	 *
	 * @return a new resource creator
	 *
	 * @throws NullPointerException if either {@code format} or {@code slug} is null
	 */
	public static <T> Creator creator(
			final Format<T> format, final BiFunction<? super Request, ? super T, String> slug
	) {
		return new Creator(request -> request.body(format).fold(error -> "", value -> slug.apply(request, value)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	private Creator(final Function<Request, String> slug) {

		final Engine engine=asset(engine());

		delegate(engine.wrap(wrapper(slug) // chain slug immediately before handler after custom wrappers

				.wrap(engine::create)

				.with(throttler(Create, Detail))

				.with(validator())

		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper wrapper(final Function<Request, String> slug) {
		return handler -> request -> consumer -> {
			synchronized ( monitor ) { // attempt to serialize slug operations from multiple snapshot txns
				handler.handle(request.header("Slug",

						Objects.requireNonNull(slug.apply(request), "null resource name")

				)).accept(consumer);
			}
		};
	}

}
