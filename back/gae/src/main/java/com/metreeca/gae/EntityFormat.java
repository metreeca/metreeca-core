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

package com.metreeca.gae;

import com.metreeca.rest.*;

import com.google.appengine.api.datastore.Entity;

import static com.metreeca.rest.formats.JSONFormat.json;


public final class EntityFormat implements Format<Entity> {

	@Override public Result<Entity, Failure> get(final Message<?> message) {
		return message.body(json()).process(json -> {

			throw new UnsupportedOperationException("to be implemented"); // !!! tbi

		});
	}

}
