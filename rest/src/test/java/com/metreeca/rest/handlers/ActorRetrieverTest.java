/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.handlers;

import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.things.Values;
import com.metreeca.tray.rdf.Graph;
import com.metreeca.tray.rdf.GraphTest;
import com.metreeca.tray.rdf.graphs.Stardog;
import com.metreeca.tray.sys.Trace;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;

import static com.metreeca.form.Order.decreasing;
import static com.metreeca.form.Order.increasing;
import static com.metreeca.form.Shape.filter;
import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.queries.Items.items;
import static com.metreeca.form.queries.Stats.stats;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Guard.guard;
import static com.metreeca.form.shapes.In.in;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.MaxLength.maxLength;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.MinExclusive.minExclusive;
import static com.metreeca.form.shapes.MinInclusive.minInclusive;
import static com.metreeca.form.shapes.MinLength.minLength;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.*;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.form.things.ValuesTest.term;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.rdf.GraphTest.tuples;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import static java.util.stream.Collectors.toList;


final class ActorRetrieverTest extends ActorProcessorTest {

	private Collection<Statement> query(final Query query) {
		return tool(Graph.Factory).query(connection -> {
			return query

					.map(new ActorRetriever(tool(Trace.Factory), connection, Form.root, true))

					.stream()

					// ;(virtuoso) counts reported as xsd:int

					.map(statement -> type(statement.getObject()).equals(XMLSchema.INT) ? statement(
							statement.getSubject(),
							statement.getPredicate(),
							literal(integer(statement.getObject()).orElse(BigInteger.ZERO)),
							statement.getContext()
					) : statement)

					.collect(toList());
		});
	}

	private List<Statement> graph(final String sparql) {
		return GraphTest.graph(sparql)

				.stream()

				// ;(stardog) statement from default context explicitly tagged

				.map(statement -> Stardog.Default.equals(statement.getContext()) ? statement(
						statement.getSubject(),
						statement.getPredicate(),
						statement.getObject()
				) : statement)

				.collect(toList());
	}


	//// Queries ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Edges {

		@Test void testEmptyShape() {
			exec(() -> assertThat(query(

					edges(and())

			)).isEmpty());
		}

		@Test void testEmptyResultSet() {
			exec(() -> assertThat(query(

					edges(field(RDF.TYPE, all(RDF.NIL)))

			)).isEmpty());
		}

		@Test void testEmptyProjection() {
			exec(() -> assertThat(query(

					edges(any(item("employees/1002"), item("employees/1056")))

			)).isIsomorphicTo(graph( // empty template => symmetric+labelled concise bounded description

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t\n"
							+"\t?employee ?d ?r.\n"
							+"\t?r ?i ?employee.\n"
							+"\n"
							+"\t?r rdf:type ?t.\n"
							+"\t?r rdfs:label ?l.\n"
							+"\t?r rdfs:comment ?c.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tvalues ?employee {\n"
							+"\t\t<employees/1002>\n"
							+"\t\t<employees/1056>\n"
							+"\t}\n"
							+"\n"
							+"\t{ ?employee ?d ?r } union { ?r ?i ?employee }\n"
							+"\n"
							+"\toptional { ?r rdf:type ?t }\n"
							+"\toptional { ?r rdfs:label ?l }\n"
							+"\toptional { ?r rdfs:comment ?c }\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testMatching() {
			exec(() -> assertThat(query(

					edges(field(RDF.TYPE, all(term("Employee"))))

			)).isIsomorphicTo(graph(

					"construct { form:root ldp:contains ?employee. ?employee a :Employee }"
							+" where { ?employee a :Employee }"

			)));
		}

		@Test void testSorting() {
			exec(() -> {

				final String query="select ?employee "
						+" where { ?employee a :Employee; rdfs:label ?label; :office ?office }";

				final Shape shape=filter().then(clazz(term("Employee")));

				final Function<Query, List<Value>> actual=edges -> query(edges)
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


				Assertions.assertThat(actual.apply(edges(shape)))
						.as("default (on value)")
						.containsExactlyElementsOf(expected.apply(query+" order by ?employee"));

				Assertions.assertThat(actual.apply(edges(shape, increasing(RDFS.LABEL))))
						.as("custom increasing")
						.containsExactlyElementsOf(expected.apply(query+" order by ?label"));

				Assertions.assertThat(actual.apply(edges(shape, decreasing(RDFS.LABEL))))
						.as("custom decreasing")
						.containsExactlyElementsOf(expected.apply(query+" order by desc(?label)"));

				Assertions.assertThat(actual.apply(edges(shape, increasing(term("office")), increasing(RDFS.LABEL))))
						.as("custom combined")
						.containsExactlyElementsOf(expected.apply(query+" order by ?office ?label"));

				Assertions.assertThat(actual.apply(edges(shape, decreasing())))
						.as("custom on root")
						.containsExactlyElementsOf(expected.apply(query+" order by desc(?employee)"));

			});
		}

	}

	@Nested final class Stats {

		@Test void testEmptyResultSet() {
			exec(() -> assertThat(query(

					stats(field(RDF.TYPE, all(RDF.NIL)))

			)).isIsomorphicTo(decode(

					"form:root form:count 0 ."

			)));
		}

		@Test void testEmptyProjection() {
			exec(() -> assertThat(query(

					stats(clazz(term("Employee")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root form:count ?count; form:min ?min; form:max ?max;\n"
							+"\n"
							+"\t\t\tform:stats form:iri.\n"
							+"\t\t\t\n"
							+"\tform:iri form:count ?count; form:min ?min; form:max ?max.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\tselect (count(?p) as ?count) (min(?p) as ?min) (max(?p) as ?max) {\n"
							+"\n"
							+"\t\t?p a :Employee\n"
							+"\n"
							+"\t}\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testRootConstraints() {
			exec(() -> assertThat(query(

					stats(all(item("employees/1370")), term("account"))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root \n"
							+"\t\tform:count ?count; form:min ?min; form:max ?max.\n"
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

	@Nested final class Items {

		@Test void testEmptyResultSet() {
			exec(() -> assertThat(query(

					items(field(RDF.TYPE, all(RDF.NIL)))

			)).isEmpty());
		}

		@Test void testEmptyProjection() {
			exec(() -> assertThat(query(

					items(clazz(term("Employee")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root form:items [\n"
							+"\t\tform:value ?employee;\n"
							+"\t\tform:count 1\n"
							+"\t].\n"
							+"\n"
							+"\t?employee rdfs:label ?label.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?employee a :Employee; \n"
							+"\t\trdfs:label ?label.\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testRootConstraints() {
			exec(() -> assertThat(query(

					items(all(item("employees/1370")), list(term("account")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root form:items [\n"
							+"\t\tform:value ?account;\n"
							+"\t\tform:count 1\n"
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


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Annotations {

		@Test void testMeta() {
			exec(() -> assertThat(query(

					edges(meta(RDF.VALUE, RDF.NIL))

			)).as("ignore annotations")
					.isEmpty());
		}

		@Test void testGuard() {
			exec(() -> assertThatThrownBy(() ->
					query(edges(guard(RDF.VALUE, RDF.NIL)))

			).as("reject partially redacted shapes")
					.isInstanceOf(UnsupportedOperationException.class));
		}

	}

	@Nested final class TermConstraints {

		@Test void testDatatype() {
			exec(() -> {

				assertThat(query(

						edges(field(term("code"), datatype(XMLSchema.INTEGER)))

				)).isEmpty();

				assertThat(query(

						edges(field(term("code"), datatype(XMLSchema.STRING)))

				)).isIsomorphicTo(graph(

						"construct {\n"
								+"\n"
								+"\tform:root ldp:contains ?item.\n"
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

					edges(and(field(RDF.TYPE), clazz(term("Employee"))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
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


		@Test void testMinExclusiveConstraint() {
			exec(() -> assertThat(query(

					edges(field(term("seniority"), minExclusive(literal(integer(3)))))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
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

					edges(field(term("seniority"), maxExclusive(literal(integer(3)))))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
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

					edges(field(term("seniority"), minInclusive(literal(integer(3)))))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
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

					edges(field(term("seniority"), maxInclusive(literal(integer(3)))))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
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

					edges(field(term("forename"), minLength(5)))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
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

					edges(field(term("forename"), maxLength(5)))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
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

					edges(field(RDFS.LABEL, pattern("\\bgerard\\b", "i")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
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

					edges(field(RDFS.LABEL, like("ger bo")))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
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
			exec(() -> assertThatThrownBy(() -> query(

					edges(field(term("employee"), minCount(3)))

			)).isInstanceOf(UnsupportedOperationException.class));
		}

		@Test void testMaxCount() {
			exec(() -> assertThatThrownBy(() -> query(

					edges(field(term("employee"), maxCount(3)))

			)).isInstanceOf(UnsupportedOperationException.class));
		}


		@Test void testIn() {
			exec(() -> assertThatThrownBy(() -> query(

					edges(field(term("office"), in(item("employees/1621"), item("employees/1625"))))

			)).isInstanceOf(UnsupportedOperationException.class));
		}


		@Test void testAllDirect() {
			exec(() -> assertThat(query(

					edges(field(term("employee"),
							all(item("employees/1002"), item("employees/1056"))
					))

			)).isIsomorphicTo(graph(

					"construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
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
			exec(() -> assertThat(query(

					edges(field(inverse(term("office")),
							all(item("employees/1002"), item("employees/1056"))
					))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?office.\n"
							+"\t?employee :office ?office.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office ^:office ?employee, <employees/1002>, <employees/1056>.\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testAllRoot() {
			exec(() -> assertThat(query(

					edges(and(
							all(item("employees/1002"), item("employees/1056")),
							field(RDF.TYPE)
					))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
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
			exec(() -> assertThat(query(

					edges(field(term("employee"), all(item("employees/1002"))))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?office.\n"
							+"\t?office :employee ?employee.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office :employee ?employee, <employees/1002>\n"
							+"\n"
							+"}"

			)));
		}


		@Test void testAny() {
			exec(() -> assertThat(query(

					edges(field(term("employee"),
							any(item("employees/1002"), item("employees/1056")))
					)

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?office.\n"
							+"\t?office :employee ?employee.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?office :employee ?employee, ?value filter (?value in (<employees/1002>, <employees/1056>))\n"
							+"\n"
							+"}"

			)));
		}

		@Test void testAnySingleton() {
			exec(() -> assertThat(query(

					edges(field(term("employee"),
							any(item("employees/1002")))
					)

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?office.\n"
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

					edges(and(
							any(item("employees/1002"), item("employees/1056")),
							field(RDFS.LABEL)
					))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
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

	}

	@Nested final class StructuralConstraints {

		@Test void testField() {
			exec(() -> assertThat(query(

					edges(field(term("country")))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
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

		@Test void testAnd() {
			exec(() -> assertThat(query(

					edges(and(
							field(term("country")),
							field(term("city"))
					))

			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
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

					edges(and(
							field(RDF.TYPE),
							or(
									clazz(term("Office")),
									clazz(term("Employee"))
							)))


			)).isIsomorphicTo(graph(

					"construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
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

					edges(when(guard(RDF.VALUE, RDF.NIL), clazz(RDFS.LITERAL)))

			)).isInstanceOf(UnsupportedOperationException.class));
		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testUseIndependentPatternsAndFilters() {
		exec(() -> assertThat(query(

				edges(and(
						field(term("employee")),
						filter().then(field(term("employee"), any(item("employees/1002"), item("employees/1188"))))
				))

		)).isIsomorphicTo(graph(

				"construct {\n"
						+"\n"
						+"\tform:root ldp:contains ?office.\n"
						+"\t?office :employee ?employee\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?office :employee ?employee, ?x filter (?x in (<employees/1002>, <employees/1188>))\n"
						+"\n"
						+"}"

		)));
	}

}
