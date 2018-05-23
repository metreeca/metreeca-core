/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tray.rdf;

import org.eclipse.rdf4j.model.ValueFactory;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.eclipse.rdf4j.repository.RepositoryException;

import java.io.File;


final class MappingRepository implements Repository {

	private final _Mapping mapping;
	private final Repository repository;


	MappingRepository(final _Mapping mapping, final Repository repository) {
		this.mapping=mapping;
		this.repository=repository;
	}


	@Override public boolean isWritable() throws RepositoryException {
		return repository.isWritable();
	}

	@Override public boolean isInitialized() {
		return repository.isInitialized();
	}


	@Override public File getDataDir() {
		return repository.getDataDir();
	}

	@Override public void setDataDir(final File dataDir) {
		repository.setDataDir(dataDir);
	}


	@Override public ValueFactory getValueFactory() {
		return repository.getValueFactory();
	}

	@Override public RepositoryConnection getConnection() throws RepositoryException {
		return new MappingConnection(mapping, this, repository.getConnection());
	}


	@Override public void initialize() throws RepositoryException {
		repository.initialize();
	}

	@Override public void shutDown() throws RepositoryException {
		repository.shutDown();
	}

}
