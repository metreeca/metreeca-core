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

package com.metreeca.gcloud.services;

import com.metreeca.rest.Future;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import com.google.cloud.datastore.Key;
import com.google.cloud.datastore.KeyQuery;
import com.google.cloud.datastore.Query;

import static com.metreeca.gcloud.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Failure.internal;

import static com.google.cloud.datastore.StructuredQuery.PropertyFilter.eq;


final class DatastoreDeleter extends DatastoreProcessor {

	private final Datastore datastore=service(datastore());


	Future<Response> handle(final Request request) {
		return request.container() ? container(request) : resource(request);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Future<Response> container(final Request request) {
		return request.reply(internal(new UnsupportedOperationException("container DELETE method")));
	}

	private Future<Response> resource(final Request request) {
		return request.reply(response -> datastore.exec(service -> {

			final Key key=datastore.key(convey(request.shape()), request.path());

			final KeyQuery query=Query.newKeyQueryBuilder()
					.setKind(key.getKind())
					.setFilter(eq("__key__", key))
					.build();

			if ( service.run(query).hasNext() ) {

				service.delete(key);

				return response.status(Response.NoContent);

			} else {

				return response.status(Response.NotFound);

			}

		}));
	}

}
