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
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.google.appengine.api.datastore.KeyFactory.createKey;


final class _Work extends GAETestBase {

	@Test void test() throws EntityNotFoundException {

		final Key root=createKey("*", "/employees/");
		final Entity employee=new Entity("Employee", "/employees/1", root);

		employee.setProperty("label", "Employee 1");

		final EmbeddedEntity office=new EmbeddedEntity();

		office.setKey(createKey("Office", "/offices/1"));

		office.setProperty("label", "Office 1");

		employee.setIndexedProperty("office", office);


		final DatastoreService datastore=DatastoreServiceFactory.getDatastoreService();

		datastore.put(employee);

		final Query query=new Query("Employee")
				.setFilter(new Query.FilterPredicate(
						"office.__key__", Query.FilterOperator.EQUAL, createKey("Office", "/offices/1")
				));


		final List<Entity> results=datastore.prepare(query)
				.asList(FetchOptions.Builder.withDefaults());

		System.out.println(results);

		results.forEach(entity -> System.out.println(((EmbeddedEntity)entity.getProperty("office")).getKey().getName()));

	}

}
