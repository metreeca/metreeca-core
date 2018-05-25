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

package com.metreeca.link.handlers;

import com.metreeca.link.*;
import com.metreeca.spec.Issue.Level;
import com.metreeca.spec.*;
import com.metreeca.spec.codecs.QueryParser;
import com.metreeca.spec.codecs.ShapeCodec;
import com.metreeca.spec.probes.Inferencer;
import com.metreeca.spec.probes.Optimizer;
import com.metreeca.spec.probes.Outliner;
import com.metreeca.spec.things.*;
import com.metreeca.spec.things.Transputs;
import com.metreeca.tray.Tool;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.sys.Setup;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.BiConsumer;

import static com.metreeca.link._Handler.unauthorized;
import static com.metreeca.link._Handler.unsupported;
import static com.metreeca.spec.Shape.*;
import static com.metreeca.spec.queries.Items.ItemsShape;
import static com.metreeca.spec.queries.Stats.StatsShape;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Or.or;
import static com.metreeca.spec.things.Values.*;
import static com.metreeca.spec.things._Cell.cell;


/**
 * Model-driven LDP RDF Resource handler.
 *
 * @see "http://www.w3.org/TR/ldp/"
 */
public final class Resource implements _Handler { // !!! rename to avoid clashes with RDF4J

	private final Graph graph;

	private final Shape shape;

	private final _Handler dispatcher=new Dispatcher(Maps.map(Maps.entry(_Request.GET, this::get), Maps.entry(_Request.PUT, _Handler.sysadm(this::put)), // !!! remove after testing shape-based authorization
			Maps.entry(_Request.DELETE, _Handler.sysadm(this::delete)) // !!! remove after testing shape-based authorization
	));


	public Resource(final Tool.Loader tools, final Shape shape) {

		if ( tools == null ) {
			throw new NullPointerException("null tools");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		final Setup setup=tools.get(Setup.Tool);

		this.graph=tools.get(Graph.Tool);

		this.shape=shape.accept(new Optimizer());
	}


	@Override public void handle(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		dispatcher.handle(tools, request, response, (_request, _response) -> {

			if ( _response.getStatus() == _Response.OK ) {
				_response.addHeader("Link", format(Link.ShapedResource)+"; rel=\"type\"")
						.addHeader("Link", "<http://www.w3.org/ns/ldp#RDFResource>; rel=\"type\"")
						.addHeader("Link", "<http://www.w3.org/ns/ldp#Resource>; rel=\"type\"")
						.addHeader("Link", String.format("<%s?specs>; rel=\"%s\"", _request.getTarget(), LDP.CONSTRAINED_BY));
			}

			sink.accept(_request, _response);

		});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void get(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		final IRI target=iri(request.getTarget());
		final String query=request.getQuery();

		final Shape relating=shape.accept(task(Spec.relate));
		final Shape authorized=relating.accept(role(request.getRoles()));

		if ( empty(relating) ) {

			unsupported(tools, request, response, sink);

		} else if ( empty(authorized) ) {

			unauthorized(tools, request, response, sink);

		} else if ( query.equals("specs") ) { // !!! review / factor

			// !!! specs query can't be processed as a regular query as it requires visibility on all tasks
			// !!! user redaction must be performed before task redaction (ie reversed wrt regular processing)

			final IRI iri=iri(target+"?"+query);
			final Collection<Statement> model=new ArrayList<>();

			model.add(statement(target, LDP.CONSTRAINED_BY, iri));

			final Shape shape=this.shape
					.accept(role(request.getRoles()))
					.accept(mode(Spec.verify))
					.accept(new Inferencer())
					.accept(new Optimizer());

			final ShapeCodec codec=new ShapeCodec();

			for (final IRI task : Lists.list(Spec.relate, Spec.update, Spec.delete)) {

				final Shape spec=shape.accept(task(task));

				if ( !empty(spec) ) {
					model.add(statement(iri, task, codec.encode(spec, model)));
				}

			}

			response.setStatus(_Response.OK);

			new _Transfer(request, response).model(model, and() /* !!! SpecsShape*/);

			sink.accept(request, response);

		} else {

			final Shape shape=and(all(target), authorized);

			// !!! test resource existence inside main query (currently returns implied statements…)

			final Graph graph=request.map(this.graph);

			if ( !graph.contains(target) ) {

				throw new _LinkException(_Response.NotFound);

			} else {

				// construct and process configured query, merging constraints from the query string

				final Query filter;
				try {
					filter=new QueryParser(shape).parse(Transputs.decode(query));
				} catch ( final RuntimeException e ) {
					throw new _LinkException(_Response.BadRequest, "malformed query: "+e.getMessage(), e);
				}

				final _Cell cell=graph.get(filter);

				if ( cell.values().isEmpty() ) { // resource known but empty envelope for the current user
					throw new _LinkException(_Response.Forbidden); // !!! return 404 under strict security
				}

				response.setStatus(_Response.OK);

				new _Transfer(request, response).model( // !!! re/factor

						query.isEmpty()

								// base resource: convert its shape to RDF and merge into results

								? Sets.union(cell.model(), shape.accept(mode(Spec.verify)).accept(new Outliner()))

								// filtered resource: return selected data

								: filter instanceof com.metreeca.spec.queries.Graph ? cell.model()

								// introspection query: rewrite query results to the target IRI

								: rewrite(cell.model(), Spec.meta, target),

						// merge all possible shape elements to properly drive response formatting

						or(shape, StatsShape, ItemsShape)

				);

				sink.accept(request, response);

			}
		}
	}

	private void put(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		final Graph graph=request.map(this.graph);

		final Shape updating=shape.accept(task(Spec.update));
		final Shape authorized=updating.accept(role(request.getRoles()));

		if ( empty(updating) ) {

			unsupported(tools, request, response, sink);

		} else if ( empty(authorized) || !graph.isTransactional() && !request.isSysAdm() ) {

			unauthorized(tools, request, response, sink);

		} else {

			final IRI target=iri(request.getTarget());
			final Shape shape=and(all(target), authorized);

			final Collection<Statement> model=new ArrayList<>();

			model.addAll(new _Transfer(request, response).model(shape)); // add user-submitted statements
			model.addAll(shape.accept(mode(Spec.verify)).accept(new Outliner())); // add implied statements

			graph.update(connection -> {

				// !!! remove system-generated properties (e.g. rdf:types inferred from Link header)

				if ( !graph.contains(target) ) {

					// !!! 410 Gone if the resource is known to have existed (how to test?)

					throw new _LinkException(_Response.NotFound);  // !!! convert to status code outside update txn

				} else {

					// identify and remove updatable cell

					connection.remove(graph.get(shape).model());

					// upload and validate submitted statements

					final Report report=graph.set(shape, cell(model).insert(target));

					if ( report.assess(Level.Error) ) { // shape violations

						throw new _LinkException(_Response.UnprocessableEntity, report // !!! convert to status code outside update txn
								.prune(Level.Warning).map(Report::toString).orElse("") // prune for readability
						);

					} else { // valid data

						// execute post-processing updates

						// !!! compute delta for postprocessing script
						// !!! supply delta to script (pre-post update hooks? automatic memoization?)
						// !!! factor with ContentHandler and decouple

						// signal successful update of the resource (updated description included by server postprocessing)

						sink.accept(request, response.setStatus(_Response.NoContent));

						return this;

					}

				}

			});

		}

	}

	private void delete(final Tool.Loader tools, final _Request request, final _Response response, final BiConsumer<_Request, _Response> sink) {

		final Graph graph=request.map(this.graph);

		final Shape deleting=shape.accept(task(Spec.delete));
		final Shape authorized=deleting.accept(role(request.getRoles()));

		if ( empty(deleting) ) {

			unsupported(tools, request, response, sink);

		} else if ( empty(authorized) || !graph.isTransactional() && !request.isSysAdm() ) {

			unauthorized(tools, request, response, sink);

		} else {

			final IRI target=iri(request.getTarget());
			final Shape shape=and(all(target), authorized);

			graph.update(connection -> {

				if ( !graph.contains(target) ) {

					// !!! 410 Gone if the resource is known to have existed (how to test?)

					response.setStatus(_Response.NotFound);  // !!! convert to status code outside update txn

				} else { // !!! merge remove operations

					connection.remove(graph.get(shape).model()); // identify and remove deletable cell
					connection.remove(shape.accept(mode(Spec.verify)).accept(new Outliner())); // remove implied statements

					response.setStatus(_Response.NoContent); // signal successful deletion of the resource

				}

				sink.accept(request, response);

				return null;

			});

		}
	}

}

