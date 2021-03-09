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

import com.metreeca.json.queries.Stats;
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.Xtream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.ValuesTest.*;
import static com.metreeca.json.queries.Stats.stats;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.rdf4j.assets.GraphFetcherTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.graph;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class GraphQueryStatsTest {

	private Collection<Statement> query(final Stats stats) {
		return new GraphQueryStats(new Options(new GraphEngine())).process(iri("app:/"), stats);
	}


	@Test void testEmptyResultSet() {
		exec(() -> assertThat(query(

				stats(field(RDF.TYPE, all(RDF.NIL)), emptyList(), 0, 0)

		)).isIsomorphicTo(decode(

				"@prefix app: <app:/terms#> . <app:/> app:count 0 ."

		)));
	}

	@Test void testEmptyProjection() {
		exec(() -> assertThat(query(

				stats(filter(clazz(term("Office"))), emptyList(), 0, 0)

		)).isIsomorphicTo(Xtream.from(

				graph("construct { \n"
						+"\n"
						+"\t<app:/> app:count ?count; app:min ?min; app:max ?max;\n"
						+"\n"
						+"\t\t\tapp:stats app:iri.\n"
						+"\t\t\t\n"
						+"\tapp:iri app:count ?count; app:min ?min; app:max ?max.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\tselect (count(?p) as ?count) (min(?p) as ?min) (max(?p) as ?max) {\n"
						+"\n"
						+"\t\t?p a :Office\n"
						+"\n"
						+"\t}\n"
						+"\n"
						+"}"
				),

				graph("construct { \n"
						+"\n"
						+"\t?min rdfs:label ?min_label.\n"
						+"\t?max rdfs:label ?max_label.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t{ select (min(?p) as ?min) (max(?p) as ?max) { ?p a :Office } }\n"+
						"\n"
						+"\t?min "
						+"rdfs:label ?min_label.\n"
						+"\t?max rdfs:label ?max_label.\n"
						+"\n"
						+"}"
				)

		).collect(toList())));
	}

	@Test void testRootConstraints() {
		exec(() -> assertThat(query(

				stats(all(item("employees/1370")), singletonList(term("account")), 0, 0)

		)).isIsomorphicTo(graph(

				"construct { \n"
						+"\n"
						+"\t<app:/> \n"
						+"\t\tapp:count ?count; app:min ?min; app:max ?max.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\tselect (count(?account) as ?count) (min(?account) as ?min) (max(?account) as ?max) {\n"
						+"\n"
						+"\t\t<employees/1370> :account ?account\n"
						+"\n"
						+"\t}\n"
						+"\n"
						+"}"

		)));
	}

}
