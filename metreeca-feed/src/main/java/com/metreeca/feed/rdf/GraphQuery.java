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

package com.metreeca.feed.rdf;

import com.metreeca.rdf4j.services.Graph;
import com.metreeca.rest.services.Logger;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.QueryLanguage;

import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.services.Logger.logger;
import static com.metreeca.rest.services.Logger.time;

import static org.eclipse.rdf4j.common.iteration.Iterations.asList;


public final class GraphQuery implements Function<String, Stream<Statement>> {

	private Graph graph; // on-demand initialization

	private final Logger logger=service(logger());


	public GraphQuery graph(final Graph graph) {

		if ( graph == null ) {
			throw new NullPointerException("null graph");
		}

		this.graph=graph;

		return this;
	}


	@Override public Stream<Statement> apply(final String query) {
		return (graph != null ? graph : (graph=service(Graph.graph()))).exec(connection -> {
			return time(() ->

					asList(connection // statements must be retrieved inside txn
							.prepareGraphQuery(QueryLanguage.SPARQL, query, null)
							.evaluate()
					).parallelStream()

			).apply((t, v) -> logger.info(this, String.format("executed in <%,d> ms", t)));
		});
	}

}
