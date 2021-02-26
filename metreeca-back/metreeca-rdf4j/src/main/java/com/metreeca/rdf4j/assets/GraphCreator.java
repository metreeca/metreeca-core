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
import com.metreeca.json.Values;
import com.metreeca.rest.*;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.Optional;
import java.util.regex.Matcher;

import static com.metreeca.json.Values.*;
import static com.metreeca.rdf4j.assets.Graph.graph;
import static com.metreeca.rdf4j.assets.Graph.txn;
import static com.metreeca.rdf4j.assets.GraphFetcher.filter;
import static com.metreeca.rdf4j.assets.GraphFetcher.outline;
import static com.metreeca.rest.Context.asset;
import static com.metreeca.rest.Either.Left;
import static com.metreeca.rest.Either.Right;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.Created;
import static com.metreeca.rest.Response.InternalServerError;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.shape;

import static java.util.stream.Collectors.toList;


final class GraphCreator {

	private final Graph graph=asset(graph());


	Future<Response> handle(final Request request) {
		return request

				.body(jsonld())

				.flatMap(rdf -> graph.exec(txn(connection -> {

					final IRI target=iri(request.item());
					final IRI member=iri(request.item()+request.header("Slug") // assign entity a slug-based id
							.map(Xtream::encode)  // encode slug as IRI path component
							.orElseGet(Values::md5)
					);

					final Shape shape=request.attribute(shape());


					final boolean clashing=connection.hasStatement(member, null, null, true)
							|| connection.hasStatement(null, null, member, true);

					if ( clashing ) { // report clashing slug

						return Left(status(InternalServerError,
								new IllegalStateException("clashing entity slug {"+member+"}")
						));

					} else { // store model

						connection.add(outline(member, filter(shape)));
						connection.add(rewrite(member, target, rdf));

						final String location=member.stringValue();

						return Right(request.reply(status(Created, Optional // root-relative to support relocation
								.of(member.stringValue())
								.map(IRIPattern::matcher)
								.filter(Matcher::matches)
								.map(matcher -> matcher.group("pathall"))
								.orElse(location)
						)));

					}

				})))

				.fold(request::reply, future -> future);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> rewrite(final IRI target, final IRI source, final Collection<Statement> model) {
		return model.stream().map(statement -> rewrite(target, source, statement)).collect(toList());
	}

	private Statement rewrite(final IRI target, final IRI source, final Statement statement) {
		return statement(
				rewrite(target, source, statement.getSubject()),
				rewrite(target, source, statement.getPredicate()),
				rewrite(target, source, statement.getObject()),
				rewrite(target, source, statement.getContext())
		);
	}

	private <T extends Value> T rewrite(final T target, final T source, final T value) {
		return source.equals(value) ? target : value;
	}

}
