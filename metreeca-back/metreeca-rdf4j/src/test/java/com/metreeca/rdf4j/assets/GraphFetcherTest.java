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

import com.metreeca.json.*;
import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.Xtream;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.metreeca.json.Focus.focus;
import static com.metreeca.json.Frame.inverse;
import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Order.decreasing;
import static com.metreeca.json.Order.increasing;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.*;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.json.queries.Stats.stats;
import static com.metreeca.json.queries.Terms.terms;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.Like.like;
import static com.metreeca.json.shapes.Localized.localized;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.MinExclusive.minExclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.MinLength.minLength;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.Stem.stem;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rdf4j.assets.GraphTest.localized;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static com.metreeca.rdf4j.assets.GraphTest.tuples;
import static com.metreeca.rest.Context.asset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.stream.Collectors.toList;


final class GraphFetcherTest {

	private static final IRI Root=iri("app:/");

	private static final IRI StardogDefault=iri("tag:stardog:api:context:default");

	static final Shape EmployeeShape=and(

			filter(field(RDF.TYPE, term("Employee"))),

			field(RDFS.LABEL),
			field(term("forename")),
			field(term("surname")),
			field(term("email")),
			field(term("title")),
			field(term("code")),
			field(term("seniority")),
			field(term("office")),
			field(term("supervisor"))

	);

	private static final Options options=new Options() {};


	private static void exec(final Runnable task) {
		GraphTest.exec(model(localized(birt(), "", "en", "it")), task);
	}

	private static Shape always(final Shape... shapes) { return mode(Convey, Filter, Refine).then(shapes); }

	private Collection<Statement> query(final IRI resource, final Query query) {
		return asset(Graph.graph()).exec(connection -> {
			return query.map(new GraphFetcher(connection, resource, options))

					.stream()

					// ;(virtuoso) counts reported as xsd:int // !!! review dependency

					.map(statement -> type(statement.getObject()).equals(XSD.INT) ? statement(
							statement.getSubject(),
							statement.getPredicate(),
							literal(integer(statement.getObject()).orElse(BigInteger.ZERO)),
							statement.getContext()
					) : statement)

					.collect(toList());
		});
	}

	private Collection<Statement> graph(final String sparql) {
		return model(sparql)

				.stream()

				// ;(stardog) statement from default context explicitly tagged // !!! review dependency

				.map(statement -> StardogDefault.equals(statement.getContext()) ? statement(
						statement.getSubject(),
						statement.getPredicate(),
						statement.getObject()
				) : statement)

				.collect(toList());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Items {

		@Test void testEmptyShape() {
			exec(() -> assertThat(query(

					Root, items(and())

			)).isEmpty());
		}

		@Test void testEmptyResultSet() {
			exec(() -> {
				assertThat(query(

						Root, items(field(RDF.TYPE, all(RDF.NIL)))

				)).isEmpty();
			});
		}

		@Test void testEmptyProjection() {
			exec(() -> assertThat(query(

					Root, items(filter(clazz(term("Office"))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\t<app:/> ldp:contains ?office.\n"
							+"\t\n"
							+"} where {\n"
							+"\n"
							+"\t?office a :Office\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testMatching() {
			exec(() -> {
				assertThat(query(

						Root, items(field(RDF.TYPE, filter(all(term("Employee")))))

				)).isIsomorphicTo(graph(

						"construct { <app:/> ldp:contains ?employee. ?employee a :Employee }"
								+" where { ?employee a :Employee }"

				));
			});
		}

		@Test void testSorting() {
			exec(() -> {

				final String query="select ?employee "
						+" where { ?employee a :Employee; rdfs:label ?label; :office ?office }";

				final Shape shape=filter().then(clazz(term("Employee")));

				final Function<Query, List<Value>> actual=edges -> query(Root, edges)
						.stream()
						.filter(Values.pattern(null, LDP.CONTAINS, null))
						.map(Statement::getObject)
						.distinct()
						.collect(toList());

				final Function<String, List<Value>> expected=sparql -> tuples(sparql)
						.stream()
						.map(map -> map.get("employee"))
						.distinct()
						.collect(toList());


				assertThat(actual.apply(items(shape)))
						.as("default (on value)")
						.containsExactlyElementsOf(expected.apply(query+" order by ?employee"));

				assertThat(actual.apply(items(shape, asList(increasing(RDFS.LABEL)))))
						.as("custom increasing")
						.containsExactlyElementsOf(expected.apply(query+" order by ?label"));

				assertThat(actual.apply(items(shape, asList(decreasing(RDFS.LABEL)))))
						.as("custom decreasing")
						.containsExactlyElementsOf(expected.apply(query+" order by desc(?label)"));

				assertThat(actual.apply(items(shape, asList(increasing(term("office")),
						increasing(RDFS.LABEL)))))
						.as("custom combined")
						.containsExactlyElementsOf(expected.apply(query+" order by ?office ?label"));

				assertThat(actual.apply(items(shape, asList(decreasing()))))
						.as("custom on root")
						.containsExactlyElementsOf(expected.apply(query+" order by desc(?employee)"));

			});
		}

	}

	@Nested final class Stats {

		@Test void testEmptyResultSet() {
			exec(() -> {
				assertThat(query(

						Root, stats(field(RDF.TYPE, all(RDF.NIL)), asList(), 0, 0)

				)).isIsomorphicTo(decode(

						"@prefix app: <app:/terms#> . <app:/> app:count 0 ."

				));
			});
		}

		@Test void testEmptyProjection() {
			exec(() -> assertThat(query(

					Root, stats(filter(clazz(term("Office"))), emptyList(), 0, 0)

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

					Root, stats(all(item("employees/1370")), asList(term("account")), 0, 0)

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

	@Nested final class Terms {

		@Test void testEmptyResultSet() {
			exec(() -> {
				assertThat(query(

						Root, terms(field(RDF.TYPE, all(RDF.NIL)), emptyList(), 0, 0)

				)).isEmpty();
			});
		}

		@Test void testEmptyProjection() {
			exec(() -> assertThat(query(

					Root, terms(filter(clazz(term("Office"))), emptyList(), 0, 0)

			)).isIsomorphicTo(Xtream.from(

					graph("construct { \n"
							+"\n"
							+"\t<app:/> app:terms [\n"
							+"\t\tapp:value ?office;\n"
							+"\t\tapp:count 1\n"
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

					Root, terms(all(item("employees/1370")), asList(term("account")), 0, 0)

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\t<app:/> app:items [\n"
							+"\t\tapp:value ?account;\n"
							+"\t\tapp:count 1\n"
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


	@Nested final class Anchors {

		@Test void testFilterDefaultToBasicContainment() {
			exec(() -> assertThat(query(

					item("/employees-basic/"), items(convey(field(RDFS.LABEL)))

			)).isIsomorphicTo(graph(

					"construct where { </employees-basic/> ldp:contains [rdfs:label ?label] }"

			)));
		}

		@Test void testResolveReferencesToTarget() {
			exec(() -> assertThat(query(

					item("/employees-basic/"), items(and(
							filter().then(field(inverse(LDP.CONTAINS), focus())),
							convey().then(field(RDFS.LABEL))
					))

			)).isIsomorphicTo(graph(

					"construct where { </employees-basic/> ldp:contains [rdfs:label ?label] }"

			)));
		}

	}


	@Nested final class Shapes {

		@Nested final class ValueConstraints {

			@Test void testDatatype() {
				exec(() -> {

					assertThat(query(

							Root, items(field(term("code"), filter(datatype(XSD.INTEGER))))

					)).isEmpty();

					assertThat(query(

							Root, items(field(term("code"), filter(datatype(XSD.STRING))))

					)).isIsomorphicTo(graph(

							"construct {\n"
									+"\n"
									+"\t<app:/> ldp:contains ?item.\n"
									+"\t?item :code ?code.\n"
									+"\n"
									+"} where {\n"
									+"\n"
									+"\t?item :code ?code filter ( datatype(?code) = xsd:string )\n"
									+"\n"
									+"}"

					));

				});
			}

			@Test void testClazz() {
				exec(() -> assertThat(query(

						Root, items(and(field(RDF.TYPE), filter(clazz(term("Employee")))))

				)).isIsomorphicTo(graph(

						"construct {\n"
								+"\n"
								+"\t<app:/> ldp:contains ?employee.\n"
								+"\t?employee a ?type\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\tvalues ?type { :Employee }\n"
								+"\n"
								+"\t?employee a ?type\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testRange() {
				exec(() -> assertThatThrownBy(() -> query(

						Root, items(field(term("office"), filter(range(
								item("employees/1621"),
								item("employees/1625")
						))))

				)).isInstanceOf(UnsupportedOperationException.class));
			}


			@Test void testMinExclusiveConstraint() {
				exec(() -> assertThat(query(

						Root, items(field(term("seniority"), filter(minExclusive(literal(integer(3))))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?employee.\n"
								+"\t?employee :seniority ?seniority.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?employee :seniority ?seniority filter (?seniority > 3)\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testMaxExclusiveConstraint() {
				exec(() -> assertThat(query(

						Root, items(field(term("seniority"), filter(maxExclusive(literal(integer(3))))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?employee.\n"
								+"\t?employee :seniority ?seniority.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?employee :seniority ?seniority filter (?seniority < 3)\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testMinInclusiveConstraint() {
				exec(() -> assertThat(query(

						Root, items(field(term("seniority"), filter(minInclusive(literal(integer(3))))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?employee.\n"
								+"\t?employee :seniority ?seniority.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?employee :seniority ?seniority filter (?seniority >= 3)\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testMaxInclusiveConstraint() {
				exec(() -> assertThat(query(

						Root, items(field(term("seniority"), filter(maxInclusive(literal(integer(3))))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?employee.\n"
								+"\t?employee :seniority ?seniority.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?employee :seniority ?seniority filter (?seniority <= 3)\n"
								+"\n"
								+"}"

				)));
			}


			@Test void testMinLength() {
				exec(() -> assertThat(query(

						Root, items(field(term("forename"), filter(minLength(5))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?employee.\n"
								+"\t?employee :forename ?forename.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?employee :forename ?forename filter (strlen(str(?forename)) >= 5)\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testMaxLength() {
				exec(() -> assertThat(query(

						Root, items(field(term("forename"), filter(maxLength(5))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?employee.\n"
								+"\t?employee :forename ?forename.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?employee :forename ?forename filter (strlen(str(?forename)) <= 5)\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testPattern() {
				exec(() -> assertThat(query(

						Root, items(field(RDFS.LABEL, filter(pattern("\\bgerard\\b", "i"))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?item.\n"
								+"\t?item rdfs:label ?label.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?item rdfs:label ?label filter regex(?label, '\\\\bgerard\\\\b', 'i')\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testLike() {
				exec(() -> assertThat(query(

						Root, items(field(RDFS.LABEL, filter(like("ger bo", true))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?item.\n"
								+"\t?item rdfs:label ?label.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?item rdfs:label ?label, 'Gerard Bondur'^^xsd:string\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testStem() {
				exec(() -> assertThat(query(

						Root, items(field(RDFS.LABEL, filter(stem("Gerard B"))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?item.\n"
								+"\t?item rdfs:label ?label.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?item rdfs:label ?label, 'Gerard Bondur'^^xsd:string\n"
								+"\n"
								+"}"

				)));
			}

		}

		@Nested final class SetConstraints {

			@Test void testMinCount() {
				exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> query(

						Root, items(field(term("employee"), filter(minCount(3))))

				)));
			}

			@Test void testMaxCount() {
				exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> query(

						Root, items(field(term("employee"), filter(maxCount(3))))

				)));
			}


			@Test void testAllDirect() {
				exec(() -> assertThat(query(

						Root, items(field(term("employee"), filter(all(
								item("employees/1002"),
								item("employees/1056")
						))))

				)).isIsomorphicTo(graph(

						"construct { \n"
								+"\n"
								+"\t<app:/> ldp:contains ?item.\n"
								+"\t?item :employee ?employee.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\t?item :employee ?employee, <employees/1002>, <employees/1056>.\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testAllInverse() {
				exec(() -> {
					assertThat(query(

							Root, items(field(inverse(term("office")), filter(all(
									item("employees/1002"),
									item("employees/1056")
							))))

					)).isIsomorphicTo(graph(

							"construct {\n"
									+"\n"
									+"\t<app:/> ldp:contains ?office.\n"
									+"\t?employee :office ?office.\n"
									+"\n"
									+"} where {\n"
									+"\n"
									+"\t?office ^:office ?employee, <employees/1002>, <employees/1056>.\n"
									+"\n"
									+"}"

					));
				});
			}

			@Test void testAllRoot() {
				exec(() -> assertThat(query(

						Root, items(and(
								filter(all(item("employees/1002"), item("employees/1056"))),
								field(RDF.TYPE)
						))

				)).isIsomorphicTo(graph(

						"construct {\n"
								+"\n"
								+"\t<app:/> ldp:contains ?employee.\n"
								+"\t?employee a ?type\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\tvalues ?employee { <employees/1002> <employees/1056> }\n"
								+"\n"
								+"\t?employee a ?type\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testAllSingleton() {
				exec(() -> {
					assertThat(query(

							Root, items(field(term("employee"), filter(all(item("employees/1002")))))

					)).isIsomorphicTo(graph(

							"construct {\n"
									+"\n"
									+"\t<app:/> ldp:contains ?office.\n"
									+"\t?office :employee ?employee.\n"
									+"\n"
									+"} where {\n"
									+"\n"
									+"\t?office :employee ?employee, <employees/1002>\n"
									+"\n"
									+"}"

					));
				});
			}


			@Test void testAny() {
				exec(() -> {
					assertThat(query(

							Root, items(field(term("employee"), filter(any(
									item("employees/1002"), item("employees/1056")
							))))

					)).isIsomorphicTo(graph(

							"construct {\n"
									+"\n"
									+"\t<app:/> ldp:contains ?office.\n"
									+"\t?office :employee ?employee.\n"
									+"\n"
									+"} where {\n"
									+"\n"
									+"\t?office :employee ?employee, ?value filter (?value in (<employees/1002>, "
									+"<employees/1056>))\n"
									+"\n"
									+"}"

					));
				});
			}

			@Test void testAnySingleton() {
				exec(() -> {
					assertThat(query(

							Root, items(field(term("employee"), filter(any(
									item("employees/1002")
							))))

					)).isIsomorphicTo(graph(

							"construct {\n"
									+"\n"
									+"\t<app:/> ldp:contains ?office.\n"
									+"\t?office :employee ?employee.\n"
									+"\n"
									+"} where {\n"
									+"\n"
									+"\t?office :employee ?employee, <employees/1002>\n"
									+"\n"
									+"}"

					));
				});
			}

			@Test void testAnyRoot() {
				exec(() -> assertThat(query(

						Root, items(and(
								filter(any(item("employees/1002"), item("employees/1056"))),
								field(RDFS.LABEL)
						))

				)).isIsomorphicTo(graph(

						"construct {\n"
								+"\n"
								+"\t<app:/> ldp:contains ?employee.\n"
								+"\t?employee rdfs:label ?label.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\tvalues ?employee {\n"
								+"\t\t<employees/1002>\n"
								+"\t\t<employees/1056>\n"
								+"\t}\n"
								+"\n"
								+"\t?employee rdfs:label ?label\n"
								+"\n"
								+"}"

				)));
			}


			@Test void testLocalized() {
				exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> query(

						Root, items(field(term("employee"), filter(localized())))

				)));
			}

		}

		@Nested final class StructuralConstraints {

			@Test void testField() {
				exec(() -> assertThat(query(

						Root, items(and(filter(field(term("country"))), field(term("country"))))

				)).isIsomorphicTo(graph(

						"construct {\n"
								+"\n"
								+"\t<app:/> ldp:contains ?item.\n"
								+"\t?item :country ?country.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\t?item :country ?country.\n"
								+"\n"
								+"}"

				)));
			}

		}

		@Nested final class LogicalConstraints {

			@Test void testGuard() { // reject partially redacted shapes
				exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
						query(Root, items(guard("axis", RDF.NIL)))
				));
			}

			@Test void testAnd() {
				exec(() -> assertThat(query(

						Root, items(and(
								always(field(term("country"))),
								always(field(term("city")))
						))

				)).isIsomorphicTo(graph(

						"construct {\n"
								+"\n"
								+"\t<app:/> ldp:contains ?item.\n"
								+"\t?item :country ?country; :city ?city.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\t?item :country ?country; :city ?city.\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testOr() {
				exec(() -> assertThat(query(

						Root, items(and(field(RDF.TYPE), filter(or(
								clazz(term("Office")),
								clazz(term("Employee"))
						))))


				)).isIsomorphicTo(graph(

						"construct {\n"
								+"\n"
								+"\t<app:/> ldp:contains ?item.\n"
								+"\t?item a ?type.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\tvalues ?type {\n"
								+"\t\t:Office\n"
								+"\t\t:Employee\n"
								+"\t}\n"
								+"\n"
								+"\t?item a ?type.\n"
								+"\n"
								+"}"

				)));
			}

			@Test void testWhen() {
				exec(() -> assertThatThrownBy(() -> query(

						Root, items(when(guard("axis", RDF.NIL), clazz(RDFS.LITERAL)))

				)).isInstanceOf(UnsupportedOperationException.class));
			}

		}

	}

	///////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testUseIndependentPatternsAndFilters() {
		exec(() -> {
			assertThat(query(

					Root, items(and(field(term("employee")), filter().then(field(term("employee"), any(
							item("employees/1002"),
							item("employees/1188")
					)))))

			)).isIsomorphicTo(graph(

					""
							+"\n"
							+"construct {\n"
							+"\n"
							+"\t<app:/> ldp:contains ?office.\n"
							+"\t?office :employee ?employee\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office :employee ?employee, ?x filter (?x in (<employees/1002>, <employees/1188>))\n"
							+"\n"
							+"}"

			));
		});
	}


	@Nested final class SameAs {

		private Collection<Statement> query(final Query query) {
			return asset(Graph.graph()).exec(connection -> {
				return query.map(new GraphFetcher(connection, Root, new Options() {

					@Override public boolean same() { return true;}

				}));
			});
		}


		@Test void testClassFilters() {
			exec(() -> assertThat(query(

					items(filter(clazz(term("Alias"))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\t<app:/> ldp:contains ?item.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?item a ?type values ?type { :Office :Alias }\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testEdgeFilters() {
			exec(() -> assertThat(query(

					items(filter(field(RDF.TYPE, term("Alias"))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\t<app:/> ldp:contains ?item. "
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?item (owl:sameAs|^owl:sameAs)*/a/(owl:sameAs|^owl:sameAs)* ?type"
							+" values ?type { :Office :Alias }\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testFieldPatterns() {
			exec(() -> assertThat(query(

					items(and(field(RDFS.LABEL), filter(clazz(term("Alias")))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\t<app:/> ldp:contains ?item. "
							+"?item rdfs:label ?label.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tvalues ?type { :Office :Alias }\n"
							+"\n"
							+"\t?item (owl:sameAs|^owl:sameAs)*/a/(owl:sameAs|^owl:sameAs)* ?type;\n"
							+"\t\t(owl:sameAs|^owl:sameAs)*/rdfs:label/(owl:sameAs|^owl:sameAs)* ?label\n"
							+"\n"
							+"}"

			)));
		}

	}


}
