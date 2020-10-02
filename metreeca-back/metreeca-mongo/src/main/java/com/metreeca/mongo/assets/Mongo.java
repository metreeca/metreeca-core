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

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;

import java.util.function.Function;
import java.util.function.Supplier;

public final class Mongo implements AutoCloseable {

	public static Supplier<Mongo> mongo() {
		return () -> new Mongo(MongoClients.create());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private MongoClient client;


	public Mongo(final MongoClient client) {

		if ( client == null ) {
			throw new NullPointerException("null client");
		}

		this.client=client;
	}


	@Override public void close() {
		try {

			if ( client != null ) { client.close(); }

		} finally {

			client=null;

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public <V> V exec(final Function<MongoClient, V> task) {

		if ( task == null ) {
			throw new NullPointerException("null task");
		}

		if ( client == null ) {
			throw new IllegalStateException("closed mongo store");
		}

		return task.apply(client);

	}

}
