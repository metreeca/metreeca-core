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
import com.metreeca.spec.things._Cell;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;

import static com.metreeca.spec.Shape.mode;
import static com.metreeca.spec.Shape.task;
import static com.metreeca.spec.Shape.view;
import static com.metreeca.spec.queries.Items.ItemsShape;
import static com.metreeca.spec.queries.Stats.StatsShape;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.sparql.SPARQLEngine._browse;
import static com.metreeca.spec.things.Sets.union;
import static com.metreeca.spec.things.Values.rewrite;
import static com.metreeca.tray.Tray.tool;


public final class Browser extends Shaper {

	public static Browser browser(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return new Browser(shape);
	}


	private final Shape shape;

	private final Graph graph=tool(Graph.Tool);


	private Browser(final Shape shape) {
		this.shape=shape
				.accept(task(Spec.relate))
				.accept(view(Spec.digest));
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

				// construct and process configured query, merging constraints from the query string

				// retrieve filtered content from repository

				final _Cell cell=_browse(connection, filter); // !!! remove Cell
				final Collection<Statement> model=cell.model();

				if ( filter instanceof com.metreeca.spec.queries.Graph ) {
					cell.reverse(LDP.CONTAINS).insert(focus);
				}

				// signal successful retrieval of the filtered container

				response.status(Response.OK).rdf( // !!! re/factor

						request.query().isEmpty()

								// base container: convert its shape to RDF and merge into results

								? union(model, container.accept(mode(Spec.verify)).accept(new Outliner()))

								// filtered container: return selected data

								: filter instanceof com.metreeca.spec.queries.Graph ? model

								// introspection query: rewrite query results to the target IRI

								: rewrite(model, Spec.meta, focus),

						// merge all possible shape elements to properly drive response formatting

						or(container, trait(LDP.CONTAINS, resource), StatsShape, ItemsShape)

				);

			}
		}));
	}

}
