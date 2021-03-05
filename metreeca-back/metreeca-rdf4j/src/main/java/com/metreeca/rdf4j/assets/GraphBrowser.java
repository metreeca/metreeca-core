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
import org.eclipse.rdf4j.model.vocabulary.LDP;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rdf4j.assets.GraphEngine.StatsShape;
import static com.metreeca.rdf4j.assets.GraphEngine.TermsShape;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.formats.JSONLDFormat.*;

final class GraphBrowser {

	private final Options options;

	private final Graph graph=asset(graph());


	GraphBrowser(final Options options) {
		this.options=options;
	}


	Future<Response> handle(final Request request) {

		final IRI item=iri(request.item());
		final Shape shape=request.attribute(shape());

		return query(item, shape, request.query()).fold(request::reply, query ->
				request.reply(response -> graph.exec(connection -> {

					return response.status(OK) // containers are virtual and respond always with 200 OK
							.attribute(shape(), query.map(new ShapeProbe()))
							.body(jsonld(), query.map(new GraphFetcher(connection, item, options)));

				}))
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class ShapeProbe extends Query.Probe<Shape> {

		@Override public Shape probe(final Items items) {
			return field(LDP.CONTAINS, items.shape());
		}

		@Override public Shape probe(final Stats stats) {
			return StatsShape(stats);
		}

		@Override public Shape probe(final Terms terms) {
			return TermsShape(terms);
		}

	}

}
