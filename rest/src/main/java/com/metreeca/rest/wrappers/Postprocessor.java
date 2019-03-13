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


import com.metreeca.rest.Handler;
import com.metreeca.rest.Response;
import com.metreeca.rest.Wrapper;
import com.metreeca.rest.bodies.RDFBody;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.RDFBody.rdf;

import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;


/**
 * RDF postprocessor.
 *
 * <p>Processes response {@linkplain RDFBody RDF} payloads.</p>
 */
public final class Postprocessor implements Wrapper {

	private final Collection<BiFunction<Response, Model, Model>> filters;


	/**
	 * Creates an RDF preprocessor.
	 *
	 * <p>Filters are chained in the specified order and executed on {@linkplain Response#success() successful}
	 * outgoing responses and their {@linkplain RDFBody RDF} payload, if one is present, or ignored, otherwise.</p>
	 *
	 * @param filters the response RDF postprocessing filters to be inserted; each filter takes as argument a successful
	 *                outgoing response and its {@linkplain RDFBody RDF} payload and must return a non-null RDF model;
	 *                filters based on SPARQL Query/Update scripts may be created using {@link Connector} factory
	 *                methods
	 *
	 * @throws NullPointerException if {@code filters} is null or contains null values
	 * @see Connector#query(String, BiConsumer[])
	 * @see Connector#update(String, BiConsumer[])
	 */
	@SafeVarargs public Postprocessor(final BiFunction<Response, Model, Model>... filters) {
		this(asList(filters));
	}

	/**
	 * Inserts a response RDF postprocessing filters.
	 *
	 * <p>The filters is chained after previously inserted postprocessing filters and executed on {@linkplain
	 * Response#success() successful} outgoing responses and their {@linkplain RDFBody RDF} payload, if one is
	 * present, or ignored, otherwise.</p>
	 *
	 * @param filters the response RDF postprocessing filters to be inserted; each filter takes as argument a successful
	 *                outgoing response and its {@linkplain RDFBody RDF} payload and must return a non-null RDF model;
	 *                filters based on SPARQL Query/Update scripts may be created using {@link Connector} factory
	 *                methods
	 *
	 * @throws NullPointerException if {@code filters} is null or contains null values
	 * @see Connector#query(String, BiConsumer[])
	 * @see Connector#update(String, BiConsumer[])
	 */
	public Postprocessor(final Collection<BiFunction<Response, Model, Model>> filters) {

		if ( filters == null ) {
			throw new NullPointerException("null filters");
		}

		if ( filters.contains(null) ) {
			throw new NullPointerException("null filter");
		}

		this.filters=new ArrayList<>(filters);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null handler");
		}

		return new Connector()
				.wrap(postprocessor())
				.wrap(handler);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper postprocessor() {
		return handler -> request -> handler.handle(request)
				.map(response -> response.success() ? response.pipe(rdf(), model -> Value(filters.stream().reduce(

						(Model)new LinkedHashModel(model),

						(_model, filter) -> requireNonNull(
								filter.apply(response, new LinkedHashModel(_model)),
								"null filter return value"
						),

						(x, y) -> {

							x.addAll(y);

							return x;

						}

				))) : response);
	}

}
