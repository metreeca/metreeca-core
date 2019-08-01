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

package com.metreeca.back.gae;

import com.metreeca.tray.Tray;

import com.google.appengine.api.datastore.Key;
import com.google.appengine.api.datastore.KeyFactory;
import com.google.appengine.api.datastore.Query;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.*;

import java.util.Collection;

import static com.metreeca.back.gae.Datastore.datastore;
import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.rest.Engine.engine;
import static com.metreeca.tray.Tray.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class DatastoreEngineTest {

	private static final LocalServiceTestHelper helper=new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
	);


	private static final String StoreKind=DatastoreEngine.class.getName()+":context";
	private static final String EntryKind=DatastoreEngine.class.getName()+":resource";


	private void exec(final Runnable... tasks) {
		new Tray()

				.set(engine(), DatastoreEngine::new)

				.exec(() -> tool(datastore()).exec(service -> {

					final Key root=KeyFactory.createKey(StoreKind, "default"); // !!! name
					final Query query=new Query(EntryKind, root);


					final boolean empty=!service
							.prepare(service.getCurrentTransaction(null), query)
							.asIterator()
							.hasNext();

					System.out.println(empty);

					if ( empty ) {

						final Model small=small();



						small.filter(item("/offices-basic/"), LDP.CONTAINS, null).objects()
								.forEach(office -> {

									final Collection<Statement> description=small
											.filter((Resource)office, null, null)
											;

									System.out.println(encode(description));
								});

					}

					return this;

				}))

				.exec(tasks)
				.clear();
	}


	@BeforeEach void setUp() {
		helper.setUp();
	}

	@AfterEach void tearDown() {
		helper.tearDown();
	}


	@Nested final class Browse {

		@Test void testSilentlyIgnoreContainers() {
			exec(() -> assertThat(tool(engine()).relate(item("/resource"), edges(and())))
					.isInstanceOf(UnsupportedOperationException.class)
			);
		}

	}

	@Nested final class Relate {

		@Test void testRejectResourceFilters() {
			exec(() -> assertThatThrownBy(() -> tool(engine()).relate(item("/resource"), edges(and())))
					.isInstanceOf(UnsupportedOperationException.class)
			);
		}

	}

	@Disabled @Nested final class Create {


		@Test void testCreateNewResource() {
			exec(() -> tool(engine()).create(RDF.NIL, and(), decode("rdf:nil ")));
		}


		@Test void testRejectExistingResource() {}

		@Test void testRejectMalformedModel() {}

		@Test void testRejectExceedingModel() {}

	}

	@Nested final class Update {}

	@Nested final class Delete {}

}
