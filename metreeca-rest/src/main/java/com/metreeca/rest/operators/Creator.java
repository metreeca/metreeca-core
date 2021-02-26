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

package com.metreeca.rest.operators;


import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Guard;
import com.metreeca.rest.*;
import com.metreeca.rest.assets.Engine;
import com.metreeca.rest.formats.JSONLDFormat;
import com.metreeca.rest.handlers.Delegator;

import org.eclipse.rdf4j.model.IRI;

import java.util.Objects;
import java.util.function.BiFunction;
import java.util.function.Function;
import javax.json.JsonObject;

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
 * <li>engine-assisted request payload {@linkplain JSONLDFormat#scan(IRI, Shape, JsonObject) scanning};</li>
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

		delegate(wrapper(slug).wrap(engine::create) // wrap slug immediately before handler after custom wrappers

				.with(engine)

				.with(throttler(Create, Detail))

				.with(scanner())

		);
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
