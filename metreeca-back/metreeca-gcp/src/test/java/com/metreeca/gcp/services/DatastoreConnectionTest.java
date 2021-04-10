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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Namespace;
import org.eclipse.rdf4j.repository.Repository;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.metreeca.gcp.services.DatastoreTest.datastore;
import static com.metreeca.json.Values.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.common.iteration.Iterations.stream;

final class DatastoreConnectionTest {

	private static final IRI w=item("w");
	private static final IRI x=item("x");
	private static final IRI y=item("y");
	private static final IRI z=item("z");

	private static final IRI p=term("p");
	private static final IRI q=term("q");

	private static final IRI a=item("a");
	private static final IRI b=item("b");


	private void exec(final Consumer<RepositoryConnection> task) {
		datastore(options -> {

			final Repository repository=new DatastoreRepository("test", options);

			try {

				repository.init();

				task.accept(repository.getConnection());

			} finally {
				repository.shutDown();
			}

		});
	}


	@Nested final class Transactions {

		@Test void testCommit() {
			exec(connection -> {

				connection.begin();
				connection.add(w, p, x, a);
				connection.add(x, q, z, b);
				connection.commit();

				assertThat(connection.getStatements(

						null, null, null, true

				)).containsExactlyInAnyOrder(
						statement(w, p, x, a),
						statement(x, q, z, b)
				);

			});
		}

		@Test void testRollback() {
			exec(connection -> {

				connection.begin();
				connection.add(w, p, x);
				connection.add(x, q, z);
				connection.rollback();

				assertThat(connection.getStatements(

						null, null, null, true

				)).isEmpty();
			});
		}

	}

	@Nested final class Namespaces {

		private final Namespace n=namespace(a.getLocalName(), a.getNamespace());
		private final Namespace o=namespace(b.getLocalName(), b.getNamespace());

		@Test void testGetNamespaces() {
			exec(connection -> {

				connection.setNamespace(n.getPrefix(), n.getName());
				connection.setNamespace(o.getPrefix(), o.getName());

				assertThat(connection.getNamespaces())
						.containsExactlyInAnyOrder(n, o);

			});
		}

		@Test void testSetAndGetNamespace() {
			exec(connection -> {

				connection.setNamespace(n.getPrefix(), n.getName());

				assertThat(connection.getNamespace(n.getPrefix()))
						.isEqualTo(n.getName());

			});
		}

		@Test void testRemoveNamespace() {
			exec(connection -> {

				connection.setNamespace(n.getPrefix(), n.getName());
				connection.setNamespace(o.getPrefix(), o.getName());

				connection.removeNamespace(o.getPrefix());

				assertThat(connection.getNamespaces())
						.containsExactly(n);

			});
		}

		@Test void testClearNamespaces() {
			exec(connection -> {

				connection.setNamespace(n.getPrefix(), n.getName());
				connection.setNamespace(o.getPrefix(), o.getName());

				connection.clearNamespaces();

				assertThat(connection.getNamespaces())
						.isEmpty();

			});
		}

	}

	@Nested final class Statements {

		@Test void testGetContextIDs() {
			exec(connection -> {

				connection.add(x, p, y, a, b);
				connection.add(x, p, z, a, b);

				assertThat(stream(connection.getContextIDs()))
						.containsExactlyInAnyOrder(a, b);

			});
		}

		@Test void testSize() {
			exec(connection -> {

				connection.add(x, p, y);
				connection.add(x, p, z);

				assertThat(connection.size()).isEqualTo(2);

			});
		}

		@Test void testGetStatements() {
			exec(connection -> {

				connection.add(w, p, x, a);
				connection.add(y, q, z, b);

				assertThat(connection.getStatements(

						null, null, null, true

				)).containsExactlyInAnyOrder(
						statement(w, p, x, a),
						statement(y, q, z, b)
				);

				assertThat(connection.getStatements(

						w, null, null, true

				)).containsExactlyInAnyOrder(
						statement(w, p, x, a)
				);

				assertThat(connection.getStatements(

						null, p, null, true

				)).containsExactlyInAnyOrder(
						statement(w, p, x, a)
				);

				assertThat(connection.getStatements(

						null, null, x, true

				)).containsExactlyInAnyOrder(
						statement(w, p, x, a)
				);

				assertThat(connection.getStatements(

						null, null, null, true, a

				)).containsExactlyInAnyOrder(
						statement(w, p, x, a)
				);

			});
		}

		@Test void testRemoveStatements() {
			exec(connection -> {

				connection.add(w, p, x, a);
				connection.add(y, q, z, b);

				connection.remove(y, q, z, b);

				assertThat(connection.getStatements(

						null, null, null, true

				)).containsExactlyInAnyOrder(
						statement(w, p, x, a)
				);

			});
		}

	}

	@Nested final class Operations {

	}

}