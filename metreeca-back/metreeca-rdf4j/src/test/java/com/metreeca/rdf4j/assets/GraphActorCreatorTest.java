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

package com.metreeca.rdf4j.assets;


import com.metreeca.json.Shape;
import com.metreeca.rest.Request;
import com.metreeca.rest.formats.JSONLDFormat;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.ValueAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.decode;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.Guard.required;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.rdf4j.assets.GraphTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;

final class GraphActorCreatorTest {

	private static final Shape Employee=and(
			filter(field(RDF.TYPE, all(term("Employee")))),
			field(RDFS.LABEL, required(), datatype(XSD.STRING)),
			field(term("code"), required(), datatype(XSD.STRING), pattern("\\d+")),
			field(term("forename"), required(), datatype(XSD.STRING), maxLength(80)),
			field(term("surname"), required(), datatype(XSD.STRING), maxLength(80)),
			field(term("email"), required(), datatype(XSD.STRING), maxLength(80)),
			field(term("title"), required(), datatype(XSD.STRING), maxLength(80))

	);

	private Request request() {
		return new Request()
				.base(Base)
				.path("/employees/")
				.header("Slug", "slug")
				.body(jsonld(), decode("</employees/>"
						+" :forename 'Tino' ;"
						+" :surname 'Faussone' ;"
						+" :email 'tfaussone@classicmodelcars.com' ;"
						+" :title 'Sales Rep' ;"
						+" :seniority 1 ."
				)).attribute(JSONLDFormat.shape(), Employee);
	}


	@Test void testCreate() {
		exec(() -> new GraphActorCreator()

				.handle(request())

				.accept(response -> {

					final IRI location=response.header("Location")
							.map(path -> iri(response.request().base(), path)) // resolve root-relative location
							.orElse(null);

					assertThat(response)
							.hasStatus(com.metreeca.rest.Response.Created)
							.doesNotHaveBody();

					assertThat(location)
							.as("resource created with supplied slug")
							.isEqualTo(item("employees/slug"));

					assertThat(model())
							.as("resource description stored into the graph")
							.hasSubset(
									statement(location, RDF.TYPE, term("Employee")),
									statement(location, term("forename"), literal("Tino")),
									statement(location, term("surname"), literal("Faussone"))
							);

				}));
	}

	@Test void testConflictingSlug() {
		exec(() -> {

			final GraphActorCreator creator=new GraphActorCreator();

			creator.handle(request()).accept(response -> {});

			final Model snapshot=model();

			creator.handle(request()).accept(response -> {

				assertThat(response)
						.hasStatus(com.metreeca.rest.Response.InternalServerError);

				assertThat(model())
						.as("graph unchanged")
						.isIsomorphicTo(snapshot);

			});

		});
	}

}
