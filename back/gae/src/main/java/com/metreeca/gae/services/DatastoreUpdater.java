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

import com.metreeca.rest.Future;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import com.google.cloud.datastore.*;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Failure.internal;

import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.eq;


final class DatastoreUpdater extends DatastoreProcessor {

	private final Datastore datastore=service(datastore());


	Future<Response> handle(final Request request) {
		return request.container() ? container(request) : resource(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> container(final Request request) {
		return request.reply(internal(new UnsupportedOperationException("container PUT method")));
	}

	private Future<Response> resource(final Request request) {
		return request

				.body(entity())

				.value(entity -> request.reply(response -> datastore.exec(datastore -> {

					final Key key=this.datastore.key(request.path(), convey(request.shape()));

					final KeyQuery query=Query.newKeyQueryBuilder()
							.setKind(key.getKind())
							.setFilter(eq("__key__", key))
							.build();

					if ( datastore.run(query).hasNext() ) {

						datastore.put(Entity.newBuilder(key, entity).build());

						return response.status(Response.NoContent);

					} else {

						return response.status(Response.NotFound);

					}

				})))

				.fold(future -> future, request::reply);

	}

}
