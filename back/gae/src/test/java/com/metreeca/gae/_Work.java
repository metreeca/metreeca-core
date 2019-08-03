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

import static java.util.Arrays.asList;


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

		//final Key root=KeyFactory.createKey("root", 1); // !!! name

		final Entity entity=new Entity("Person");

		entity.setProperty("name", "Alice");

		final EmbeddedEntity address1=new EmbeddedEntity();
		address1.setProperty("street", "100 Main Street");
		address1.setProperty("locality", "Springfield");
		address1.setProperty("region", "VA");

		final EmbeddedEntity address2=new EmbeddedEntity();
		address2.setProperty("street", "Via di Lì");
		address2.setProperty("locality", "Qui e Là");
		address2.setProperty("region", "ZZ");

		entity.setIndexedProperty("address", asList(address1, address2));

		final DatastoreService datastore=DatastoreServiceFactory.getDatastoreService();
		datastore.put(entity);

		final Query query=new Query("Person");

		//final Query.FilterPredicate filter=new Query.FilterPredicate(
		//		"name", Query.FilterOperator.EQUAL, "Alice"
		//);

		final Query.FilterPredicate filter=new Query.FilterPredicate(
				"address.region", Query.FilterOperator.EQUAL, "ZZ"
		);

		query.setFilter(filter);


		final List<Entity> results=datastore.prepare(query)
				.asList(FetchOptions.Builder.withDefaults());

		System.out.println(results);
	}

}
