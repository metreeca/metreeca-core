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
import com.metreeca.tree.queries.*;

import com.google.appengine.api.datastore.Entity;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.api.datastore.Query.FilterOperator;

import java.util.Optional;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Meta.hint;

import static com.google.appengine.api.datastore.KeyFactory.createKey;


final class DatastoreRelator extends DatastoreProcessor {

	private final Datastore datastore=service(datastore());


	Future<Response> handle(final Request request) {
		return request.container() ? container(request) : resource(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> container(final Request request) {

		final com.metreeca.tree.Query query=null; // !!! parse query

		return query.map(new Probe<Future<Response>>() {

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
	}

	private Future<Response> resource(final Request request) {
		return request.reply(response -> datastore.exec(service -> {

			final Shape shape=convey(request.shape());

			final String name=request.path();
			final String kind=clazz(shape).orElse(GAE.Roots);
			final String root=hint(shape).orElseGet(() -> name.substring(0, name.lastIndexOf('/')+1)); // path stem

			final Query query=new Query(kind)
					.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
							FilterOperator.EQUAL, createKey(createKey(GAE.Roots, root), kind, name)
					));

			// !!! set filters from filter shape?

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
