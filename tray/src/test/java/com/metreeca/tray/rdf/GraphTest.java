
/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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


import org.eclipse.rdf4j.IsolationLevel;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryException;
import org.eclipse.rdf4j.repository.sail.SailRepository;
import org.eclipse.rdf4j.sail.memory.MemoryStore;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class GraphTest {


	@Test void testPreventUpdateTransactionsOnReadOnlyRepositories() {

		final Graph graph=new Graph() {

			private final Repository repository=new SailRepository(new MemoryStore());

			@Override protected Repository repository() { return repository; }

			@Override protected IsolationLevel isolation() { return READ_ONLY; }

		};

		assertThatThrownBy(() -> graph.update(connection -> {}) )
				.isInstanceOf(RepositoryException.class);

	}

}
