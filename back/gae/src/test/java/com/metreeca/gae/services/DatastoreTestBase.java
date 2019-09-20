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

package com.metreeca.gae.services;

import com.metreeca.rest.Codecs;
import com.metreeca.rest.Context;

import com.google.cloud.datastore.Batch;
import com.google.cloud.datastore.Entity;
import com.google.cloud.datastore.EntityValue;
import com.google.cloud.datastore.testing.LocalDatastoreHelper;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonValue;

import static com.metreeca.gae.services.Datastore.datastore;
import static com.metreeca.rest.Context.service;

import static java.lang.String.format;
import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.toList;


public abstract class DatastoreTestBase {

	private static final LocalDatastoreHelper helper=LocalDatastoreHelper.create(1.0);


	@BeforeAll static void start() throws IOException, InterruptedException {
		helper.start();
	}

	@AfterAll static void stop() throws IOException, InterruptedException, TimeoutException {
		helper.stop();
	}

	@BeforeEach void reset() throws IOException {
		helper.reset();
	}


	protected void exec(final Runnable... tasks) {
		new Context()
				.set(datastore(), () -> new Datastore(helper.getOptions()))
				.exec(tasks)
				.clear();
	}


	protected Runnable load(final Supplier<? extends Iterable<Entity>> entities) {
		return () -> service(datastore()).exec(datastore -> {

			final Batch batch=datastore.newBatch();

			service(entities).forEach(batch::put); // cache for future use

			return batch.submit();

		});
	}


	//// BIRT Test Dataset /////////////////////////////////////////////////////////////////////////////////////////////

	protected Supplier<Collection<Entity>> birt() {
		return () -> {

			final JsonObject _entities=Json.createReader(Codecs.input(DatastoreTestBase.class, ".json")).readObject();

			final Collection<Entity> entities=new ArrayList<>();

			entities.addAll(offices(_entities.getJsonArray("offices")));
			entities.addAll(employees(_entities.getJsonArray("employees")));

			return unmodifiableCollection(entities);

		};
	}


	private static List<Entity> offices(final Collection<JsonValue> _offices) {

		final Datastore datastore=service(datastore());

		return _offices.stream().map(JsonValue::asJsonObject).map(_office -> {

			final JsonObject _country=_office.getJsonObject("country");
			final JsonObject _city=_office.getJsonObject("city");

			return Entity

					.newBuilder(datastore.key("Office", "/offices/"+_office.getString("code")))

					.set("code", _office.getString("code"))
					.set("label", _office.getString("label"))

					.set("country", Entity
							.newBuilder(datastore.key("Location", format("http://sws.geonames.org/%s/", _country.getString("code"))))
							.set("label", _country.getString("label"))
							.build()
					)

					.set("city", Entity
							.newBuilder(datastore.key("Location", format("http://sws.geonames.org/%s/", _city.getString("code"))))
							.set("label", _city.getString("label"))
							.build()
					)

					.build();

		}).collect(toList());
	}

	private static List<Entity> employees(final Collection<JsonValue> _employees) {

		final Datastore datastore=service(datastore());

		return _employees.stream().map(JsonValue::asJsonObject).map(_employee -> {

			final JsonObject _office=_employee.getJsonObject("office");
			final JsonObject _supervisor=_employee.getJsonObject("supervisor");

			final Entity.Builder employee=Entity

					.newBuilder(datastore.key("Employee", "/employees/"+_employee.getString("code")))

					.set("code", _employee.getString("code"))
					.set("label", _employee.getString("label"))
					.set("forename", _employee.getString("forename"))
					.set("surname", _employee.getString("surname"))

					.set("email", _employee.getString("email"))
					.set("title", _employee.getString("title"))
					.set("seniority", _employee.getJsonNumber("seniority").longValue()) // integrals stored as longs


					.set("office", Entity
							.newBuilder(datastore.key("Office", format("/offices/%s", _office.getString("code"))))
							.set("label", _office.getString("label"))
							.build()
					);

			Optional.ofNullable(_supervisor).ifPresent(s -> employee.set("supervisor", Entity
					.newBuilder(datastore.key("Employee", format("/employees/%s", s.getString("code"))))
					.set("label", s.getString("label"))
					.build()
			));

			final List<EntityValue> subordinates=_employees.stream().map(JsonValue::asJsonObject)

					.filter(_subordinate -> _subordinate.containsKey("supervisor")
							&& _subordinate.getJsonObject("supervisor").getString("code").equals(_employee.getString("code"))
					)

					.map(_subordinate -> Entity
							.newBuilder(datastore.key("Employee", "/employees/"+_subordinate.getString("code")))
							.set("label", _subordinate.getString("label"))
							.build()
					)

					.map(EntityValue::of)

					.collect(toList());

			if ( !subordinates.isEmpty() ) {
				employee.set("subordinates", subordinates);
			}

			return employee.build();

		}).collect(toList());
	}

}
