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
import com.metreeca.rest.Failure;
import com.metreeca.rest.Message;
import com.metreeca.rest.Result;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Traverser;
import com.metreeca.tree.shapes.*;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.PropertyContainer;

import static com.metreeca.gae.formats.EntityFormat.entity;


final class DatastoreTrimmer extends DatastoreProcessor {

	<M extends Message<M>> Result<M, Failure> trim(final M message) {
		return message.body(entity()).value(entity -> message.body(entity(),
				trim(convey(message.shape()), entity)
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Object trim(final Shape shape, final Object object) {
		return GAE.isEntity(object) ? trim(shape, (PropertyContainer)object)
				: object;
	}

	private EmbeddedEntity trim(final Shape shape, final PropertyContainer entity) {

		final EmbeddedEntity target=new EmbeddedEntity();

		shape.map(new TrimmerProbe(entity, target));

		return target;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final class TrimmerProbe extends Traverser<Object> {

		private final PropertyContainer source;
		private final PropertyContainer target;


		private TrimmerProbe(final PropertyContainer source, final PropertyContainer target) {
			this.source=source;
			this.target=target;
		}


		@Override public Object probe(final Field field) {

			final String name=field.getName();
			final Object value=trim(field.getShape(), source.getProperty(name));

			if ( value != null ) {

				target.setProperty(name, value);

			}

			return null;
		}

		@Override public Object probe(final And and) {

			and.getShapes().forEach(shape -> shape.map(this));

			return null;
		}

		@Override public Object probe(final Or or) {

			or.getShapes().forEach(shape -> shape.map(this));

			return null;
		}

		@Override public Object probe(final When when) {

			when.getPass().map(this);
			when.getFail().map(this);

			return null;
		}

	}

}
