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
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.util.Collection;
import java.util.Collections;
import java.util.function.BiFunction;

import static com.metreeca.spec.Shape.*;
import static com.metreeca.spec.queries.Items.ItemsShape;
import static com.metreeca.spec.queries.Stats.StatsShape;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.sparql.SPARQLEngine.browse;
import static com.metreeca.spec.sparql.SPARQLEngine.contains;
import static com.metreeca.spec.things.Sets.union;
import static com.metreeca.spec.things.Values.rewrite;
import static com.metreeca.tray.Tray.tool;


public final class Relator extends Shaper {

	public static Relator relator() {
		return new Relator();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Graph graph=tool(Graph.Factory);


	private Shape shape=and();

	private BiFunction<Request, Model, Model> pipe;

	private Relator() {}


	public boolean active() {
		return !empty(shape);
	}

	public Relator shape(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.shape=shape
				.accept(task(Spec.relate))
				.accept(view(Spec.detail));

		return this;
	}

	public Relator pipe(final BiFunction<Request, Model, Model> pipe) { // !!! abstract

		if ( pipe == null ) {
			throw new NullPointerException("null pipe");
		}

		this.pipe=chain(this.pipe, pipe);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public void handle(final Request request, final Response response) {
		authorize(request, response, shape, shape ->
				query(request, response, and(all(request.focus()), shape), query -> { // focused shape
					try (final RepositoryConnection connection=graph.connect()) {

						final IRI focus=request.focus();

						if ( !contains(connection, focus) ) {

							response.status(Response.NotFound).done();

						} else {

							final Collection<Statement> model=browse(connection, query)
									.values()
									.stream()
									.findFirst()
									.orElseGet(Collections::emptySet);

							final Collection<Statement> piped=(pipe == null) ?
									model : pipe.apply(request, new LinkedHashModel(model));

							if ( piped.isEmpty() ) { // resource known but empty envelope for the current user

								response.status(Response.Forbidden).done(); // !!! 404 under strict security

							} else {

								final Shape focused=and(all(focus), shape); // !!! review/remove focusing

								response.status(Response.OK).rdf( // !!! re/factor

										request.query().isEmpty()

												// base resource: convert its shape to RDF and merge into results

												? union(piped, focused.accept(mode(Spec.verify)).accept(new Outliner()))

												// filtered resource: return selected data

												: query instanceof Edges ? piped

												// introspection query: rewrite query results to the target IRI

												: rewrite(piped, Spec.meta, focus),

										// merge all possible shape elements to properly drive response formatting

										or(focused, StatsShape, ItemsShape)

								);

							}

						}

					}

				}));

	}

}
