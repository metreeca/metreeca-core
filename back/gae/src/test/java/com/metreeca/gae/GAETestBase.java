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
import com.metreeca.rest.Context;

import com.google.appengine.api.datastore.EmbeddedEntity;
import com.google.appengine.api.datastore.Entity;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;

import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import static com.google.appengine.api.datastore.KeyFactory.createKey;

import static java.lang.String.format;
import static java.util.stream.Collectors.toList;


public abstract class GAETestBase {

	private static final LocalServiceTestHelper helper=new LocalServiceTestHelper(
			new LocalDatastoreServiceTestConfig()
	);


	@BeforeEach void setUp() {
		helper.setUp();
	}

	@AfterEach void tearDown() {
		helper.tearDown();
	}


	protected void exec(final Runnable... tasks) {
		new Context()
				.exec(tasks)
				.clear();
	}


	//// BIRT Test Dataset /////////////////////////////////////////////////////////////////////////////////////////////

	protected Iterable<Entity> birt() {
		return BIRT.Entities;
	}


	private static final class BIRT {

		private static final Iterable<Entity> Entities=entities(
				Json.createReader(Codecs.input(GAETestBase.class, ".json")).readObject()
		).collect(toList());


		private static Stream<Entity> entities(final JsonObject _entities) {
			return Stream.concat(
					offices(_entities.getJsonArray("offices")),
					employees(_entities.getJsonArray("employees"))
			);
		}

		private static Stream<Entity> offices(final Collection<JsonValue> _offices) {
			return _offices.stream().map(JsonValue::asJsonObject).map(_office -> {

				final Entity office=new Entity("Office",
						"/offices/"+_office.getString("code"),
						createKey(GAE.Roots, "/offices/")
				);

				office.setProperty("code", _office.getString("code"));
				office.setProperty("label", _office.getString("label"));

				final JsonObject _country=_office.getJsonObject("country");
				final EmbeddedEntity country=new EmbeddedEntity();

				country.setKey(createKey("Country", format("http://sws.geonames.org/%s/", _country.getString("code"))));
				country.setProperty("label", _country.getString("label"));

				office.setIndexedProperty("country", country);

				final JsonObject _city=_office.getJsonObject("city");
				final EmbeddedEntity city=new EmbeddedEntity();

				city.setKey(createKey("City", format("http://sws.geonames.org/%s/", _city.getString("code"))));
				city.setProperty("label", _city.getString("label"));

				office.setIndexedProperty("city", city);

				return office;

			});
		}

		private static Stream<Entity> employees(final Collection<JsonValue> _employees) {
			return _employees.stream().map(JsonValue::asJsonObject).map(_employee -> {

				final Entity employee=new Entity("Employee",
						"/employees/"+_employee.getString("code"),
						createKey(GAE.Roots, "/employees/")
				);

				employee.setProperty("code", _employee.getString("code"));
				employee.setProperty("label", _employee.getString("label"));

				employee.setProperty("forename", _employee.getString("forename"));
				employee.setProperty("surname", _employee.getString("surname"));

				employee.setProperty("email", _employee.getString("email"));
				employee.setProperty("title", _employee.getString("title"));
				employee.setProperty("seniority", _employee.getJsonNumber("seniority").intValue());

				final JsonObject _office=_employee.getJsonObject("office");
				final EmbeddedEntity office=new EmbeddedEntity();

				office.setKey(createKey("Office", format("/offices/%s", _office.getString("code"))));
				office.setProperty("label", _office.getString("label"));

				employee.setIndexedProperty("office", office);

				Optional.ofNullable(_employee.getJsonObject("supervisor")).ifPresent(_supervisor -> {

					final EmbeddedEntity supervisor=new EmbeddedEntity();

					supervisor.setKey(createKey("Employee", format("/employees/%s", _supervisor.getString("code"))));
					supervisor.setProperty("label", _supervisor.getString("label"));

					employee.setIndexedProperty("supervisor", supervisor);

				});

				return employee;

			});
		}

	}

}
