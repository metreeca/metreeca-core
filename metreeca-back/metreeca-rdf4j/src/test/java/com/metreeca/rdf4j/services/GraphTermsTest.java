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

import com.metreeca.json.Values;
import com.metreeca.json.queries.Terms;
import com.metreeca.rest.Config;
import com.metreeca.rest.Xtream;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.function.Supplier;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.queries.Terms.terms;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.rdf4j.services.GraphFactsTest.exec;
import static com.metreeca.rdf4j.services.GraphTest.graph;

import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

final class GraphTermsTest {

	private static Config options() {
		return new Config() {
			@Override public <V> V get(final Supplier<V> option) {
				return option.get();
			}
		};
	}

	private Collection<Statement> query(final Terms terms) {
		return new GraphTerms(options()).process(Values.Root, terms);
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

				terms(filter(clazz(term("Office"))), emptyList(), 0, 0)

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
		exec(() -> assertThat(query(terms(

				and(all(item("employees/1370")), field(term("account"))),

				singletonList(term("account")),

				0, 0

		))).isIsomorphicTo(graph(

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


	@Nested final class AnchoringPaths {

		@Test void testReportUnknownSteps() {
			exec(() -> {

				assertThatIllegalArgumentException().isThrownBy(() -> query(terms(
						field(term("country")),
						singletonList(term("unknown")),
						0, 0
				)));

				assertThatIllegalArgumentException().isThrownBy(() -> query(terms(
						field(term("country")),
						asList(term("country"), term("unknown")),
						0, 0
				)));

			});
		}

		@Test void testReportFilteringSteps() {
			exec(() -> assertThatIllegalArgumentException().isThrownBy(() -> query(terms(

					and(
							filter(field(term("country"))),
							field(term("city"))
					),

					singletonList(term("country")),

					0, 0
			))));
		}

		@Test void testTraversingLink() {
			exec(() -> assertThat(query(terms(

					and(
							filter(clazz(term("Alias"))),
							link(OWL.SAMEAS, field(term("country")))
					),

					singletonList(term("country")),

					0, 0

			)).stream().filter(s -> !s.getPredicate().equals(RDFS.LABEL)).collect(toList())).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\t<> :terms [\n"
							+"\t\t:value ?value;\n"
							+"\t\t:count ?count\n"
							+"\t].\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t{ select (?country as ?value) (count(distinct ?alias) as ?count) {\n"
							+"\n"
							+"\t\t?alias a :Alias; owl:sameAs/:country ?country\n"
							+"\n"
							+"\t} group by ?country }\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testFiltered() {
			exec(() -> assertThat(query(terms(

					and(
							filter(clazz(term("Employee"))),
							filter(field(term("seniority"), minInclusive(integer(3)))),
							field(term("seniority"))
					),

					singletonList(term("seniority")),

					0, 0

			)).stream().filter(s -> !s.getPredicate().equals(RDFS.LABEL)).collect(toList())).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\t<> :terms [\n"
							+"\t\t:value ?value;\n"
							+"\t\t:count ?count\n"
							+"\t].\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t{ select (?seniority as ?value) (count(distinct ?employee) as ?count) {\n"
							+"\n"
							+"\t\t?employee a :Employee; :seniority ?seniority filter (?seniority >= 3)\n"
							+"\n"
							+"\t} group by ?seniority }\n"
							+"\n"
							+"}"

			)));

		}


	}

}
