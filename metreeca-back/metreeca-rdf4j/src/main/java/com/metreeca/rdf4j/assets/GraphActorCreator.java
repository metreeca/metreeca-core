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


import com.metreeca.json.Shape;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;
import java.util.regex.Matcher;

import static com.metreeca.json.Values.IRIPattern;
import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.iri;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rdf4j.assets.Graph.txn;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.Created;
import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.shape;

import static java.lang.String.format;


final class GraphActorCreator implements Handler {

	private final Graph graph=asset(graph());


	@Override public Future<Response> handle(final Request request) {

		final IRI item=iri(request.item());
		final Shape shape=request.attribute(shape());

		return request.body(jsonld()).fold(request::reply, model ->
				request.reply(response -> graph.exec(txn(connection -> {

					final boolean clashing=connection.hasStatement(item, null, null, true)
							|| connection.hasStatement(null, null, item, true);

					if ( clashing ) { // report clash

						return response.map(status(InternalServerError,
								new IllegalStateException(format("clashing resource identifier %s", format(item)))
						));

					} else { // store model

						connection.add(shape.outline(item));
						connection.add(model);

						final String location=item.stringValue();

						return response.status(Created)
								.header("Location", Optional // root-relative to support relocation
										.of(item.stringValue())
										.map(IRIPattern::matcher)
										.filter(Matcher::matches)
										.map(matcher -> matcher.group("pathall"))
										.orElse(location)
								);

					}

				})))
		);
	}

}
