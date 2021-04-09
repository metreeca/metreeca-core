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

package com.metreeca.gcp.services;

import com.metreeca.json.Frame;
import com.metreeca.json.Shape;
import com.metreeca.rest.Config;

import com.google.cloud.datastore.Entity;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.Map;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.localize;
import static com.metreeca.json.ValuesTest.small;
import static com.metreeca.json.queries.Items.items;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.convey;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.Localized.localized;
import static com.metreeca.rest.Toolbox.service;
import static com.metreeca.rest.Xtream.entry;
import static com.metreeca.rest.Xtream.task;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;


final class DatastoreFactsTest {

	private static final IRI Alias=term("Alias");
	private static final IRI Office=term("Office");
	private static final IRI Employee=term("Employee");

	private static final Shape AliasShape=and(
			field(RDF.TYPE),
			field(RDFS.LABEL),
			field(RDFS.COMMENT)
	);

	private static final Shape OfficeShape=and(
			field(RDF.TYPE),
			field(RDFS.LABEL),
			field(RDFS.COMMENT)
	);

	private static final Shape EmployeeShape=and(
			field(RDF.TYPE),
			field(RDFS.LABEL),
			field(RDFS.COMMENT)
	);

	private static final Map<Frame, Shape> resources=resources();


	static Config config() {
		return new Config() {
			@Override public <V> V get(final Supplier<V> option) { return option.get(); }
		};
	}

	static Map<Frame, Shape> resources() {

		final Model dataset=localize(small(), "", "en", "it");

		return Stream.of(

				dataset.stream()
						.filter(pattern(null, RDF.TYPE, Alias))
						.map(Statement::getSubject)
						.map(resource -> entry(frame(resource, dataset), AliasShape)),

				dataset.stream()
						.filter(pattern(null, RDF.TYPE, Office))
						.map(Statement::getSubject)
						.map(resource -> entry(frame(resource, dataset), OfficeShape)),

				dataset.stream()
						.filter(pattern(null, RDF.TYPE, Employee))
						.map(Statement::getSubject)
						.map(resource -> entry(frame(resource, dataset), EmployeeShape))

		).flatMap(identity()).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}

	static void exec(final Runnable task) {
		DatastoreTest.exec(() -> service(datastore()).update(task(transaction ->

				transaction.put(resources.entrySet().stream().map(entry ->

						new DatastoreCodec(iri(Base), transaction.getDatastore()).decode(entry.getKey(),
								entry.getValue())

				).toArray(Entity[]::new))

		)), task);
	}


	private Collection<Frame> query(final Shape shape) {
		return service(datastore()).query(datastore -> new DatastoreItems(config())
				.process(iri(Base), items(shape))
				.map(entity -> new DatastoreCodec(iri(Base), datastore).encode(entity, shape))
				.collect(toList())
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testHandlePatternFilters() {
		exec(() -> assertThat(query(and(

				filter(clazz(Office)),

				field(RDFS.LABEL, convey(localized("en")))

		))).containsExactlyInAnyOrderElementsOf(resources.keySet().stream()
				.filter(frame -> frame.values(RDF.TYPE).anyMatch(Office::equals))
				.map(frame -> frame(frame.focus()).values(RDFS.LABEL, frame.values(RDFS.LABEL)))
				.collect(toList())
		));
	}

	@Test void testUseIndependentPatternsAndFilters() {
		exec(() -> assertThat(query(and(

				field(term("employee")),

				filter(field(term("employee"), any(
						item("employees/1002"),
						item("employees/1188")
				)))

		))).containsExactlyInAnyOrderElementsOf(resources.keySet().stream()
				.filter(frame -> frame.values(RDF.TYPE).anyMatch(Office::equals))
				.collect(toList())
		));
	}

	//@Test void testIgnoreNonConveyPatternFilters() {
	//	exec(() -> ModelAssert.assertThat(query(and(
	//
	//			filter(clazz(term("Employee"))),
	//			field(term("seniority"), minInclusive(integer(10)))
	//
	//	))).isIsomorphicTo(graph(
	//
	//			"construct { <> ldp:contains ?employee. ?employee :seniority ?seniority }"
	//					+" where { ?employee a :Employee; :seniority ?seniority }"
	//
	//	)));
	//}
	//
	//
	//@Nested final class Anchors {
	//
	//	@Test void testAnchorGenericContainer() {
	//		exec(() -> ModelAssert.assertThat(query(item("/employees-basic/"), convey(field(RDFS.LABEL))))
	//		.isIsomorphicTo(graph(
	//
	//				"construct where { </employees-basic/> ldp:contains [rdfs:label ?label] }"
	//
	//		)));
	//	}
	//
	//	@Test void testResolveReferencesToTarget() {
	//		exec(() -> ModelAssert.assertThat(query(item("/employees-basic/"), and(
	//				filter(field(inverse(LDP.CONTAINS), focus())),
	//				convey(field(RDFS.LABEL))
	//		))).isIsomorphicTo(graph(
	//
	//				"construct where { </employees-basic/> ldp:contains [rdfs:label ?label] }"
	//
	//		)));
	//	}
	//
	//}
	//
	//@Nested final class ValueConstraints {
	//
	//	@Test void testDatatype() {
	//		exec(() -> {
	//
	//			ModelAssert.assertThat(query(field(term("code"), filter(datatype(XSD.INTEGER))))).isEmpty();
	//
	//			ModelAssert.assertThat(query(field(term("code"), filter(datatype(XSD.STRING))))).isIsomorphicTo(graph(
	//
	//					"construct {\n"
	//							+"\n"
	//							+"\t<> ldp:contains ?item.\n"
	//							+"\t?item :code ?code.\n"
	//							+"\n"
	//							+"} where {\n"
	//							+"\n"
	//							+"\t?item :code ?code filter ( datatype(?code) = xsd:string )\n"
	//							+"\n"
	//							+"}"
	//
	//			));
	//
	//		});
	//	}
	//
	//	@Test void testClazz() {
	//		exec(() -> ModelAssert.assertThat(query(and(field(RDF.TYPE), filter(clazz(term("Employee"))))))
	//		.isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?employee.\n"
	//						+"\t?employee a ?type\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\tvalues ?type { :Employee }\n"
	//						+"\n"
	//						+"\t?employee a ?type\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testRange() {
	//		exec(() -> assertThatThrownBy(() -> query(field(term("office"), filter(range(
	//				item("employees/1621"),
	//				item("employees/1625")
	//		))))).isInstanceOf(UnsupportedOperationException.class));
	//	}
	//
	//
	//	@Test void testMinExclusiveConstraint() {
	//		exec(() -> ModelAssert.assertThat(query(field(term("seniority"), filter(minExclusive(literal(integer(3)))))
	//		)).isIsomorphicTo(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?employee.\n"
	//						+"\t?employee :seniority ?seniority.\n"
	//						+"\t \n"
	//						+"} where { \n"
	//						+"\n"
	//						+"\t?employee :seniority ?seniority filter (?seniority > 3)\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testMaxExclusiveConstraint() {
	//		exec(() -> ModelAssert.assertThat(query(field(term("seniority"), filter(maxExclusive(literal(integer(3)))))
	//		)).isIsomorphicTo(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?employee.\n"
	//						+"\t?employee :seniority ?seniority.\n"
	//						+"\t \n"
	//						+"} where { \n"
	//						+"\n"
	//						+"\t?employee :seniority ?seniority filter (?seniority < 3)\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testMinInclusiveConstraint() {
	//		exec(() -> ModelAssert.assertThat(query(field(term("seniority"), filter(minInclusive(literal(integer(3)))))
	//		)).isIsomorphicTo(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?employee.\n"
	//						+"\t?employee :seniority ?seniority.\n"
	//						+"\t \n"
	//						+"} where { \n"
	//						+"\n"
	//						+"\t?employee :seniority ?seniority filter (?seniority >= 3)\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testMaxInclusiveConstraint() {
	//		exec(() -> ModelAssert.assertThat(query(field(term("seniority"), filter(maxInclusive(literal(integer(3)))))
	//		)).isIsomorphicTo(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?employee.\n"
	//						+"\t?employee :seniority ?seniority.\n"
	//						+"\t \n"
	//						+"} where { \n"
	//						+"\n"
	//						+"\t?employee :seniority ?seniority filter (?seniority <= 3)\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//
	//	@Test void testMinLength() {
	//		exec(() -> ModelAssert.assertThat(query(field(term("forename"), filter(minLength(5))))).isIsomorphicTo
	//		(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?employee.\n"
	//						+"\t?employee :forename ?forename.\n"
	//						+"\t \n"
	//						+"} where { \n"
	//						+"\n"
	//						+"\t?employee :forename ?forename filter (strlen(str(?forename)) >= 5)\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testMaxLength() {
	//		exec(() -> ModelAssert.assertThat(query(field(term("forename"), filter(maxLength(5))))).isIsomorphicTo
	//		(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?employee.\n"
	//						+"\t?employee :forename ?forename.\n"
	//						+"\t \n"
	//						+"} where { \n"
	//						+"\n"
	//						+"\t?employee :forename ?forename filter (strlen(str(?forename)) <= 5)\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testPattern() {
	//		exec(() -> ModelAssert.assertThat(query(field(RDFS.LABEL, filter(pattern("\\bgerard\\b", "i")))))
	//		.isIsomorphicTo(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?item.\n"
	//						+"\t?item rdfs:label ?label.\n"
	//						+"\t \n"
	//						+"} where { \n"
	//						+"\n"
	//						+"\t?item rdfs:label ?label filter regex(?label, '\\\\bgerard\\\\b', 'i')\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testLike() {
	//		exec(() -> ModelAssert.assertThat(query(field(RDFS.LABEL, filter(like("ger bo", true))))).isIsomorphicTo
	//		(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?item.\n"
	//						+"\t?item rdfs:label ?label.\n"
	//						+"\t \n"
	//						+"} where { \n"
	//						+"\n"
	//						+"\t?item rdfs:label ?label, 'Gerard Bondur'^^xsd:string\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testStem() {
	//		exec(() -> ModelAssert.assertThat(query(field(RDFS.LABEL, filter(stem("Gerard B"))))).isIsomorphicTo(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?item.\n"
	//						+"\t?item rdfs:label ?label.\n"
	//						+"\t \n"
	//						+"} where { \n"
	//						+"\n"
	//						+"\t?item rdfs:label ?label, 'Gerard Bondur'^^xsd:string\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//}
	//
	//@Nested final class SetConstraints {
	//
	//	@Test void testMinCount() {
	//		exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> query(field(term
	//		("employee"), filter(minCount(3))))));
	//	}
	//
	//	@Test void testMaxCount() {
	//		exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> query(field(term
	//		("employee"), filter(maxCount(3))))));
	//	}
	//
	//
	//	@Test void testAllDirect() {
	//		exec(() -> ModelAssert.assertThat(query(field(term("employee"), filter(all(
	//				item("employees/1002"),
	//				item("employees/1056")
	//		))))).isIsomorphicTo(graph(
	//
	//				"construct { \n"
	//						+"\n"
	//						+"\t<> ldp:contains ?item.\n"
	//						+"\t?item :employee ?employee.\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\t?item :employee ?employee, <employees/1002>, <employees/1056>.\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testAllInverse() {
	//		exec(() -> {
	//			ModelAssert.assertThat(query(field(inverse(term("office")), filter(all(
	//					item("employees/1002"),
	//					item("employees/1056")
	//			))))).isIsomorphicTo(graph(
	//
	//					"construct {\n"
	//							+"\n"
	//							+"\t<> ldp:contains ?office.\n"
	//							+"\t?employee :office ?office.\n"
	//							+"\n"
	//							+"} where {\n"
	//							+"\n"
	//							+"\t?office ^:office ?employee, <employees/1002>, <employees/1056>.\n"
	//							+"\n"
	//							+"}"
	//
	//			));
	//		});
	//	}
	//
	//	@Test void testAllRoot() {
	//		exec(() -> ModelAssert.assertThat(query(and(
	//				filter(all(item("employees/1002"), item("employees/1056"))),
	//				field(RDF.TYPE)
	//		))).isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?employee.\n"
	//						+"\t?employee a ?type\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\tvalues ?employee { <employees/1002> <employees/1056> }\n"
	//						+"\n"
	//						+"\t?employee a ?type\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testAllSingleton() {
	//		exec(() -> {
	//			ModelAssert.assertThat(query(field(term("employee"), filter(all(item("employees/1002"))))))
	//			.isIsomorphicTo(graph(
	//
	//					"construct {\n"
	//							+"\n"
	//							+"\t<> ldp:contains ?office.\n"
	//							+"\t?office :employee ?employee.\n"
	//							+"\n"
	//							+"} where {\n"
	//							+"\n"
	//							+"\t?office :employee ?employee, <employees/1002>\n"
	//							+"\n"
	//							+"}"
	//
	//			));
	//		});
	//	}
	//
	//
	//	@Test void testAny() {
	//		exec(() -> ModelAssert.assertThat(query(field(term("employee"), filter(any(
	//				item("employees/1002"), item("employees/1056")
	//		))))).isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?office.\n"
	//						+"\t?office :employee ?employee.\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\t?office :employee ?employee, ?value filter (?value in (<employees/1002>, "
	//						+"<employees/1056>))\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testAnySingleton() {
	//		exec(() -> ModelAssert.assertThat(query(field(term("employee"), filter(any(
	//				item("employees/1002")
	//		))))).isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?office.\n"
	//						+"\t?office :employee ?employee.\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\t?office :employee ?employee, <employees/1002>\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testAnyRoot() {
	//		exec(() -> ModelAssert.assertThat(query(and(
	//				filter(any(item("employees/1002"), item("employees/1056"))),
	//				field(RDFS.LABEL)
	//		))).isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?employee.\n"
	//						+"\t?employee rdfs:label ?label.\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\tvalues ?employee {\n"
	//						+"\t\t<employees/1002>\n"
	//						+"\t\t<employees/1056>\n"
	//						+"\t}\n"
	//						+"\n"
	//						+"\t?employee rdfs:label ?label\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//
	//	@Test void testLocalized() {
	//		exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> query(field(term
	//		("employee"), filter(localized())))));
	//	}
	//
	//}
	//
	//@Nested final class StructuralConstraints {
	//
	//	@Test void testField() {
	//		exec(() -> ModelAssert.assertThat(query(and(
	//
	//				filter(field(term("country"))),
	//				field(term("country"))
	//
	//		))).isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?item.\n"
	//						+"\t?item :country ?country.\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\t?item :country ?country.\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//
	//	@Test void testLinkClassFilters() {
	//		exec(() -> ModelAssert.assertThat(query(
	//
	//				filter(link(OWL.SAMEAS, clazz(term("Office"))))
	//
	//		)).isIsomorphicTo(graph(
	//
	//				"construct { <> ldp:contains ?item } where { ?item owl:sameAs/a :Office }"
	//
	//		)));
	//	}
	//
	//	@Test void testLinkFieldFilters() {
	//		exec(() -> ModelAssert.assertThat(query(
	//
	//				link(OWL.SAMEAS, filter(field(RDF.TYPE, term("Office"))))
	//
	//		)).isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?alias."
	//						+"\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\t?alias owl:sameAs/a :Office\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testLinkFieldPatterns() {
	//		exec(() -> ModelAssert.assertThat(query(and(
	//
	//				filter(clazz(term("Alias"))),
	//
	//				link(OWL.SAMEAS, field(RDFS.LABEL))
	//
	//		))).isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?alias. ?alias rdfs:label ?label.\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\t?alias a :Alias; owl:sameAs/rdfs:label ?label"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testLinkFilteringOnLinkedField() {
	//		exec(() -> ModelAssert.assertThat(query(and(
	//
	//				filter(clazz(term("Alias"))),
	//
	//				link(OWL.SAMEAS, field(RDFS.LABEL)),
	//
	//				filter(link(OWL.SAMEAS, field(RDFS.LABEL, like("USA"))))
	//
	//		))).isIsomorphicTo(graph(
	//
	//				"construct { <> ldp:contains ?item. ?item rdfs:label ?label } where {"
	//						+" ?item owl:sameAs [a :Office; rdfs:label ?label]."
	//						+" filter(contains(?label, '(USA)'))"
	//						+"}"
	//
	//		)));
	//	}
	//
	//}
	//
	//@Nested final class LogicalConstraints {
	//
	//	private Shape always(final Shape... shapes) { return mode(Filter, Convey).then(shapes); }
	//
	//
	//	@Test void testGuard() { // reject partially redacted shapes
	//		exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
	//				query(guard("axis", RDF.NIL))
	//		));
	//	}
	//
	//	@Test void testWhen() { // reject conditional shapes
	//		exec(() -> assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
	//
	//				query(when(guard("axis", RDF.NIL), field(RDF.VALUE)))
	//
	//		));
	//	}
	//
	//	@Test void testAnd() {
	//		exec(() -> ModelAssert.assertThat(query(and(
	//				always(field(term("country"))),
	//				always(field(term("city")))
	//		))).isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?item.\n"
	//						+"\t?item :country ?country; :city ?city.\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\t?item :country ?country; :city ?city.\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//	@Test void testOr() {
	//		exec(() -> ModelAssert.assertThat(query(and(field(RDF.TYPE), filter(or(
	//				clazz(term("Office")),
	//				clazz(term("Employee"))
	//		))))).isIsomorphicTo(graph(
	//
	//				"construct {\n"
	//						+"\n"
	//						+"\t<> ldp:contains ?item.\n"
	//						+"\t?item a ?type.\n"
	//						+"\n"
	//						+"} where {\n"
	//						+"\n"
	//						+"\tvalues ?type {\n"
	//						+"\t\t:Office\n"
	//						+"\t\t:Employee\n"
	//						+"\t}\n"
	//						+"\n"
	//						+"\t?item a ?type.\n"
	//						+"\n"
	//						+"}"
	//
	//		)));
	//	}
	//
	//}
	//
	//
	//@Nested final class Path {
	//
	//	@Test void testRoot() {
	//		assertThat(path(
	//
	//				and(all(x), field(p)),
	//
	//				emptyList()
	//
	//		)).isEqualTo(
	//
	//				and()
	//
	//		);
	//	}
	//
	//	@Test void testShallow() {
	//		assertThat(path(
	//
	//				and(all(x), field(p), field(q)),
	//
	//				singletonList(p)
	//
	//		)).isEqualTo(
	//
	//				field(p)
	//
	//		);
	//	}
	//
	//	@Test void testDeep() {
	//		assertThat(path(
	//
	//				and(all(x), field(p, field(q, all(y))), field(r)),
	//
	//				asList(p, q)
	//
	//		)).isEqualTo(
	//
	//				field(p, field(q))
	//
	//		);
	//	}
	//
	//
	//	@Test void testRootLink() {
	//		assertThat(path(
	//
	//				link(OWL.SAMEAS, all(x)),
	//
	//				emptyList()
	//
	//		)).isEqualTo(
	//
	//				link(OWL.SAMEAS)
	//
	//		);
	//	}
	//
	//	@Test void testShallowLink() {
	//		assertThat(path(
	//
	//				field(p, link(OWL.SAMEAS, field(q))),
	//
	//				singletonList(p)
	//
	//		)).isEqualTo(
	//
	//				field(p, link(OWL.SAMEAS))
	//
	//		);
	//	}
	//
	//	@Test void testDeepLink() {
	//		assertThat(path(
	//
	//				field(p, field(q, link(OWL.SAMEAS))),
	//
	//				asList(p, q)
	//
	//		)).isEqualTo(
	//
	//				field(p, field(q, link(OWL.SAMEAS)))
	//
	//		);
	//	}
	//
	//}
	//
	//@Nested final class Hook {
	//
	//	@Test void testRoot() {
	//		assertThat(hook(
	//
	//				and(all(x), field("p", p)),
	//
	//				emptyList()
	//
	//		)).isEqualTo(root);
	//	}
	//
	//	@Test void testShallow() {
	//		assertThat(hook(
	//
	//				and(all(x), field("p", p), field("q", q)),
	//
	//				singletonList(p)
	//
	//		)).isEqualTo("p");
	//	}
	//
	//	@Test void testDeep() {
	//		assertThat(hook(
	//
	//				and(all(x), field("p", p, field("q", q, all(y))), field("r", r)),
	//
	//				asList(p, q)
	//
	//		)).isEqualTo("q");
	//	}
	//
	//
	//	@Test void testRootDirectLink() {
	//		assertThat(hook(
	//
	//				link(OWL.SAMEAS, all(x)),
	//
	//				emptyList()
	//
	//		)).isEqualTo(value(root));
	//	}
	//
	//	@Test void testShallowDirectLink() {
	//		assertThat(hook(
	//
	//				field("p", p, link(OWL.SAMEAS, field("q", q))),
	//
	//				singletonList(p)
	//
	//		)).isEqualTo(value("p"));
	//	}
	//
	//	@Test void testDeepDirectLink() {
	//		assertThat(hook(
	//
	//				field(p, field("q", q, link(OWL.SAMEAS))),
	//
	//				asList(p, q)
	//
	//		)).isEqualTo(value("q"));
	//	}
	//
	//
	//	@Test void testRootInverseLink() {
	//		assertThat(hook(
	//
	//				link(inverse(OWL.SAMEAS), all(x)),
	//
	//				emptyList()
	//
	//		)).isEqualTo(alias(root));
	//	}
	//
	//	@Test void testShallowInverseLink() {
	//		assertThat(hook(
	//
	//				field("p", p, link(inverse(OWL.SAMEAS), field("q", q))),
	//
	//				singletonList(p)
	//
	//		)).isEqualTo(alias("p"));
	//	}
	//
	//	@Test void testDeepInverseLink() {
	//		assertThat(hook(
	//
	//				field(p, field("q", q, link(inverse(OWL.SAMEAS)))),
	//
	//				asList(p, q)
	//
	//		)).isEqualTo(alias("q"));
	//	}
	//
	//}

}
