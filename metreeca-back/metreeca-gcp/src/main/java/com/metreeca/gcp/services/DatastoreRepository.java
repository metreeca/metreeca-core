/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.gcp.services;

import com.metreeca.json.Values;

import com.google.cloud.datastore.Datastore;
import com.google.cloud.datastore.DatastoreOptions;
import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.base.AbstractRepository;

import java.io.File;

public final class DatastoreRepository extends AbstractRepository {

	private File dataDir;

	private final String name;
	private final Datastore datastore;


	public DatastoreRepository(final String name) {
		this(name, DatastoreOptions.getDefaultInstance());
	}

	public DatastoreRepository(final String name, final DatastoreOptions options) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( name.isEmpty() ) {
			throw new IllegalArgumentException("empty name");
		}

		if ( options == null ) {
			throw new NullPointerException("null options");
		}

		this.name=name;
		this.datastore=options.getService();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override protected void initializeInternal() {
		try ( final DatastoreConnection connection=new DatastoreConnection(this) ) { connection.init(); }
	}

	@Override protected void shutDownInternal() {}


	@Override public boolean isWritable() {
		return true;
	}


	@Override public File getDataDir() {
		return dataDir;
	}

	@Override public void setDataDir(final File dataDir) {
		this.dataDir=dataDir;
	}


	public String getName() {
		return name;
	}

	public Datastore getDatastore() {
		return datastore;
	}


	@Override public ValueFactory getValueFactory() {
		return Values.factory();
	}

	@Override public RepositoryConnection getConnection() {
		return new DatastoreConnection(this);
	}

}
