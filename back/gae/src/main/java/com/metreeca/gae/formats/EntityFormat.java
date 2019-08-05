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

import com.metreeca.rest.*;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Inferencer;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.probes.Redactor;

import com.google.appengine.api.datastore.Entity;

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

	private EntityFormat() {} // singleton


	@Override public Result<Entity, Failure> get(final Message<?> message) {
		return message.body(json()).value(json ->
				new EntityDecoder().decode(json, shape(message), message.request().path())
		);
	}


	@Override public <M extends Message<M>> M set(final M message, final Entity value) {
		return message.body(json(),
				new EntityEncoder().encode(value, shape(message)) // !!! will see stale shape if changed after body is set…
		);
	}


	private Shape shape(final Message<?> message) {
		return message.shape()

				.map(new Redactor(Shape.Role))
				.map(new Redactor(Shape.Task))
				.map(new Redactor(Shape.Detail))
				.map(new Redactor(Shape.Mode, Shape.Convey))

				.map(new Inferencer())
				.map(new Optimizer());
	}

}
