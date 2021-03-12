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

import com.metreeca.json.Query;
import com.metreeca.json.Shape;
import com.metreeca.rdf4j.assets.GraphEngine.Options;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.*;

import java.math.BigInteger;
import java.util.Collection;

import static com.metreeca.json.Focus.focus;
import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.small;
import static com.metreeca.json.queries.Items.items;
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
import static com.metreeca.rdf4j.assets.GraphTest.graph;
import static com.metreeca.rdf4j.assets.GraphTest.localized;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static com.metreeca.rest.Context.asset;

import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.stream.Collectors.toList;


final class GraphFetcherTest {

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

	private static Options options() {
		return new Options(new GraphEngine());
	}


	static void exec(final Runnable task) {
		GraphTest.exec(model(localized(small(), "", "en", "it")), task);
	}


	private Collection<Statement> query(final IRI resource, final Query query) {
		return asset(Graph.graph()).exec(connection -> {
			return query.map(new GraphFetcher(resource, options()))

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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testUseIndependentPatternsAndFilters() {
		exec(() -> assertThat(query(

				Root, items(and(field(term("employee")), filter(field(term("employee"), any(
						item("employees/1002"),
						item("employees/1188")
				)))))

		)).isIsomorphicTo(graph(

				""
						+"\n"
						+"construct {\n"
						+"\n"
						+"\t<> ldp:contains ?office.\n"
						+"\t?office :employee ?employee\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?office :employee ?employee, ?x filter (?x in (<employees/1002>, <employees/1188>))\n"
						+"\n"
						+"}"

		)));
	}


	@Test void testHandlePatternFilters() {
		exec(() -> assertThat(query(Root, items(and(

				filter(clazz(term("Office"))),

				field(RDFS.LABEL, convey(localized("en")))

		)))).isIsomorphicTo(graph(

				"construct { <> ldp:contains ?office. ?office rdfs:label ?label }"
						+" where { ?office a :Office; rdfs:label ?label filter (lang(?label) = 'en') }"

		)));
	}

	@Test void testIgnoreNonConveyPatternFilters() {
		exec(() -> assertThat(query(Root, items(and(

				filter(clazz(term("Employee"))),
				field(term("seniority"), minInclusive(integer(10)))

		)))).isIsomorphicTo(graph(

				"construct { <> ldp:contains ?employee. ?employee :seniority ?seniority }"
						+" where { ?employee a :Employee; :seniority ?seniority }"

		)));
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
							filter(field(inverse(LDP.CONTAINS), focus())),
							convey(field(RDFS.LABEL))
					))

			)).isIsomorphicTo(graph(

					"construct where { </employees-basic/> ldp:contains [rdfs:label ?label] }"

			)));
		}


		@Disabled @Test void testVirtualClassFilters() {
			exec(() -> assertThat(query(Root, items(

					filter(field(OWL.SAMEAS, clazz(term("Office"))))

			))).isIsomorphicTo(graph(

					"construct { <> ldp:contains ?item } where { ?item owl:sameAs/a :Office }"

			)));
		}

		//@Test void testVirtualEdgeFilters() {
		//	exec(() -> assertThat(query(
		//
		//			Root, items(virtual(OWL.SAMEAS, filter(field(RDF.TYPE, term("Office")))))
		//
		//	)).isIsomorphicTo(graph(
		//
		//			"construct {\n"
		//					+"\n"
		//					+"\t<> ldp:contains ?item. "
		//					+"\n"
		//					+"\n"
		//					+"} where {\n"
		//					+"\n"
		//					+"\t?item owl:sameAs/a :Office\n"
		//					+"\n"
		//					+"}"
		//
		//	)));
		//}
		//
		//@Test void testVirtualFieldPatterns() {
		//	exec(() -> assertThat(query(
		//
		//			Root, items(and(filter(clazz(term("Alias"))), virtual(OWL.SAMEAS, field(RDFS.LABEL))))
		//
		//	)).isIsomorphicTo(graph(
		//
		//			"construct {\n"
		//					+"\n"
		//					+"\t<> ldp:contains ?item. "
		//					+"?item rdfs:label ?label.\n"
		//					+"\n"
		//					+"} where {\n"
		//					+"\n"
		//					+"\t?item a :Alias; owl:sameAs/rdfs:label ?label"
		//					+"\n"
		//					+"}"
		//
		//	)));
		//}

	}

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
								+"\t<> ldp:contains ?item.\n"
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
							+"\t<> ldp:contains ?employee.\n"
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
							+"\t<> ldp:contains ?employee.\n"
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
							+"\t<> ldp:contains ?employee.\n"
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
							+"\t<> ldp:contains ?employee.\n"
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
							+"\t<> ldp:contains ?employee.\n"
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
							+"\t<> ldp:contains ?employee.\n"
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
							+"\t<> ldp:contains ?employee.\n"
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
							+"\t<> ldp:contains ?item.\n"
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
							+"\t<> ldp:contains ?item.\n"
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
							+"\t<> ldp:contains ?item.\n"
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
							+"\t<> ldp:contains ?item.\n"
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
								+"\t<> ldp:contains ?office.\n"
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
							+"\t<> ldp:contains ?employee.\n"
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
								+"\t<> ldp:contains ?office.\n"
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
			exec(() -> assertThat(query(

					Root, items(field(term("employee"), filter(any(
							item("employees/1002"), item("employees/1056")
					))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\t<> ldp:contains ?office.\n"
							+"\t?office :employee ?employee.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office :employee ?employee, ?value filter (?value in (<employees/1002>, "
							+"<employees/1056>))\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testAnySingleton() {
			exec(() -> assertThat(query(

					Root, items(field(term("employee"), filter(any(
							item("employees/1002")
					))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\t<> ldp:contains ?office.\n"
							+"\t?office :employee ?employee.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office :employee ?employee, <employees/1002>\n"
							+"\n"
							+"}"

			)));
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
							+"\t<> ldp:contains ?employee.\n"
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
							+"\t<> ldp:contains ?item.\n"
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

		private Shape always(final Shape... shapes) { return mode(Filter, Convey).then(shapes); }


		@Test void testGuard() { // reject partially redacted shapes
			exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
					query(Root, items(guard("axis", RDF.NIL)))
			));
		}

		@Test void testWhen() { // reject conditional shapes
			exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->

					query(Root, items(when(guard("axis", RDF.NIL), field(RDF.VALUE))))

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
							+"\t<> ldp:contains ?item.\n"
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
							+"\t<> ldp:contains ?item.\n"
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

	}

}
