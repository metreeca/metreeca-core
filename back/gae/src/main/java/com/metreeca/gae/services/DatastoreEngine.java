/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.gae.services;

import com.metreeca.gae.GAE;
import com.metreeca.rest.*;
import com.metreeca.rest.codecs.Codecs;
import com.metreeca.rest.services.Engine;
import com.metreeca.tree.Query;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Inferencer;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Redactor;
import com.metreeca.tree.queries.*;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.appengine.api.datastore.Query.FilterPredicate;

import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;

import static java.lang.String.format;


public final class DatastoreEngine implements Engine {

	private final Datastore datastore=service(datastore());


	@Override public <R> R exec(final Supplier<R> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		return datastore.exec(service -> task.get());
	}

	@Override public Future<Response> handle(final Request request) {
		switch ( request.method() ) {

			case Request.GET: return relate(request);
			case Request.POST: return create(request);
			case Request.PUT: throw new UnsupportedOperationException("to be implemented"); // !!! tbi
			case Request.DELETE: throw new UnsupportedOperationException("to be implemented"); // !!! tbi

			default: return request.reply(failure(format("%s method", request.method())));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> relate(final Request request) {
		if ( request.container() ) {

			final Query query=null; // !!! parse query

			return query.map(new Query.Probe<Future<Response>>() {

				@Override public Future<Response> probe(final Items items) {
					throw new UnsupportedOperationException("to be implemented"); // !!! tbi
				}

				@Override public Future<Response> probe(final Terms terms) {
					throw new UnsupportedOperationException("to be implemented"); // !!! tbi
				}

				@Override public Future<Response> probe(final Stats stats) {
					throw new UnsupportedOperationException("to be implemented"); // !!! tbi
				}

				@Override public Future<Response> probe(final Links links) {
					throw new UnsupportedOperationException("to be implemented"); // !!! tbi
				}

			});

		} else {

			return request

					.shape(request.shape() // !!! identify/cache

							.map(new Redactor(Shape.Role, request.roles()))
							.map(new Redactor(Shape.Task, Shape.Relate))
							.map(new Redactor(Shape.View, Shape.Detail))

							.map(new Splitter(true, GAE.Contains))
							.map(new Inferencer())
							.map(new Optimizer())

					)

					.reply(response -> datastore.exec(service -> {

						final Shape shape=request.shape();


						final String kind="Office"; // !!! extract from shape w/in decoder
						final String name=request.path();

						final com.google.appengine.api.datastore.Query query=new com.google.appengine.api.datastore.Query(kind)
								.setFilter(new FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
										FilterOperator.EQUAL, KeyFactory.createKey(kind, name)
								));

						// !!! set filters from filter shape
						// !!! trim results to convey shape

						// ;( projecting only properties actually included in the shape would lower costs as projection
						// queries are counted as small operations: unfortunately, a number of limitations apply:
						// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries#Java_Limitations_on_projections
						// https://cloud.google.com/appengine/docs/standard/java/datastore/projectionqueries#Java_Projections_and_multiple_valued_properties

						return Optional.ofNullable(service.prepare(query).asSingleEntity())

								.map(entity -> response
										.status(OK)
										.shape(shape)
										.body(entity(), entity)
								)

								.orElseGet(() -> response
										.status(NotFound)
								);

					}));

		}
	}

	private Future<Response> create(final Request request) {
		if ( request.container() ) {

			return reply(request, request

					.shape(request.shape()

							.map(new Redactor(Shape.Role, request.roles()))
							.map(new Redactor(Shape.Task, Shape.Create))
							.map(new Redactor(Shape.View, Shape.Detail))
							.map(new Redactor(Shape.Mode, Shape.Convey))

							.map(new Splitter(true, GAE.Contains))
							.map(new Inferencer())
							.map(new Optimizer())

					)

					.body(entity())

					.value(entity -> datastore.exec(datastore -> {

						// !!! validate against shape


						final String kind="kind"; // !!! extract from shape w/in decoder?

						final String name=request.path()+request.header("Slug")
								.map(Codecs::encode) // encode slug as IRI path component
								.orElseGet(() -> UUID.randomUUID().toString()); // !! sequential generator


						final Entity resource=new Entity(entity.getKind(), name);

						resource.setPropertiesFrom(entity);

						final Key key=datastore.put(resource);


						return response -> response
								.status(Response.Created)
								.header("Location", request.base()+key.getName());

					})));

		} else {

			return request.reply(failure("resource POST method"));

		}
	}


	private Future<Response> reply(final Request request,
			final Result<? extends Function<Response, Response>, ? extends Function<Response, Response>> result) {
		return result.fold(request::reply, request::reply);
	}


	private Failure failure(final String cause) {
		return new Failure()
				.status(Response.InternalServerError)
				.notes("see server logs for details")
				.cause(new UnsupportedOperationException(cause));
	}

}
