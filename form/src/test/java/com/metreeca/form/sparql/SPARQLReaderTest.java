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

package com.metreeca.form.sparql;

import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Count;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import static com.metreeca.form.Query.decreasing;
import static com.metreeca.form.Query.increasing;
import static com.metreeca.form.Shape.filter;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.shapes.Test.test;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shapes.Virtual.virtual;
import static com.metreeca.form.things.Lists.concat;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.ValuesTest.construct;
import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.form.things.ValuesTest.term;

import static java.util.stream.Collectors.toSet;


final class SPARQLReaderTest {

	private final Supplier<RepositoryConnection> sandbox=ValuesTest.sandbox(ValuesTest.large());


	//// Edges /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testEdgesEmptyShape() {

		final Map<Resource, Collection<Statement>> matches=edges(and());

		Assertions.assertThat(matches.isEmpty()).as("empty focus").isTrue();

	}

	@Test void testEdgesEmptyResultSet() {

		final Map<Resource, Collection<Statement>> matches=edges(trait(RDF.TYPE, all(RDF.NIL)));

		Assertions.assertThat(matches.isEmpty()).as("empty focus").isTrue();

	}

	@Test void testEdgesEmptyProjection() {

		final Map<Resource, Collection<Statement>> matches=edges(clazz(term("Product")));

		Assertions.assertThat((Object)focus(

				"select * where { ?product a :Product }"

		)).as("matching focus").isEqualTo(matches.keySet());

		Assertions.assertThat(model(matches).isEmpty()).as("empty model").isTrue();

	}

	@Test void testEdgesMatching() {

		final Map<Resource, Collection<Statement>> matches=edges(trait(RDF.TYPE, all(term("Product"))));

		Assertions.assertThat((Object)focus(

				"select * where { ?product a :Product }"

		)).as("matching focus").isEqualTo(set(matches.keySet()));

		assertThat(model(

				"construct where { ?product a :Product }"

		)).as("matching model").isIsomorphicTo(model(matches));

	}

	@Test void testEdgesSorting() {

		final String query="construct { ?product a :Product }"
				+" where { ?product a :Product; rdfs:label ?label; :line ?line }";

		final Shape shape=trait(RDF.TYPE, all(term("Product")));

		// convert to lists to assert ordering

		Assertions.assertThat(list(model(query+" order by ?product"))).as("default (on value)").isEqualTo(list(model(edges(shape))));

		Assertions.assertThat(list(model(query+" order by ?label"))).as("custom increasing").isEqualTo(list(model(edges(shape, increasing(Step.step(RDFS.LABEL))))));

		Assertions.assertThat(list(model(query+" order by desc(?label)"))).as("custom decreasing").isEqualTo(list(model(edges(shape, decreasing(Step.step(RDFS.LABEL))))));

		Assertions.assertThat(list(model(query+" order by ?line ?label"))).as("custom combined").isEqualTo(list(model(edges(shape, increasing(Step.step(term("line"))), increasing(Step.step(RDFS.LABEL))))));

		Assertions.assertThat(list(model(query+" order by desc(?product)"))).as("custom on root").isEqualTo(list(model(edges(shape, decreasing()))));

	}


	//// Stats /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testStatsEmptyResultSet() {

		final Map<Resource, Collection<Statement>> matches=stats(trait(RDF.TYPE, all(RDF.NIL)));

		Assertions.assertThat(set(Form.meta)).as("meta focus").isEqualTo(matches.keySet());

		assertThat(model("prefix spec: <"+Form.Namespace+"> construct { spec:meta spec:count 0 } where {}")).isIsomorphicTo(model(matches));
	}

	@Test void testStatsEmptyProjection() {

		final Map<Resource, Collection<Statement>> matches=stats(clazz(term("Product")));

		assertThat(model("prefix spec: <"+Form.Namespace+">\n"
				+"\n"
				+"construct { \n"
				+"\n"
				+"\tspec:meta \n"
				+"\t\tspec:count ?count; spec:min ?min; spec:max ?max;\n"
				+"\t\tspec:stats rdfs:Resource.\n"
				+"\t\n"
				+"\trdfs:Resource \n"
				+"\t\tspec:count ?count; spec:min ?min; spec:max ?max.\n"
				+"\n"
				+"} where {\n"
				+"\n"
				+"\tselect (count(?p) as ?count) (min(?p) as ?min) (max(?p) as ?max) {\n"
				+"\n"
				+"\t\t?p a :Product\n"
				+"\n"
				+"}\n"
				+"\n"
				+"}")).isIsomorphicTo(model(matches));

	}

	@Test void testStatsRootConstraints() {

		final Map<Resource, Collection<Statement>> matches=stats(all(item("employees/1370")), Step.step(term("account")));

		assertThat(model("prefix spec: <"+Form.Namespace+">\n"
				+"\n"
				+"construct { \n"
				+"\n"
				+"\tspec:meta \n"
				+"\t\tspec:count ?count; spec:min ?min; spec:max ?max;\n"
				+"\t\tspec:stats rdfs:Resource.\n"
				+"\t\n"
				+"\trdfs:Resource \n"
				+"\t\tspec:count ?count; spec:min ?min; spec:max ?max.\n"
				+"\n"
				+"} where {\n"
				+"\n"
				+"\tselect (count(?account) as ?count) (min(?account) as ?min) (max(?account) as ?max) {\n"
				+"\n"
				+"\t\t<employees/1370> :account ?account\n"
				+"\n"
				+"\t}\n"
				+"\n"
				+"}")).isIsomorphicTo(model(matches));

	}


	//// Items /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testItemsEmptyResultSet() {

		final Map<Resource, Collection<Statement>> matches=items(trait(RDF.TYPE, all(RDF.NIL)));

		Assertions.assertThat(set(Form.meta)).isEqualTo(matches.keySet());

		assertThat(model("construct {} where {}")).isIsomorphicTo(model(matches));
	}

	@Test void testItemsEmptyProjection() {

		final Map<Resource, Collection<Statement>> matches=items(clazz(term("Product")));

		assertThat(model("prefix spec: <"+Form.Namespace+">\n"
				+"\n"
				+"construct { \n"
				+"\n"
				+"\tspec:meta spec:items [\n"
				+"\t\tspec:value ?product;\n"
				+"\t\tspec:count 1\n"
				+"\t].\n"
				+"\t\n"
				+"\t?product rdfs:label ?label.\n"
				+"\n"
				+"} where {\n"
				+"\n"
				+"\t?product a :Product; rdfs:label ?label\n"
				+"\n"
				+"}")).isIsomorphicTo(model(matches));

	}

	@Test void testItemsRootConstraints() {

		final Map<Resource, Collection<Statement>> matches=items(all(item("employees/1370")), Step.step(term("account")));

		assertThat(model("prefix spec: <"+Form.Namespace+">\n"
				+"\n"
				+"construct { \n"
				+"\n"
				+"\tspec:meta spec:items [\n"
				+"\t\tspec:value ?account;\n"
				+"\t\tspec:count 1\n"
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
				+"}")).isIsomorphicTo(model(matches));

	}


	//// Constraints ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testClassConstraint() {
		assertThat(model("construct where { ?product a :Product }")).isIsomorphicTo(model(edges(and(
				clazz(term("Product")),
				trait(RDF.TYPE)
		))));
	}

	@Test void testDirectUniversalConstraint() {
		assertThat(model("construct { ?root :product ?product } where {\n"
				+"\t?root :product ?product, <products/S10_2016>, <products/S24_2022>\n"
				+"}")).isIsomorphicTo(model(edges(trait(
				term("product"),
				all(item("products/S10_2016"), item("products/S24_2022"))
		))));
	}

	@Test void testInverseUniversalConstraint() {
		assertThat(model("construct { ?product :customer ?customer } where {\n"
				+"\t?customer ^:customer ?product, <products/S10_2016>, <products/S24_2022>.\n"
				+"}")).isIsomorphicTo(model(edges(trait(
				Step.step(term("customer"), true),
				all(item("products/S10_2016"), item("products/S24_2022"))
		))));
	}

	@Test void testRootUniversalConstraint() {
		assertThat(model("construct { ?product a ?type } where {\n"
				+"\n"
				+"\tvalues ?product { <products/S18_2248> <products/S24_3969> }\n"
				+"\t\n"
				+"\t?product a ?type\n"
				+"\t\n"
				+"}")).isIsomorphicTo(model(edges(and(
				all(item("products/S18_2248"), item("products/S24_3969")),
				trait(RDF.TYPE)
		))));
	}

	@Test void testSingletonUniversalConstraint() {
		assertThat(model("construct { ?customer :product ?product } where {\n"
				+"\t?customer :product ?product, <products/S10_2016>\n"
				+"}")).isIsomorphicTo(model(edges(trait(term("product"), all(item("products/S10_2016"))))));
	}

	@Test void testExistentialConstraint() {
		assertThat(model("construct { ?item :product ?product } where {\n"
				+"\t?item :product ?product, ?value filter (?value in (<products/S18_2248>, <products/S24_3969>))\n"
				+"}")).isIsomorphicTo(model(edges(trait(term("product"), any(item("products/S18_2248"), item("products/S24_3969"))))));
	}

	@Test void testSingletonExistentialConstraint() {
		assertThat(model("construct where { <products/S18_2248> rdfs:label ?label }")).isIsomorphicTo(model(edges(and(
				any(item("products/S18_2248")),
				trait(RDFS.LABEL)
		))));
	}

	@Test void testMinExclusiveConstraint() { // 100.17 is the exact sell price of 'The Titanic'
		assertThat(model("construct { \n"
				+"\n"
				+"\t?product :sell ?sell.\n"
				+"\t \n"
				+"} where { \n"
				+"\n"
				+"\t?product :sell ?sell filter (?sell > 100.17)\n"
				+"\t\n"
				+"}")).isIsomorphicTo(model(edges(trait(term("sell"), MinExclusive.minExclusive(Values.literal(BigDecimal.valueOf(100.17)))))));
	}

	@Test void testMaxExclusiveConstraint() { // 100.17 is the exact sell price of 'The Titanic'
		assertThat(model("construct { \n"
				+"\n"
				+"\t?product :sell ?sell.\n"
				+"\t \n"
				+"} where { \n"
				+"\n"
				+"\t?product :sell ?sell filter (?sell < 100.17)\n"
				+"\t\n"
				+"}")).isIsomorphicTo(model(edges(trait(term("sell"), maxExclusive(Values.literal(BigDecimal.valueOf(100.17)))))));
	}

	@Test void testMinInclusiveConstraint() {
		assertThat(model("construct { \n"
				+"\n"
				+"\t?product :sell ?sell.\n"
				+"\t \n"
				+"} where { \n"
				+"\n"
				+"\t?product :sell ?sell filter (?sell >= 100)\n"
				+"\t\n"
				+"}")).isIsomorphicTo(model(edges(trait(term("sell"), MinInclusive.minInclusive(Values.literal(BigDecimal.valueOf(100)))))));
	}

	@Test void testMaxInclusiveConstraint() {
		assertThat(model("construct { \n"
				+"\n"
				+"\t?product :sell ?sell.\n"
				+"\t \n"
				+"} where { \n"
				+"\n"
				+"\t?product :sell ?sell filter (?sell <= 100)\n"
				+"\t\n"
				+"}")).isIsomorphicTo(model(edges(trait(term("sell"), maxInclusive(Values.literal(BigDecimal.valueOf(100)))))));
	}

	@Test void testPattern() {
		assertThat(model("construct { \n"
				+"\n"
				+"\t?item rdfs:label ?label\n"
				+"\t \n"
				+"} where { \n"
				+"\n"
				+"\t?item rdfs:label ?label filter regex(?label, '\\\\bferrari\\\\b', 'i')\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(trait(RDFS.LABEL, pattern("\\bferrari\\b", "i")))));
	}

	@Test void testLike() {
		assertThat(model("construct { \n"
				+"\n"
				+"\t?item rdfs:label ?label\n"
				+"\t \n"
				+"} where { \n"
				+"\n"
				+"\t?item rdfs:label ?label filter regex(?label, '\\\\bromeo\\\\b', 'i')\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(trait(RDFS.LABEL, like("alf ro")))));
	}

	@Test void testMinLength() {
		assertThat(model("construct { \n"
				+"\n"
				+"\t?item birt:sell ?sell\n"
				+"\t \n"
				+"} where { \n"
				+"\n"
				+"\t?item birt:sell ?sell filter (strlen(str(?sell)) >= 5)\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(trait(term("sell"), MinLength.minLength(5)))));
	}

	@Test void testMaxLength() {
		assertThat(model("construct { \n"
				+"\n"
				+"\t?item birt:sell ?sell\n"
				+"\t \n"
				+"} where { \n"
				+"\n"
				+"\t?item birt:sell ?sell filter (strlen(str(?sell)) <= 5)\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(trait(term("sell"), MaxLength.maxLength(5)))));
	}


	//// Derivates /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDerivateEdge() {
		assertThat(model("construct {\n"
				+"\n"
				+"\t?product a :Product; :price ?price.\n"
				+"\n"
				+"} where {\n"
				+"\n"
				+"\t?product a :Product; :sell ?price.\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(and(
				trait(RDF.TYPE, all(term("Product"))),
				virtual(trait(term("price")), Step.step(term("sell")))
		))));
	}

	// !!! nested expressions

	@Disabled @Test void testDerivateInternalFiltering() {
		assertThat(model("construct {\n"
				+"\n"
				+"\t?product a :Product; :price ?price.\n"
				+"\n"
				+"} where {\n"
				+"\n"
				+"\t?product a :Product; :sell ?price filter ( ?price >= 200 )\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(and(
				trait(RDF.TYPE, all(term("Product"))),
				virtual(
						trait(term("price"), MinInclusive.minInclusive(Values.literal(Values.integer(200)))),
						Step.step(term("sell"))
				)
		))));
	}

	@Disabled @Test void testDerivateExternalFiltering() {
		assertThat(model("construct {\n"
				+"\n"
				+"\t?product a :Product; :price ?price.\n"
				+"\n"
				+"} where {\n"
				+"\n"
				+"\t?product a :Product; :sell ?price filter ( ?price >= 200 )\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(and(
				trait(RDF.TYPE, all(term("Product"))),
				virtual(trait(term("price")), Step.step(term("sell"))),
				trait(term("price"), MinInclusive.minInclusive(Values.literal(Values.integer(200))))
		))));
	}


	@Disabled @Test void testDerivateSorting() {
		// convert to lists to assert ordering
		Assertions.assertThat(list(model("construct {\n"
				+"\n"
				+"\t?product a :Product; :price ?price.\n"
				+"\n"
				+"} where {\n"
				+"\n"
				+"\t?product a :Product; :sell ?price\n"
				+"\n"
				+"} order by ?price"))).isEqualTo(list(model(edges(and(

				trait(RDF.TYPE, all(term("Product"))),
				virtual(trait(term("price")), Step.step(term("sell")))

		))), increasing(Step.step(term("price")))));
	}

	@Disabled @Test void testDerivateStats() {
		assertThat(model(stats(trait(RDF.TYPE), Step.step(RDF.TYPE)))).isIsomorphicTo(model(stats(virtual(trait(RDF.VALUE), Step.step(RDF.TYPE)), Step.step(RDF.VALUE))));
	}

	@Disabled @Test void testDerivateItems() {
		assertThat(model(items(trait(RDF.TYPE), Step.step(RDF.TYPE)))).isIsomorphicTo(model(items(virtual(trait(RDF.VALUE), Step.step(RDF.TYPE)), Step.step(RDF.VALUE))));
	}


	//// Aggregates ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testAggregateCount() {
		// subordinates may be 0
		assertThat(model("construct {\n"
				+"\n"
				+"\t?employee a :Employee; :subordinates ?subordinates.\n" // subordinates may be 0
				+"\n"
				+"} where {\n"
				+"\n"
				+"\t{ select ?employee (count(distinct ?subordinate) as ?subordinates) { \n"
				+"\t\n"
				+"\t\t?employee a :Employee optional { ?employee :subordinate ?subordinate }\n"
				+"\n"
				+"\t} group by ?employee }\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(and(
				trait(RDF.TYPE, all(term("Employee"))),
				virtual(trait(term("subordinates")), Count.count(Step.step(term("subordinate"))))
		))));
	}

	@Test void testAggregateCountOnSingleton() {
		assertThat(model("construct {\n"
				+"\n"
				+"\t<product-lines/ships> :size ?size.\n"
				+"\n"
				+"} where {\n"
				+"\n"
				+"\t{ select (count(distinct ?product) as ?size) { \n"
				+"\t\n"
				+"\t\t<product-lines/ships> :product ?product. \n"
				+"\t\t\n"
				+"\t} }\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(and(
				all(item("product-lines/ships")),
				virtual(trait(term("size")), Count.count(Step.step(term("product"))))
		))));
	}

	// !!! sorting (($) project and reuse aggregates computed for filtering/sorting)
	// !!! grouping
	// !!! filtering
	// !!! stats/items
	// !!! nested expressions


	//// Layout ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testUseIndependentPatternsAndFilters() {
		assertThat(model("construct {\n"
				+"\n"
				+"\t?office :employee ?employee\n"
				+"\t\n"
				+"} where {\n"
				+"\n"
				+"\t?office :employee ?employee, ?x filter (?x in (<employees/1002>, <employees/1188>))\n"
				+"\n"
				+"}")).isIsomorphicTo(model(edges(and(
				trait(term("employee")),
				test(filter(), trait(term("employee"), any(item("employees/1002"), item("employees/1188"))))
		))));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<Resource, Collection<Statement>> edges(final Shape shape, final Query.Order... orders) {
		return process(new Edges(shape, list(orders), 0, 0));
	}

	private Map<Resource, Collection<Statement>> stats(final Shape shape, final Step... path) {
		return process(new Stats(shape, list(path)));
	}

	private Map<Resource, Collection<Statement>> items(final Shape shape, final Step... path) {
		return process(new Items(shape, list(path)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> model(final Map<Resource, Collection<Statement>> matches) {
		return matches.values().stream().reduce(list(), (x, y) -> concat(x, y));
	}

	private Map<Resource, Collection<Statement>> process(final Query query) {
		try (final RepositoryConnection connection=sandbox.get()) {
			return new SPARQLReader(connection).process(query);
		}
	}

	private Set<Value> focus(final String query) {
		try (final RepositoryConnection connection=sandbox.get()) {
			return ValuesTest.select(connection, query)
					.stream()
					.flatMap(tuple -> tuple.values().stream())
					.collect(toSet());
		}
	}

	private Collection<Statement> model(final String query) {
		try (final RepositoryConnection connection=sandbox.get()) {
			return construct(connection, query);
		}
	}

}
