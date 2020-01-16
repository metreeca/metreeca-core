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

package com.metreeca.gcp.services;

import com.metreeca.gcp.GCP;
import com.metreeca.gcp.formats.EntityFormat;
import com.metreeca.rest.Future;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import com.google.cloud.datastore.*;

import static com.metreeca.gcp.formats.EntityFormat.entity;
import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Failure.internal;
import static com.metreeca.tree.shapes.Clazz.clazz;

import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.eq;


final class DatastoreUpdater extends DatastoreProcessor {

	private final Datastore datastore=service(datastore());

	private final EntityFormat entity=entity();


	Future<Response> handle(final Request request) {
		return request.collection() ? holder(request) : member(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> holder(final Request request) {
		return request.reply(internal(new UnsupportedOperationException("holder PUT method")));
	}

	private Future<Response> member(final Request request) {
		return request

				.body(entity)

				.value(entity -> request.reply(response -> datastore.exec(datastore -> {

					final Key key=datastore.newKeyFactory()
							.setKind(clazz(convey(request.shape())).map(Object::toString).orElse(GCP.Resource))
							.newKey(request.path());

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
