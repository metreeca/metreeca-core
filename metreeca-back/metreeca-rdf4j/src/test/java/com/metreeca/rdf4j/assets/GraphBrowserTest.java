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
import com.metreeca.json.ValuesTest;
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.Request;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.literal;
import static com.metreeca.json.ValuesTest.birt;
import static com.metreeca.json.ValuesTest.term;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.rdf4j.assets.GraphFetcherTest.EmployeeShape;
import static com.metreeca.rdf4j.assets.GraphTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.shape;

import static org.assertj.core.api.Assertions.assertThat;

@Nested final class GraphBrowserTest {

	private static final Options options=new Options() {};

	private Request request() {
		return new Request()
				.base(ValuesTest.Base)
				.path("/employees/")
				.attribute(shape(), EmployeeShape);
	}


	@Test void testBrowse() {
		exec(model(birt()), () -> new GraphBrowser(options)

				.handle(request())

				.accept(response -> assertThat(response)

						.hasStatus(OK)
						.hasAttribute(shape(), shape -> assertThat(shape).isNotEqualTo(and()))

						.hasBody(jsonld(), rdf -> assertThat(rdf)
								.hasStatement(iri(response.item()), Shape.Contains, null)
								.hasSubset(model("construct { ?e rdfs:label ?label; :seniority ?seniority }\n"
										+"where { ?e a :Employee; rdfs:label ?label; :seniority ?seniority }"
								))
						)
				)
		);
	}

	@Test void testBrowseFiltered() {
		exec(model(birt()), () -> new GraphBrowser(options)

				.handle(request()
						.query("title=Sales+Rep")
				)

				.accept(response -> assertThat(response)

						.hasStatus(OK)
						.hasAttribute(shape(), shape -> assertThat(shape).isNotEqualTo(and()))

						.hasBody(jsonld(), rdf -> assertThat(rdf)

								.hasSubset(model(""
										+"construct { ?e :title ?t; :seniority ?seniority }\n"
										+"where { ?e a :Employee; :title ?t, 'Sales Rep'; :seniority ?seniority }"
								))

								.as("only resources matching filter included")
								.doesNotHaveStatement(null, term("title"), literal("President"))
						)
				)
		);
	}

	@Test void testSliceTermsQueries() {
		exec(model(birt()), () -> new GraphBrowser(options)

				.handle(request()
						.query("_terms=office&_offset=1&_limit=3")
				)

				.accept(response -> assertThat(response)

						.hasStatus(OK)
						.hasAttribute(shape(), shape -> assertThat(shape).isNotEqualTo(and()))

						.hasBody(jsonld(), rdf -> assertThat(rdf)

								.isIsomorphicTo(model(""
										+"construct { \n"
										+"\n"
										+"\t<employees/> app:terms [app:value ?o; app:count ?c]. \n"
										+"\t?o rdfs:label ?l\n"
										+"\n"
										+"} where { { select ?o ?l (count(?e) as ?c) {\n"
										+"\n"
										+"\t?e a :Employee; :office ?o.\n"
										+"\t?o rdfs:label ?l.\n"
										+"\n"
										+"} group by ?o ?l order by desc(?c) offset 1 limit 3 } }"
								))

						)
				)
		);
	}


}
