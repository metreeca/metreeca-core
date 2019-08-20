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

import com.metreeca.rest.Codecs;

import com.google.appengine.api.datastore.*;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import javax.json.*;

import static com.google.appengine.api.datastore.KeyFactory.createKey;


final class _Work extends GAETestBase {

	@Test void test() {

		final Entity employee=new Entity("Employee", "/employees/1");

		employee.setProperty("label", "Employee 1");

		final EmbeddedEntity office=new EmbeddedEntity();

		office.setKey(createKey("Office", "/offices/1"));

		office.setProperty("label", "Office 1");

		employee.setIndexedProperty("office", office);


		final DatastoreService datastore=DatastoreServiceFactory.getDatastoreService();

		datastore.put(employee);

		final Query query=new Query("Employee")
				.setFilter(new Query.FilterPredicate(
						"office.label", Query.FilterOperator.EQUAL, "Office 1"
				));


		final List<Entity> results=datastore.prepare(query)
				.asList(FetchOptions.Builder.withDefaults());

		System.out.println(results);

		results.forEach(entity -> System.out.println(((EmbeddedEntity)entity.getProperty("office")).getKey().getName()));
	}

	@Test void convert() {

		final JsonArrayBuilder builder=Json.createArrayBuilder();

		Arrays.stream(Codecs.text(this, "_employees.csv").split("\\R"))
				.skip(1)
				.map(record -> record.split(","))
				.map(this::employee)
				.forEachOrdered(builder::add);

		final JsonArray offices=builder.build();

		Json.createWriter(System.out).write(offices);
	}

	private JsonObject office(final String... fields) {
		return Json.createObjectBuilder()

				.add("code", fields[0])
				.add("label", fields[1])

				.add("country", Json.createObjectBuilder()
						.add("code", fields[2])
						.add("label", fields[3])
						.build()
				)

				.add("city", Json.createObjectBuilder()
						.add("code", fields[4])
						.add("label", fields[5])
						.build()
				)

				.build();
	}

	private JsonObject employee(final String... fields) {
		return Json.createObjectBuilder()

				.add("code", fields[0])
				.add("label", fields[1])
				.add("forename", fields[2])
				.add("surname", fields[3])
				.add("email", fields[4])
				.add("title", fields[5])
				.add("seniority", Integer.parseInt(fields[6]))

				.add("office", Json.createObjectBuilder()
						.add("code", fields[7])
						.add("label", fields[8])
						.build()
				)

				.add("supervisor", Json.createObjectBuilder()
						.add("code", fields[9])
						.add("label", fields[10])
						.build()
				)

				.build();
	}


	@Test void load() {
		System.out.println(birt());
	}

}
