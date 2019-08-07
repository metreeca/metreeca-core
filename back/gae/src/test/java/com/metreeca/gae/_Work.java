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

import com.google.appengine.api.datastore.*;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;


final class _Work {

	private static final LocalServiceTestHelper helper=new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
	);


	@BeforeEach void setUp() {
		helper.setUp();
	}

	@AfterEach void tearDown() {
		helper.tearDown();
	}


	@Test void test() {

		final Entity employee=new Entity("Employee", "/employees/1");

		employee.setProperty("label", "Employee 1");

		final EmbeddedEntity office=new EmbeddedEntity();

		office.setKey(KeyFactory.createKey("Office", "/offices/1"));

		office.setProperty("label", "Office 1");

		employee.setProperty("office", office);


		final DatastoreService datastore=DatastoreServiceFactory.getDatastoreService();

		datastore.put(employee);

		final Query query=new Query("Employee");

		final Query.FilterPredicate filter=new Query.FilterPredicate(
				"office.label", Query.FilterOperator.EQUAL, "Office 1"
		);

		query.setFilter(filter);


		final List<Entity> results=datastore.prepare(query)
				.asList(FetchOptions.Builder.withDefaults());

		System.out.println(results);
	}

}
