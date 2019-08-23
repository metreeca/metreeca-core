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
import com.metreeca.rest.Future;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tree.Query.Probe;
import com.metreeca.tree.Shape;
import com.metreeca.tree.queries.Items;
import com.metreeca.tree.queries.Stats;
import com.metreeca.tree.queries.Terms;

import com.google.appengine.api.datastore.*;

import java.util.Optional;

import static com.metreeca.gae.GAE.key;
import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.tree.shapes.Clazz.clazz;

import static com.google.appengine.api.datastore.Entity.KEY_RESERVED_PROPERTY;
import static com.google.appengine.api.datastore.Query.FilterOperator.EQUAL;

import static java.util.stream.Collectors.toList;
import static java.util.stream.StreamSupport.stream;


final class DatastoreRelator extends DatastoreProcessor {

	private final Datastore datastore=service(datastore());
	private final DatastoreSplitter splitter=new DatastoreSplitter();


	Future<Response> handle(final Request request) {
		return request.container() ? container(request) : resource(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> container(final Request request) {
		return request.query(request.shape(), entity()::value).fold( // !!! split/redact shape

				query -> query.map(new Probe<Future<Response>>() {

					@Override public Future<Response> probe(final Items items) {

						final Shape convey=convey(items.getShape()); // !!!
						final Shape filter=filter(splitter.resource(items.getShape()));

						final String kind=clazz(filter).orElse("*");

						final Query query=new Query(kind); // !!! set filters from filter shape

						return datastore.exec(service -> {

							final Entity container=new Entity("*", request.path());

							container.setProperty(GAE.contains,
									stream(service.prepare(query).asIterable().spliterator(), false)
											.map(entity -> {

												final EmbeddedEntity resource=new EmbeddedEntity();

												resource.setKey(entity.getKey());
												resource.setPropertiesFrom(entity);

												return resource;

											})
											.collect(toList())
							);

							return request.reply(response -> response // !!! headers?
									.status(OK) // !!! review
									.shape(convey)
									.body(entity(), container)
							);

						});

					}

					@Override public Future<Response> probe(final Terms terms) {
						throw new UnsupportedOperationException("to be implemented"); // !!! tbi
					}

					@Override public Future<Response> probe(final Stats stats) {
						throw new UnsupportedOperationException("to be implemented"); // !!! tbi
					}

				}),

				request::reply
		);
	}

	private Future<Response> resource(final Request request) {
		return request.reply(response -> datastore.exec(service -> {

			final Shape shape=convey(request.shape());
			final Key key=key(request.path(), shape);

			final Query query=new Query(key.getKind()) // !!! set filters from filter shape?
					.setFilter(new Query.FilterPredicate(KEY_RESERVED_PROPERTY, EQUAL, key));

			// ;( projecting only properties actually included in the shape would lower costs, as projection queries
			// are counted as small operations: unfortunately, a number of limitations apply:
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
