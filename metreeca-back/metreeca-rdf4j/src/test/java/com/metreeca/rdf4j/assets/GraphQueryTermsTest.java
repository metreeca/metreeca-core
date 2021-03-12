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

import com.metreeca.json.Values;
import com.metreeca.json.queries.Terms;
import com.metreeca.rest.Xtream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.queries.Terms.terms;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.rdf4j.assets.GraphFetcherTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.graph;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class GraphQueryTermsTest {

	private Collection<Statement> query(final Terms terms) {
		return new GraphQueryTerms(new GraphEngine.Options(new GraphEngine())).process(Values.Root, terms);
	}


	@Test void testEmptyResultSet() {
		exec(() -> {
			assertThat(query(

					terms(field(RDF.TYPE, all(RDF.NIL)), emptyList(), 0, 0)

			)).isEmpty();
		});
	}

	@Test void testEmptyProjection() {
		exec(() -> assertThat(query(

				terms(filter(clazz(Values.term("Office"))), emptyList(), 0, 0)

		)).isIsomorphicTo(Xtream.from(

				graph("construct { \n"
						+"\n"
						+"\t<> :terms [\n"
						+"\t\t:value ?office;\n"
						+"\t\t:count 1\n"
						+"\t].\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?office a :Office;\n"
						+"\n"
						+"}"
				),

				graph("construct { \n"
						+"\n"
						+"\t?office rdfs:label ?label.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?office a :Office; \n"
						+"\t\trdfs:label ?label.\n"
						+"\n"
						+"}"
				)

		).collect(toList())));
	}

	@Test void testRootConstraints() {
		exec(() -> assertThat(query(

				terms(all(Values.item("employees/1370")), singletonList(Values.term("account")), 0, 0)

		)).isIsomorphicTo(graph(

				"construct { \n"
						+"\n"
						+"\t<> :items [\n"
						+"\t\t:value ?account;\n"
						+"\t\t:count 1\n"
						+"\t].\n"
						+"\n"
						+"\t?account rdfs:label ?label.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t<employees/1370> :account ?account.\n"
						+"\n"
						+"\t?account rdfs:label ?label.\n"
						+"\n"
						+"}"

		)));
	}

}
