/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rdf4j.assets;


import com.metreeca.json.Shape;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Shape.convey;
import static com.metreeca.json.Shape.required;
import static com.metreeca.json.ValuesTest.*;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf4j.assets.GraphTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static com.metreeca.rest.Response.NotFound;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest._work.JSONLDFormat.jsonld;


final class GraphUpdaterTest {

	@Nested final class Holder {

		@Test void testNotImplemented() {
			exec(() -> new GraphUpdater()

					.handle(new Request()
							.path("/employees/")
							.body(jsonld(), decode("</employees/> rdfs:label 'Updated!'."))
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.InternalServerError)
					)
			);
		}

	}

	@Nested final class Member {

		@Test void testUpdate() {
			exec(model(small()), () -> new GraphUpdater()

					.handle(new Request()
							.base(Base)
							.path("/employees/1370").attribute(Shape.shape(), convey().then(
									field(term("forename"), required()),
									field(term("surname"), required()),
									field(term("email"), required()),
									field(term("title"), required()),
									field(term("seniority"), required())
							))
							.body(jsonld(), decode("</employees/1370>"
									+":forename 'Tino';"
									+":surname 'Faussone';"
									+":email 'tfaussone@example.com';"
									+":title 'Sales Rep' ;"
									+":seniority 5 ." // outside salesman envelope
							))
					)

					.accept(response -> {

						assertThat(response)
								.hasStatus(Response.NoContent)
								.doesNotHaveBody();

						assertThat(model())

								.as("updated values inserted")
								.hasSubset(decode("</employees/1370>"
										+":forename 'Tino';"
										+":surname 'Faussone';"
										+":email 'tfaussone@example.com';"
										+":title 'Sales Rep' ;"
										+":seniority 5 ."
								))

								.as("previous values removed")
								.doesNotHaveSubset(decode("</employees/1370>"
										+":forename 'Gerard';"
										+":surname 'Hernandez'."
								));

					}));
		}

		@Test void testRejectMissing() {
			exec(() -> new GraphUpdater()

					.handle(new Request()
							.base(Base)
							.path("/employees/9999")
							.body(jsonld(), decode(""))
					)

					.accept(response -> assertThat(response)
							.hasStatus(NotFound)
							.doesNotHaveBody()
					)
			);

		}

	}

}
