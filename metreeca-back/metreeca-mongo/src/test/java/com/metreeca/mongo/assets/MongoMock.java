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

import com.mongodb.ClientSessionOptions;
import com.mongodb.client.*;
import com.mongodb.connection.ClusterDescription;
import org.bson.Document;
import org.bson.conversions.Bson;

import java.util.List;

final class MongoMock implements MongoClient {

	@Override public void close() {

	}


	@Override public ClusterDescription getClusterDescription() {
		throw new UnsupportedOperationException("to be implemented");
	}


	@Override public MongoDatabase getDatabase(final String databaseName) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public ClientSession startSession() {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public ClientSession startSession(final ClientSessionOptions options) {
		throw new UnsupportedOperationException("to be implemented");
	}


	@Override public MongoIterable<String> listDatabaseNames() {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public MongoIterable<String> listDatabaseNames(final ClientSession clientSession) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public ListDatabasesIterable<Document> listDatabases() {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public ListDatabasesIterable<Document> listDatabases(final ClientSession clientSession) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public <TResult> ListDatabasesIterable<TResult> listDatabases(final Class<TResult> aClass) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public <TResult> ListDatabasesIterable<TResult> listDatabases(
			final ClientSession clientSession, final Class<TResult> aClass) {
		throw new UnsupportedOperationException("to be implemented");
	}


	@Override public ChangeStreamIterable<Document> watch() {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public <TResult> ChangeStreamIterable<TResult> watch(final Class<TResult> aClass) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public ChangeStreamIterable<Document> watch(final List<? extends Bson> pipeline) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public <TResult> ChangeStreamIterable<TResult> watch(final List<? extends Bson> pipeline,
			final Class<TResult> aClass) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public ChangeStreamIterable<Document> watch(final ClientSession clientSession) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public <TResult> ChangeStreamIterable<TResult> watch(
			final ClientSession clientSession, final Class<TResult> aClass) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public ChangeStreamIterable<Document> watch(
			final ClientSession clientSession, final List<? extends Bson> pipeline) {
		throw new UnsupportedOperationException("to be implemented");
	}

	@Override public <TResult> ChangeStreamIterable<TResult> watch(
			final ClientSession clientSession, final List<? extends Bson> pipeline, final Class<TResult> aClass) {
		throw new UnsupportedOperationException("to be implemented");
	}

}
