/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;


import com.metreeca.rest.*;
import com.metreeca.rest.formats.RDFFormat;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import java.util.function.BiFunction;

import static com.metreeca.rest.Result.Value;

import static java.util.Objects.requireNonNull;


/**
 * RDF processor.
 *
 * <p>Processes request and response {@linkplain RDFFormat RDF} payloads and executes SPARQL Update housekeeping
 * scripts.</p>
 */
public final class Processor implements Wrapper {

	private BiFunction<Request, Model, Model> pre;
	private BiFunction<Response, Model, Model> post;


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Inserts a request RDF pre-processing filter.
	 *
	 * <p>The filter is chained after previously inserted pre-processing filters and executed on incoming requests and
	 * their {@linkplain RDFFormat RDF} payload, if one is present, or ignored, otherwise.</p>
	 *
	 * @param filter the request RDF pre-processing filter to be inserted; takes as argument an incoming request and its
	 *               {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 */
	public Processor pre(final BiFunction<Request, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		this.pre=chain(pre, filter);

		return this;
	}

	/**
	 * Inserts a response RDF post-processing filter.
	 *
	 * <p>The filter is chained after previously inserted post-processing filters and executed on {@linkplain
	 * Response#success() successful} outgoing responses and their {@linkplain RDFFormat RDF} payload, if one is
	 * present, or ignored, otherwise.</p>
	 *
	 * @param filter the response RDF post-processing filter to be inserted; takes as argument a successful outgoing
	 *               response and its {@linkplain RDFFormat RDF} payload and must return a non null filtered RDF model
	 *
	 * @return this processor
	 *
	 * @throws NullPointerException if {@code filter} is null
	 */
	public Processor post(final BiFunction<Response, Model, Model> filter) {

		if ( filter == null ) {
			throw new NullPointerException("null filter");
		}

		this.post=chain(post, filter);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return new Connector()
				.wrap(preprocessor())
				.wrap(postprocessor())
				.wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper preprocessor() {
		return handler -> request -> handler.handle(process(request, pre));
	}

	private Wrapper postprocessor() {
		return handler -> request -> handler.handle(request)
				.map(response -> response.success() ? process(response, post) : response);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private <T extends Message<T>> BiFunction<T, Model, Model> chain(
			final BiFunction<T, Model, Model> pipeline, final BiFunction<T, Model, Model> filter
	) {

		final BiFunction<T, Model, Model> checked=(request, model) ->
				requireNonNull(filter.apply(request, model), "null filter return value");

		return (pipeline == null) ? checked
				: (request, model) -> checked.apply(request, pipeline.apply(request, model));
	}

	private <T extends Message<T>> T process(final T message, final BiFunction<T, Model, Model> filter) {
		return message.pipe(RDFFormat.rdf(), statements -> Value(
				filter == null ? statements : filter.apply(message, new LinkedHashModel(statements))
		));
	}

}
