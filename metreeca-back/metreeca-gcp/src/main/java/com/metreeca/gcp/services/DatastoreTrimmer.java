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

package com.metreeca.gcp.services;


import com.metreeca.rest.Failure;
import com.metreeca.rest.Message;
import com.metreeca.rest.Result;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Traverser;
import com.metreeca.tree.shapes.*;

import com.google.cloud.datastore.*;

import static com.metreeca.gcp.formats.EntityFormat.entity;
import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;


final class DatastoreTrimmer extends DatastoreProcessor {

	private final Datastore datastore=service(datastore());


	<M extends Message<M>> Result<M, Failure> trim(final M message) {
		return message.body(entity(datastore)).value(entity -> message.body(entity(datastore),
				(Entity)trim(convey(message.shape()), EntityValue.of(entity)).get()
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Value<?> trim(final Shape shape, final Value<?> value) {
		return value.getType() == ValueType.ENTITY ? trim(shape, (EntityValue)value)
				: value;
	}

	private EntityValue trim(final Shape shape, final EntityValue value) {

		final FullEntity<?> entity=value.get();

		final BaseEntity.Builder<?, ?> target=entity instanceof Entity ?
				Entity.newBuilder(((Entity)entity).getKey()) : FullEntity.newBuilder(entity.getKey());

		shape.map(new TrimmerProbe(entity, target));

		return EntityValue.of((FullEntity<?>)target.build());

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class TrimmerProbe extends Traverser<Shape> {

		private final FullEntity<?> source;
		private final BaseEntity.Builder<?, ?> target;


		private TrimmerProbe(final FullEntity<?> source, final BaseEntity.Builder<?, ?> target) {
			this.source=source;
			this.target=target;
		}


		@Override public Shape probe(final Field field) {

			final String name=field.getName().toString();

			if ( source.contains(name) ) {

				final Value<?> value=source.getValue(name);
				target.set(name, trim(field.getShape(), value));

			}

			return null;
		}

		@Override public Shape probe(final And and) {

			and.getShapes().forEach(shape -> shape.map(this));

			return null;
		}

		@Override public Shape probe(final Or or) {

			or.getShapes().forEach(shape -> shape.map(this));

			return null;
		}

		@Override public Shape probe(final When when) {

			when.getPass().map(this);
			when.getFail().map(this);

			return null;
		}

	}

}
