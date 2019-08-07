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
import com.metreeca.rest.services.Engine;

import java.util.function.Supplier;

import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Failure.internal;

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

			case Request.GET: return new DatastoreRelator().handle(request);
			case Request.POST: return new DatastoreCreator().handle(request);
			case Request.PUT: throw new UnsupportedOperationException("to be implemented"); // !!! tbi
			case Request.DELETE: throw new UnsupportedOperationException("to be implemented"); // !!! tbi

			default: return request.reply(
					internal(new UnsupportedOperationException(format("%s method", request.method())))
			);

		}
	}

}
