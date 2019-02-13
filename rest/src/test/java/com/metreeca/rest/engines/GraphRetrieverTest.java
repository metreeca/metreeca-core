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

package com.metreeca.rest.engines;

import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.Statement;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.form.Form.root;
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
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Guard.guard;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.MaxLength.maxLength;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.MinExclusive.minExclusive;
import static com.metreeca.form.shapes.MinInclusive.minInclusive;
import static com.metreeca.form.shapes.MinLength.minLength;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.decimal;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.rdf.GraphTest.graph;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.eclipse.rdf4j.model.vocabulary.RDF.NIL;
import static org.eclipse.rdf4j.model.vocabulary.RDF.TYPE;
import static org.eclipse.rdf4j.model.vocabulary.RDF.VALUE;
import static org.eclipse.rdf4j.model.vocabulary.RDFS.LABEL;


final class GraphRetrieverTest {

	private void exec(final Runnable task) {
		new Tray().exec(graph(small())).exec(task);
	}


	private Collection<Statement> query(final Query query) {
		return new GraphRetriever().retrieve(root, query);
	}

	private Collection<Statement> query(final String query) {
		return tool(Graph.Factory).query(connection -> {
			return construct(connection, query);
		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Edges {

		@Test void testEmptyShape() {
			exec(() -> assertThat(query(

					edges(and())

			)).isEmpty());
		}

		@Test void testEmptyResultSet() {
			exec(() -> assertThat(query(

					edges(field(TYPE, all(NIL)))

			)).isEmpty());
		}

		@Test void testEmptyProjection() {
			exec(() -> assertThat(query(

					edges(clazz(term("Employee")))

			)).isIsomorphicTo(query(

					"construct { form:root ldp:contains ?employee } where { ?employee a :Employee }"

			)));
		}

		@Test void testMatching() {
			exec(() -> assertThat(query(

					edges(field(TYPE, all(term("Employee"))))

			)).isIsomorphicTo(query(

					"construct { form:root ldp:contains ?employee. ?employee a :Employee } where { ?employee a :Employee }"

			)));
		}


		@Test void testSorting() {
			exec(() -> {

				final String query="construct { form:root ldp:contains ?employee }"
						+" where { ?employee a :Employee; rdfs:label ?label; :office ?office }";

				final Shape shape=filter().then(field(TYPE, all(term("Employee"))));

				// convert to lists to assert ordering

				Assertions.assertThat(list(query(edges(shape))))
						.as("default (on value)")
						.isEqualTo(list(query(query+" order by ?employee")));

				Assertions.assertThat(list(query(edges(shape, increasing(LABEL)))))
						.as("custom increasing")
						.isEqualTo(list(query(query+" order by ?label")));

				Assertions.assertThat(list(query(edges(shape, decreasing(LABEL)))))
						.as("custom decreasing")
						.isEqualTo(list(query(query+" order by desc(?label)")));

				Assertions.assertThat(list(query(edges(shape, increasing(term("office")), increasing(LABEL)))))
						.as("custom combined")
						.isEqualTo(list(query(query+" order by ?office ?label")));

				Assertions.assertThat(list(query(edges(shape, decreasing()))))
						.as("custom on root")
						.isEqualTo(list(query(query+" order by desc(?employee)")));
			});
		}

	}

	@Nested final class Stats {

		@Test void testEmptyResultSet() {
			exec(() -> {
				final Collection<Statement> matches=query(stats(field(TYPE, all(NIL))));

				assertThat(matches)
						.isIsomorphicTo(decode("form:root form:count 0 ."));
			});
		}

		@Test void testEmptyProjection() {
			exec(() -> assertThat(

					query(stats(clazz(term("Employee"))))

			).isIsomorphicTo(query(""
					+"\n"
					+"construct { \n"
					+"\n"
					+"\tform:root \n"
					+"\t\tform:count ?count; form:min ?min; form:max ?max;\n"
					+"\n"
					+"} where {\n"
					+"\n"
					+"\tselect (count(?p) as ?count) (min(?p) as ?min) (max(?p) as ?max) {\n"
					+"\n"
					+"\t\t?p a :Employee\n"
					+"\n"
					+"}\n"
					+"\n"
					+"}"
			)));
		}

		@Test void testRootConstraints() {
			exec(() -> {
				final Collection<Statement> matches=query(stats(all(item("employees/1370")), term("account")));

				assertThat(matches)
						.isIsomorphicTo(query(""
								+"\n"
								+"construct { \n"
								+"\n"
								+"\tform:root \n"
								+"\t\tform:count ?count; form:min ?min; form:max ?max;\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\tselect (count(?account) as ?count) (min(?account) as ?min) (max(?account) as ?max) {\n"
								+"\n"
								+"\t\t<employees/1370> :account ?account\n"
								+"\n"
								+"\t}\n"
								+"\n"
								+"}"));
			});
		}

	}

	@Nested final class Items {

		@Test void testEmptyResultSet() {
			exec(() -> assertThat(query(items(field(TYPE, all(NIL)), list())))
					.isEmpty());
		}

		@Test void testEmptyProjection() {
			exec(() -> {
				final Collection<Statement> matches=query(items(clazz(term("Employee")), list()));

				assertThat(matches)
						.isIsomorphicTo(query("construct { \n"
								+"\n"
								+"\tform:root form:items [\n"
								+"\t\tform:value ?employee;\n"
								+"\t\tform:count 1\n"
								+"\t].\n"
								+"\t\n"
								+"\t?employee rdfs:label ?label; rdfs:comment ?comment.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\t?employee a :Employee; \n"
								+"\t\trdfs:label ?label;\n"
								+"\t\trdfs:comment ?comment.\n"
								+"\n"
								+"}"));
			});
		}

		@Test void testRootConstraints() {
			exec(() -> {
				final Collection<Statement> matches=query(items(all(item("employees/1370")), list(term("account"))));

				assertThat(matches)
						.isIsomorphicTo(query(""
								+"\n"
								+"construct { \n"
								+"\n"
								+"\tform:root form:items [\n"
								+"\t\tform:value ?account;\n"
								+"\t\tform:count 1\n"
								+"\t].\n"
								+"\t\n"
								+"\t?account rdfs:label ?label.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\t<employees/1370> :account ?account.\n"
								+"\t\n"
								+"\t?account rdfs:label ?label.\n"
								+"\n"
								+"}"));
			});
		}

	}


	@Nested final class Annotations {

		@Test void testMeta() {
			exec(() -> assertThat(query(edges(meta(VALUE, NIL))))
					.as("ignore annotations")
					.isEmpty());
		}

		@Test void testGuard() {
			exec(() -> assertThatThrownBy(() -> query(edges(guard(VALUE, NIL))))
					.as("reject partially redacted shapes")
					.isInstanceOf(UnsupportedOperationException.class));
		}

	}

	@Nested final class TermConstraints {

		// !!! datatype

		@Test void testClazz() {
			exec(() -> assertThat(query(edges(clazz(term("Employee")))))
					.isIsomorphicTo(query("construct { form:root ldp:contains ?employee } where { ?employee a :Employee }")));
		}


		@Test void testMinExclusiveConstraint() {
			exec(() -> { // 100.17 is the exact sell price of 'The Titanic'
				assertThat(query(edges(field(term("sell"), minExclusive(literal(decimal(100.17)))))))
						.isIsomorphicTo(query("construct { \n"
								+"\n"
								+"\tform:root ldp:contains ?employee.\n"
								+"\t?employee :sell ?sell.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?employee :sell ?sell filter (?sell > 100.17)\n"
								+"\t\n"
								+"}"));
			});
		}

		@Test void testMaxExclusiveConstraint() {
			exec(() -> { // 100.17 is the exact sell price of 'The Titanic'
				assertThat(query(edges(field(term("sell"), maxExclusive(literal(decimal(100.17)))))))
						.isIsomorphicTo(query("construct { \n"
								+"\n"
								+"\tform:root ldp:contains ?employee.\n"
								+"\t?employee :sell ?sell.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?employee :sell ?sell filter (?sell < 100.17)\n"
								+"\t\n"
								+"}"));
			});
		}

		@Test void testMinInclusiveConstraint() {
			exec(() -> assertThat(query(edges(field(term("sell"), minInclusive(literal(decimal(100)))))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee :sell ?sell.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?employee :sell ?sell filter (?sell >= 100)\n"
							+"\t\n"
							+"}")));
		}

		@Test void testMaxInclusiveConstraint() {
			exec(() -> assertThat(query(edges(field(term("sell"), maxInclusive(literal(decimal(100)))))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee :sell ?sell.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?employee :sell ?sell filter (?sell <= 100)\n"
							+"\t\n"
							+"}")));
		}


		@Test void testMinLength() {
			exec(() -> assertThat(query(edges(field(term("sell"), minLength(5)))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item birt:sell ?sell\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?item birt:sell ?sell filter (strlen(str(?sell)) >= 5)\n"
							+"\n"
							+"}")));
		}

		@Test void testMaxLength() {
			exec(() -> assertThat(query(edges(field(term("sell"), maxLength(5)))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item birt:sell ?sell\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?item birt:sell ?sell filter (strlen(str(?sell)) <= 5)\n"
							+"\n"
							+"}")));
		}

		@Test void testPattern() {
			exec(() -> assertThat(query(edges(field(LABEL, pattern("\\bferrari\\b", "i")))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item rdfs:label ?label\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?item rdfs:label ?label filter regex(?label, '\\\\bferrari\\\\b', 'i')\n"
							+"\n"
							+"}")));
		}

		@Test void testLike() {
			exec(() -> assertThat(query(edges(field(LABEL, like("alf ro")))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item rdfs:label ?label\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?item rdfs:label ?label filter regex(?label, '\\\\bromeo\\\\b', 'i')\n"
							+"\n"
							+"}")));
		}

	}

	@Nested final class SetConstraints {

		// !!! in/min/maxCount

		// !!! in/min/maxCount
		@Test void testAllDirect() {
			exec(() -> assertThat(query(edges(field(term("employee"),
					all(item("employees/S10_2016"), item("employees/S24_2022"))
			))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item :employee ?employee.\n"
							+"\t\n"
							+"} where {\n"
							+"\n"
							+"\t?item :employee ?employee, <employees/S10_2016>, <employees/S24_2022>\n"
							+"\t\n"
							+"}")));
		}

		@Test void testAllInverse() {
			exec(() -> assertThat(query(edges(field(inverse(term("customer")),
					all(item("employees/S10_2016"), item("employees/S24_2022"))
			))))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?customer.\n"
							+"\t?employee :customer ?customer.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?customer ^:customer ?employee, <employees/S10_2016>, <employees/S24_2022>.\n"
							+"\t\n"
							+"}"
					)));
		}

		@Test void testAllRoot() {
			exec(() -> assertThat(query(edges(and(
					all(item("employees/1370"), item("employees/1002")),
					field(TYPE)
			))))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?employee.\n"
							+"\t?employee a ?type\n"
							+"\t\n"
							+"} where {\n"
							+"\n"
							+"\tvalues ?employee { <employees/1370> <employees/1002> }\n"
							+"\t\n"
							+"\t?employee a ?type\n"
							+"\t\n"
							+"}")));
		}

		@Test void testAllSingleton() {
			exec(() -> assertThat(query(edges(field(term("employee"), all(item("employees/S10_2016"))))))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?customer.\n"
							+"\t?customer :employee ?employee.\n"
							+"\t\n"
							+"} where {\n"
							+"\n"
							+"\t?customer :employee ?employee, <employees/S10_2016>\n"
							+"\n"
							+"}")));
		}


		@Test void testAny() {
			exec(() -> assertThat(query(edges(field(term("employee"),
					any(item("employees/S18_2248"), item("employees/S24_3969")))
			)))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item :employee ?employee.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?item :employee ?employee, ?value filter (?value in (<employees/S18_2248>, <employees/S24_3969>))\n"
							+"\t\n"
							+"}")));
		}

		@Test void testAnySingleton() {
			exec(() -> assertThat(query(edges(and(
					any(item("employees/1370")),
					field(LABEL)
			))))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains <employees/1370>.\n"
							+"\t<employees/1370> rdfs:label ?label.\n"
							+"\t\n"
							+"} where {\n"
							+"\n"
							+"\t<employees/1370> rdfs:label ?label\n"
							+"\t\n"
							+"}")));
		}

	}

	@Nested final class StructuralConstraints {

	}

	@Nested final class LogicalConstraints {

	}


	@Test void testUseIndependentPatternsAndFilters() {
		exec(() -> assertThat(query(

				edges(and(
						field(term("employee")),
						when(filter(), field(term("employee"), any(item("employees/1002"), item("employees/1188"))))
				))

		)).isIsomorphicTo(query("construct {\n"
				+"\n"
				+"\tform:root ldp:contains ?office.\n"
				+"\t?office :employee ?employee\n"
				+"\t\n"
				+"} where {\n"
				+"\n"
				+"\t?office :employee ?employee, ?x filter (?x in (<employees/1002>, <employees/1188>))\n"
				+"\n"
				+"}"
		)));
	}

}
