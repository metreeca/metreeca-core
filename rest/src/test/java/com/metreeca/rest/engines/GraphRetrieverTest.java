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
import org.eclipse.rdf4j.model.IRI;
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
		new Tray().exec(graph(large())).exec(task);
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
			exec(() -> assertThat(query(edges(and())))
					.as("empty focus")
					.isEmpty());
		}

		@Test void testEmptyResultSet() {
			exec(() -> assertThat(query(edges(field(TYPE, all(NIL)))))
					.as("empty focus")
					.isEmpty());
		}

		@Test void testEmptyProjection() {
			exec(() -> assertThat(query(edges(clazz(term("Product")))))
					.as("matching focus")
					.isIsomorphicTo(query("construct { form:root ldp:contains ?product } where { ?product a :Product }")));
		}

		@Test void testMatching() {
			new Tray().exec(() -> {
				final Collection<Statement> matches=query(edges(field(TYPE, all(term("Product")))));

				assertThat(matches)
						.as("matching focus")
						.isIsomorphicTo(query("construct { form:root ldp:contains ?product. ?product a :Product } where { ?product a :Product }"));
			});
		}

		@Test void testSorting() {
			new Tray().exec(() -> {

				final String query="construct { form:root ldp:contains ?product }"
						+" where { ?product a :Product; rdfs:label ?label; :line ?line }";

				final Shape shape=filter().then(field(TYPE, all(term("Product"))));

				// convert to lists to assert ordering

				Assertions.assertThat(list(query(query+" order by ?product")))
						.as("default (on value)")
						.isEqualTo(list(query(edges(shape))));

				Assertions.assertThat(list(query(query+" order by ?label")))
						.as("custom increasing")
						.isEqualTo(list(query(edges(shape, increasing(LABEL)))));

				Assertions.assertThat(list(query(query+" order by desc(?label)")))
						.as("custom decreasing")
						.isEqualTo(list(query(edges(shape, decreasing(LABEL)))));

				Assertions.assertThat(list(query(query+" order by ?line ?label")))
						.as("custom combined")
						.isEqualTo(list(query(edges(shape, increasing(term("line")), increasing(LABEL)))));

				Assertions.assertThat(list(query(query+" order by desc(?product)")))
						.as("custom on root")
						.isEqualTo(list(query(edges(shape, decreasing()))));
			});
		}

	}

	@Nested final class Stats {

		@Test void testEmptyResultSet() {
			new Tray().exec(() -> {
				final Collection<Statement> matches=query(stats(field(TYPE, all(NIL))));

				assertThat(matches)
						.isIsomorphicTo(decode("form:root form:count 0 ."));
			});
		}

		@Test void testEmptyProjection() {
			new Tray().exec(() -> assertThat(

					query(stats(clazz(term("Product"))))

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
					+"\t\t?p a :Product\n"
					+"\n"
					+"}\n"
					+"\n"
					+"}"
			)));
		}

		@Test void testRootConstraints() {
			new Tray().exec(() -> {
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
			new Tray().exec(() -> {
				final Collection<Statement> matches=query(items(field(TYPE, all(NIL)), list(new IRI[] {})));

				assertThat(matches)
						.isEmpty();
			});
		}

		@Test void testEmptyProjection() {
			new Tray().exec(() -> {
				final Collection<Statement> matches=query(items(clazz(term("Product")), list(new IRI[] {})));

				assertThat(matches)
						.isIsomorphicTo(query("construct { \n"
								+"\n"
								+"\tform:root form:items [\n"
								+"\t\tform:value ?product;\n"
								+"\t\tform:count 1\n"
								+"\t].\n"
								+"\t\n"
								+"\t?product rdfs:label ?label; rdfs:comment ?comment.\n"
								+"\n"
								+"} where {\n"
								+"\n"
								+"\t?product a :Product; \n"
								+"\t\trdfs:label ?label;\n"
								+"\t\trdfs:comment ?comment.\n"
								+"\n"
								+"}"));
			});
		}

		@Test void testRootConstraints() {
			new Tray().exec(() -> {
				final Collection<Statement> matches=query(items(all(item("employees/1370")), list(new IRI[] {
						term("account")
				})));

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

		// !!! datatype
		@Test void testClazz() {
			exec(() -> assertThat(query(edges(clazz(term("Product")))))
					.isIsomorphicTo(query("construct { form:root ldp:contains ?product } where { ?product a :Product }")));
		}


		@Test void testMinExclusiveConstraint() {
			exec(() -> { // 100.17 is the exact sell price of 'The Titanic'
				assertThat(query(edges(field(term("sell"), minExclusive(literal(decimal(100.17)))))))
						.isIsomorphicTo(query("construct { \n"
								+"\n"
								+"\tform:root ldp:contains ?product.\n"
								+"\t?product :sell ?sell.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?product :sell ?sell filter (?sell > 100.17)\n"
								+"\t\n"
								+"}"));
			});
		}

		@Test void testMaxExclusiveConstraint() {
			exec(() -> { // 100.17 is the exact sell price of 'The Titanic'
				assertThat(query(edges(field(term("sell"), maxExclusive(literal(decimal(100.17)))))))
						.isIsomorphicTo(query("construct { \n"
								+"\n"
								+"\tform:root ldp:contains ?product.\n"
								+"\t?product :sell ?sell.\n"
								+"\t \n"
								+"} where { \n"
								+"\n"
								+"\t?product :sell ?sell filter (?sell < 100.17)\n"
								+"\t\n"
								+"}"));
			});
		}

		@Test void testMinInclusiveConstraint() {
			exec(() -> assertThat(query(edges(field(term("sell"), minInclusive(literal(decimal(100)))))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?product.\n"
							+"\t?product :sell ?sell.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?product :sell ?sell filter (?sell >= 100)\n"
							+"\t\n"
							+"}")));
		}

		@Test void testMaxInclusiveConstraint() {
			exec(() -> assertThat(query(edges(field(term("sell"), maxInclusive(literal(decimal(100)))))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?product.\n"
							+"\t?product :sell ?sell.\n"
							+"\t \n"
							+"} where { \n"
							+"\n"
							+"\t?product :sell ?sell filter (?sell <= 100)\n"
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
			exec(() -> assertThat(query(edges(field(term("product"),
					all(item("products/S10_2016"), item("products/S24_2022"))
			))))
					.isIsomorphicTo(query("construct { \n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item :product ?product.\n"
							+"\t\n"
							+"} where {\n"
							+"\n"
							+"\t?item :product ?product, <products/S10_2016>, <products/S24_2022>\n"
							+"\t\n"
							+"}")));
		}

		@Test void testAllInverse() {
			exec(() -> assertThat(query(edges(field(inverse(term("customer")),
					all(item("products/S10_2016"), item("products/S24_2022"))
			))))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?customer.\n"
							+"\t?product :customer ?customer.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?customer ^:customer ?product, <products/S10_2016>, <products/S24_2022>.\n"
							+"\t\n"
							+"}"
					)));
		}

		@Test void testAllRoot() {
			exec(() -> assertThat(query(edges(and(
					all(item("products/S18_2248"), item("products/S24_3969")),
					field(TYPE)
			))))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?product.\n"
							+"\t?product a ?type\n"
							+"\t\n"
							+"} where {\n"
							+"\n"
							+"\tvalues ?product { <products/S18_2248> <products/S24_3969> }\n"
							+"\t\n"
							+"\t?product a ?type\n"
							+"\t\n"
							+"}")));
		}

		@Test void testAllSingleton() {
			exec(() -> assertThat(query(edges(field(term("product"), all(item("products/S10_2016"))))))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?customer.\n"
							+"\t?customer :product ?product.\n"
							+"\t\n"
							+"} where {\n"
							+"\n"
							+"\t?customer :product ?product, <products/S10_2016>\n"
							+"\n"
							+"}")));
		}


		@Test void testAny() {
			exec(() -> assertThat(query(edges(field(term("product"),
					any(item("products/S18_2248"), item("products/S24_3969")))
			)))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains ?item.\n"
							+"\t?item :product ?product.\n"
							+"\n"
							+"} where {\n"
							+"\n"
							+"\t?item :product ?product, ?value filter (?value in (<products/S18_2248>, <products/S24_3969>))\n"
							+"\t\n"
							+"}")));
		}

		@Test void testAnySingleton() {
			exec(() -> assertThat(query(edges(and(
					any(item("products/S18_2248")),
					field(LABEL)
			))))
					.isIsomorphicTo(query("construct {\n"
							+"\n"
							+"\tform:root ldp:contains <products/S18_2248>.\n"
							+"\t<products/S18_2248> rdfs:label ?label.\n"
							+"\t\n"
							+"} where {\n"
							+"\n"
							+"\t<products/S18_2248> rdfs:label ?label\n"
							+"\t\n"
							+"}")));
		}

	}

	@Nested final class StructuralConstraints {

	}

	@Nested final class LogicalConstraints {

	}


	@Test void testUseIndependentPatternsAndFilters() {
		exec(() -> assertThat(query(edges(and(
				field(term("employee")),
				when(filter(), field(term("employee"), any(item("employees/1002"), item("employees/1188"))))
		))))
				.isIsomorphicTo(query("construct {\n"
						+"\n"
						+"\tform:root ldp:contains ?office.\n"
						+"\t?office :employee ?employee\n"
						+"\t\n"
						+"} where {\n"
						+"\n"
						+"\t?office :employee ?employee, ?x filter (?x in (<employees/1002>, <employees/1188>))\n"
						+"\n"
						+"}")));
	}

}
