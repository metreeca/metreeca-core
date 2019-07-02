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

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.*;

import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.Engine.engine;
import static com.metreeca.tray.Tray.tool;


final class DatastoreEngineTest {

	private static final LocalServiceTestHelper helper=new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
	);


	private void exec(final Runnable ...tasks) {
		new Tray()
				.set(engine(), DatastoreEngine::new)
				.exec(tasks)
				.clear();
	}


	@BeforeEach void setUp() {
		helper.setUp();
	}

	@AfterEach void tearDown() {
		helper.tearDown();
	}


	@Nested final class Relate {

		@Test void test() {
			exec(() -> assertThat(tool(engine()).relate(RDF.NIL, edges(and()))));
		}

	}

	@Nested final class Create {


		@Test void testCreateNewResource() {
			exec(() -> tool(engine()).create(RDF.NIL, and(), decode("rdf:nil ")));
		}


		@Test void testRejectExistingResource() {}

		@Test void testRejectMalformedModel() {}

		@Test void testRejectExceedingModel() {}

	}

}
