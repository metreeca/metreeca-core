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


import com.metreeca.json.ValuesTest;
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.ValuesTest.birt;
import static com.metreeca.json.ValuesTest.term;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf4j.assets.GraphTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONLDFormat.shape;


final class GraphActorDeleterTest {

	private static Options options() {
		return new Options(new GraphEngine());
	}


	@Test void testDelete() {
		exec(model(birt()), () -> new GraphActorDeleter(options())

				.handle(new Request()
						.base(ValuesTest.Base)
						.path("/employees/1370")
						.attribute(shape(), and(
								field(RDF.TYPE),
								field(RDFS.LABEL),
								field(term("forename")),
								field(term("surname")),
								field(term("email")),
								field(term("title")),
								field(term("code")),
								field(term("office")),
								field(term("seniority")),
								field(term("supervisor")),
								field(term("subordinate"))
						))
				)

				.accept(response -> {

					assertThat(response)
							.hasStatus(Response.NoContent)
							.doesNotHaveBody();

					assertThat(model("construct where { <employees/1370> ?p ?o }"))
							.isEmpty();

					assertThat(model("construct where { ?s a :Employee; ?p ?o. }"))
							.isNotEmpty();

				}));
	}

	@Test void testReportUnknown() {
		exec(() -> new GraphActorDeleter(options())

				.handle(new Request()
						.path("/unknown")
				)

				.accept(response -> assertThat(response)
						.hasStatus(Response.NotFound)
						.doesNotHaveBody()
				)
		);
	}

}
