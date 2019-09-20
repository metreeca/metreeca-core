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

package com.metreeca.gae.formats;

import com.metreeca.gae.services.Datastore;
import com.metreeca.rest.*;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Redactor;

import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.FullEntity;

import javax.json.JsonValue;

import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.formats.JSONFormat.json;


public final class EntityFormat implements Format<Entity> {

	private static final EntityFormat Instance=new EntityFormat();


	/**
	 * Retrieves the entity body format.
	 *
	 * @return the singleton entity body format instance
	 */
	public static EntityFormat entity() {
		return Instance;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Datastore datastore=service(datastore());

	private final EntityDecoder decoder=new EntityDecoder();
	private final EntityEncoder encoder=new EntityEncoder();


	private EntityFormat() {} // singleton


	public Object value(final JsonValue value, final Shape shape) {
		return decoder.decode(value, shape);
	}


	@Override public Result<Entity, Failure> get(final Message<?> message) {
		return message.body(json()).value(json -> {

			final FullEntity<?> entity=decoder.decode(json, shape(message));

			return entity instanceof Entity ? (Entity)entity : Entity.newBuilder(

					datastore.key(message.request().path(), entity.getKey().getKind()), entity

			).build();

		});
	}

	@Override public <M extends Message<M>> M set(final M message, final Entity value) {
		return message.body(json(),
				encoder.encode(value, shape(message))
		);
	}


	private Shape shape(final Message<?> message) {
		return message.shape()

				.map(new Redactor(Shape.Role))
				.map(new Redactor(Shape.Task))
				.map(new Redactor(Shape.Detail))
				.map(new Redactor(Shape.Mode, Shape.Convey))

				.map(new Optimizer());
	}

}
