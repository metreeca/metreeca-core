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

package com.metreeca.rdf4j.services;

import com.metreeca.json.queries.Stats;
import com.metreeca.rest.Xtream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.decode;
import static com.metreeca.json.queries.Stats.stats;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.rdf4j.services.GraphFactsTest.exec;
import static com.metreeca.rdf4j.services.GraphFactsTest.options;
import static com.metreeca.rdf4j.services.GraphTest.graph;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class GraphStatsTest {

	private Collection<Statement> query(final Stats stats) {
		return new GraphStats(options()).process(Root, stats).model();
	}


	@Test void testEmptyResultSet() {
		exec(() -> assertThat(query(

				stats(field(RDF.TYPE, all(RDF.NIL)), emptyList(), 0, 0)

		)).isIsomorphicTo(decode(

				"<> :count 0 ."

		)));
	}

	@Test void testEmptyProjection() {
		exec(() -> assertThat(query(

				stats(filter(clazz(term("Office"))), emptyList(), 0, 0)

		)).isIsomorphicTo(Xtream.from(

				graph("construct { \n"
						+"\n"
						+"\t<> :count ?count; :min ?min; :max ?max;\n"
						+"\n"
						+"\t\t\t:stats :iri.\n"
						+"\t\t\t\n"
						+"\t:iri :count ?count; :min ?min; :max ?max.\n"
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
		exec(() -> assertThat(query(stats(

				and(all(item("employees/1370")), field(term("account"))),

				singletonList(term("account")),

				0, 0

		))).isIsomorphicTo(graph(

				"construct { \n"
						+"\n"
						+"\t<> \n"
						+"\t\t:count ?count; :min ?min; :max ?max.\n"
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

	@Nested final class AnchoringPaths {

		@Test void testReportUnknownSteps() {
			exec(() -> {

				assertThatIllegalArgumentException().isThrownBy(() -> query(stats(
						field(term("country")),
						singletonList(term("unknown")),
						0, 0
				)));

				assertThatIllegalArgumentException().isThrownBy(() -> query(stats(
						field(term("country")),
						asList(term("country"), term("unknown")),
						0, 0
				)));

			});
		}

		@Test void testReportFilteringSteps() {
			exec(() -> assertThatIllegalArgumentException().isThrownBy(() -> query(stats(

					and(
							filter(field(term("country"))),
							field(term("city"))
					),

					singletonList(term("country")),

					0, 0
			))));
		}

		@Test void testTraversingLink() {
			exec(() -> assertThat(query(stats(

					and(
							filter(clazz(term("Alias"))),
							link(OWL.SAMEAS, field(term("country")))
					),

					singletonList(term("country")),

					0, 0

			)).stream().filter(s -> !s.getPredicate().equals(RDFS.LABEL)).collect(toList())).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\t<> :count ?count; :min ?min; :max ?max; :stats :iri.\n"
							+"\t:iri :count ?count; :min ?min; :max ?max.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tselect (count(distinct ?alias) as ?count) (min(?country) as ?min) (max(?country) as "
							+"?max) "
							+"{\n"
							+"\n"
							+"\t\t?alias a :Alias; owl:sameAs/:country ?country\n"
							+"\n"
							+"\t}\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testFiltered() {
			exec(() -> assertThat(query(stats(

					and(
							filter(clazz(term("Employee"))),
							filter(field(term("seniority"), minInclusive(literal(3)))),
							field(term("seniority"))
					),

					singletonList(term("seniority")),

					0, 0

			)).stream().filter(s -> !s.getPredicate().equals(RDFS.LABEL)).collect(toList())).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\t<> :count ?count; :min ?min; :max ?max; :stats xsd:integer.\n"
							+"\txsd:integer :count ?count; :min ?min; :max ?max.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tselect \n"
							+"\n"
							+"\t\t(count(distinct ?employee) as ?count)\n"
							+"\t\t(min(?seniority) as ?min)\n"
							+"\t\t(max(?seniority) as "
							+"?max)\n"
							+"\t\t\n"
							+"\twhere "
							+"{\n"
							+"\n"
							+"\t\t?employee a :Employee; :seniority ?seniority filter (?seniority >= 3)\n"
							+"\n"
							+"\t}\n"
							+"\n"
							+"}"

			)));

		}

	}

}
