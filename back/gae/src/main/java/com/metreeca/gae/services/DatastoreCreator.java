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
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Inferencer;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Redactor;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;

import java.util.UUID;

import static com.metreeca.gae.formats.EntityFormat.entity;
import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;


final class DatastoreCreator {

	private final Datastore datastore=service(datastore());


	Future<Response> handle(final Request request) {
		return request.container() ? container(request)
				: request.reply(Failure.failure(new UnsupportedOperationException("resource POST method")));
	}


	private Future<Response> container(final Request request) {
		return request

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

				.process(entity -> { // !!! validate against shape

					return Value(entity);

				})

				.process(entity -> datastore.exec(service -> {

					final String id=request.path()+request.header("Slug")
							.map(Codecs::encode) // encode slug as IRI path component
							.orElseGet(() -> UUID.randomUUID().toString()); // !! sequential generator

					final Key key=KeyFactory.createKey(entity.getKind(), id);

					final boolean clashing=service.prepare(new Query(key.getKind())
							.setFilter(new Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY, FilterOperator.EQUAL, key))
							.setKeysOnly()
					).asIterator().hasNext();

					if ( clashing ) {

						return Error(Failure.failure(new IllegalStateException("clashing entity slug {"+key+"}")));

					} else {

						final Entity resource=new Entity(key);

						resource.setPropertiesFrom(entity);

						return Value(resource);

					}

				}))

				.value(entity -> datastore.exec(service -> {

					final Key created=service.put(entity);

					return request.reply(response -> response
							.status(Response.Created)
							.header("Location", created.getName()));

				}))

				.fold(future -> future, request::reply);
	}

}
