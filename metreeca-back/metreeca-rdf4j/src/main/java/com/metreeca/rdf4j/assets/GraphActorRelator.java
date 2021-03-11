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

package com.metreeca.rdf4j.assets;

import com.metreeca.json.Query;
import com.metreeca.json.Shape;
import com.metreeca.json.queries.*;
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.assets.Engine.StatsShape;
import static com.metreeca.rest.assets.Engine.TermsShape;
import static com.metreeca.rest.formats.JSONLDFormat.*;


final class GraphActorRelator {

	private final Options options;


	GraphActorRelator(final Options options) {
		this.options=options;
	}


	Future<Response> handle(final Request request) {

		final IRI item=iri(request.item());
		final Shape shape=and(all(item), request.attribute(shape()));

		return query(item, shape, request.query()).fold(request::reply, query ->
				request.reply(response -> Optional

						.of(query.map(new GraphFetcher(item, options)))

						.filter(model -> !model.isEmpty())

						.map(model -> response.status(OK)
								.attribute(shape(), query.map(new ShapeProbe()))
								.body(jsonld(), model)
						)

						.orElseGet(() -> response.status(NotFound)) // !!! 410 Gone if previously known
				)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ShapeProbe extends Query.Probe<Shape> {

		@Override public Shape probe(final Items items) {
			return items.shape(); // !!! add ldp:contains if items.path is not empty
		}

		@Override public Shape probe(final Stats stats) {
			return StatsShape(stats);
		}

		@Override public Shape probe(final Terms terms) {
			return TermsShape(terms);
		}

	}

}