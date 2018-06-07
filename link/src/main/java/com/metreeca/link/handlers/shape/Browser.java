/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.link.handlers.shape;


import com.metreeca.link.Request;
import com.metreeca.link.Response;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.probes.Outliner;
import com.metreeca.spec.queries.Edges;
import com.metreeca.spec.sparql.SPARQLEngine;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Map;

import static com.metreeca.spec.Shape.*;
import static com.metreeca.spec.queries.Items.ItemsShape;
import static com.metreeca.spec.queries.Stats.StatsShape;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.things.Lists.concat;
import static com.metreeca.spec.things.Sets.union;
import static com.metreeca.spec.things.Values.rewrite;
import static com.metreeca.spec.things.Values.statement;
import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;


public final class Browser extends Shaper {

	public static Browser browser(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return new Browser(shape);
	}


	private final Shape shape;

	private final Graph graph=tool(Graph.Factory);


	private Browser(final Shape shape) {
		this.shape=shape
				.accept(task(Spec.relate))
				.accept(view(Spec.digest));
	}


	public boolean active() {
		return !empty(shape);
	}

	@Override public void handle(final Request request, final Response response) {
		authorize(request, response, shape, shape -> query(request, response, shape, filter -> {
			try (final RepositoryConnection connection=graph.connect()) {

				final IRI focus=request.focus();

				// split container/resource shapes and augment them with system generated properties

				// !!! handle ldp:contains shapes

				//final Shape container=and(all(focus), shape.accept(trimmer));
				//final Shape resource=traits(shape).getOrDefault(Contains, and());

				final Shape container=shape; // !!!
				final Shape resource=shape; // !!!

				// retrieve filtered content from repository

				final Map<Value, Collection<Statement>> matches=SPARQLEngine.browse(connection, filter);
				final Collection<Statement> model=matches.values().stream().reduce(emptyList(), (x, y) -> concat(x, y));

				// signal successful retrieval of the filtered container

				if ( request.query().isEmpty() ) { // base container: convert its shape to RDF and merge into results

					response.status(Response.OK).rdf(

							union(container.accept(mode(Spec.verify)).accept(new Outliner()), model),

							or(container, trait(LDP.CONTAINS, resource))

					);

				} else if ( filter instanceof Edges ) {

					response.status(Response.OK).rdf(

							union(matches.keySet().stream()
									.map(item -> statement(focus, LDP.CONTAINS, item))
									.collect(toList()), model),

							trait(LDP.CONTAINS, resource)

					);

				} else { //introspection query: rewrite query results to the target IRI

					response.status(Response.OK).rdf(

							rewrite(model, Spec.meta, focus),
							or(StatsShape, ItemsShape)

					);

				}

			}
		}));
	}

}
