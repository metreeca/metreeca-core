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

package com.metreeca.next.handlers.shape;


import com.metreeca.form.Shape;
import com.metreeca.next.*;
import com.metreeca.next.formats._RDF;
import com.metreeca.next.formats._Shape;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryResult;

import java.util.*;

import static com.metreeca.tray.Tray.tool;

import static java.util.Collections.singleton;


/**
 * Resource relator.
 *
 * <p>Handles retrieval requests on linked data resources</p>
 *
 * <p>If the request includes a {@link _Shape} body representation, the response includes the {@linkplain _RDF RDF
 * description} of the request {@linkplain Request#item() focus item}, as defined by the associated linked data
 * {@linkplain Shape shape} redacted taking into account the user {@linkplain Request#roles() roles} of the
 * request.</p>
 *
 * <p>Otherwise, the  response includes the symmetric concise bounded description of the request focus item, extended
 * with {@code rdfs:label/comment} annotations for all referenced IRIs.</p>
 *
 * @see <a href="https://www.w3.org/Submission/CBD/">CBD - Concise Bounded Description</a>
 */
public final class Relator implements Handler {

	private final Graph graph=tool(Graph.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	//* @see <a href="https://www.w3.org/TR/ldp/#ldprs">Linked Data Platform 1.0 - §4.3 RDF Source</a>

	//	.headers("Link",
	//		"<http://www.w3.org/ns/ldp#RDFSource>; rel=\"type\"",
	//		"<http://www.w3.org/ns/ldp#Resource>; rel=\"type\""
	//)


	//public Relator shape(final Shape shape) {
	//
	//	if ( shape == null ) {
	//		throw new NullPointerException("null shape");
	//	}
	//
	//	this.shape=shape
	//			.accept(task(Form.relate))
	//			.accept(view(Form.detail));
	//
	//	return this;
	//}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Responder handle(final Request request) {

		return request.body(_Shape.Format).map(
				value -> {
					throw new UnsupportedOperationException("to be implemented"); // !!! tbi
					 },
				error -> description(request)
		);

		//authorize(request, response, shape, shape ->
		//		query(request, response, and(All.all(request.focus()), shape), query -> { // focused shape
		//			graph.browse(connection -> {
		//
		//				final IRI focus=request.item();
		//
		//				if ( !contains(connection, focus) ) {
		//
		//					return request.reply(response -> response.status(Response.NotFound));
		//
		//				} else {
		//
		//					final Collection<Statement> model=SPARQLEngine.browse(connection, query)
		//							.values()
		//							.stream()
		//							.findFirst()
		//							.orElseGet(Collections::emptySet);
		//
		//					if ( model.isEmpty() ) { // resource known but empty envelope for the current user
		//
		//						return request.reply(response -> response.status(Response.Forbidden)); // !!! 404 under strict security
		//
		//					} else {
		//
		//						final Shape focused=and(all(focus), shape); // !!! review/remove focusing
		//
		//						return request.reply(response -> response.status(Response.OK).body(_RDF.Format,  // !!! re/factor
		//
		//								request.query().isEmpty()
		//
		//										// base resource: convert its shape to RDF and merge into results
		//
		//										? union(model, focused.accept(mode(Form.verify)).accept(new Outliner()))
		//
		//										// filtered resource: return selected data
		//
		//										: query instanceof Edges ? model
		//
		//										// introspection query: rewrite query results to the target IRI
		//
		//										: rewrite(model, Form.meta, focus),
		//
		//								// merge all possible shape elements to properly drive response formatting
		//
		//								or(focused, StatsShape, ItemsShape)
		//						));
		//
		//					}
		//
		//				}
		//
		//			});
		//
		//		}));

	}

	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder description(final Request request) { // !!! optimize for SPARQL
		return graph.browse(connection -> {

			final IRI focus=request.item();
			final Collection<Statement> description=new LinkedHashModel();

			final Queue<Value> pending=new ArrayDeque<>(singleton(focus));
			final Collection<Value> visited=new HashSet<>();

			while ( !pending.isEmpty() ) {

				final Value value=pending.remove();

				if ( visited.add(value) ) {
					if ( value.equals(focus) || value instanceof BNode ) {

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								(Resource)value, null, null, true
						)) {
							while ( statements.hasNext() ) {

								final Statement statement=statements.next();

								description.add(statement);
								pending.add(statement.getObject());
							}
						}

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								null, null, value, true
						)) {
							while ( statements.hasNext() ) {

								final Statement statement=statements.next();

								description.add(statement);
								pending.add(statement.getSubject());
							}
						}

					} else if ( value instanceof IRI ) {

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								(Resource)value, RDFS.LABEL, null, true
						)) {
							while ( statements.hasNext() ) { description.add(statements.next()); }
						}

						try (final RepositoryResult<Statement> statements=connection.getStatements(
								(Resource)value, RDFS.COMMENT, null, true
						)) {
							while ( statements.hasNext() ) { description.add(statements.next()); }
						}

					}
				}

			}

			return request.reply(response -> response.status(Response.OK).body(_RDF.Format, description));

		});

	}

}
