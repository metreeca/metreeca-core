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

package com.metreeca.mongo.assets;

import com.metreeca.rest.*;
import com.metreeca.rest.assets.Engine;

import static com.metreeca.rest.Context.asset;

public final class MongoEngine implements Engine {

	private final Mongo mongo=asset(Mongo.mongo());


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		if ( handler == null ) {
			throw new NullPointerException("null task");
		}

		return request -> consumer -> mongo.exec(client -> {

			handler.handle(request).accept(consumer);

			return this;

		});
	}


	@Override public Future<Response> create(final Request request) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Future<Response> relate(final Request request) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Future<Response> update(final Request request) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

	@Override public Future<Response> delete(final Request request) {
		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
	}

}
