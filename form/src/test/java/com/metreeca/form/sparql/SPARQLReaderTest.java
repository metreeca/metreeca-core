/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Count;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.*;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.MaxLength;
import com.metreeca.form.shapes.MinLength;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;

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
import static com.metreeca.form.shifts.Step.step;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.integer;
import static com.metreeca.form.things.Values.literal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.stream.Collectors.toSet;


public class SPARQLReaderTest {

	private final Supplier<RepositoryConnection> sandbox=ValuesTest.sandbox(ValuesTest.large());


	//// Edges /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testEdgesEmptyShape() {

		final Map<Value, Collection<Statement>> matches=edges(and());

		assertTrue("empty focus", matches.isEmpty());

	}

	@Test public void testEdgesEmptyResultSet() {

		final Map<Value, Collection<Statement>> matches=edges(trait(RDF.TYPE, All.all(RDF.NIL)));

		assertTrue("empty focus", matches.isEmpty());

	}

	@Test public void testEdgesEmptyProjection() {

		final Map<Value, Collection<Statement>> matches=edges(clazz(ValuesTest.term("Product")));

		assertEquals("matching focus", focus(

				"select * where { ?product a :Product }"

		), matches.keySet());

		assertTrue("empty model", model(matches).isEmpty());

	}

	@Test public void testEdgesMatching() {

		final Map<Value, Collection<Statement>> matches=edges(trait(RDF.TYPE, All.all(ValuesTest.term("Product"))));

		Assert.assertEquals("matching focus", focus(

				"select * where { ?product a :Product }"

		), Sets.set(matches.keySet()));

		ValuesTest.assertIsomorphic("matching model", model(

				"construct where { ?product a :Product }"

		), model(matches));

	}

	@Test public void testEdgesSorting() {

		final String query="construct { ?product a :Product }"
				+" where { ?product a :Product; rdfs:label ?label; :line ?line }";

		final Shape shape=trait(RDF.TYPE, All.all(ValuesTest.term("Product")));

		// convert to lists to assert ordering

		Assert.assertEquals("default (on value)",

				Lists.list(model(query+" order by ?product")),
				Lists.list(model(edges(shape))));

		Assert.assertEquals("custom increasing",

				Lists.list(model(query+" order by ?label")),
				Lists.list(model(edges(shape, increasing(Step.step(RDFS.LABEL))))));

		Assert.assertEquals("custom decreasing",

				Lists.list(model(query+" order by desc(?label)")),
				Lists.list(model(edges(shape, decreasing(Step.step(RDFS.LABEL))))));

		Assert.assertEquals("custom combined",

				Lists.list(model(query+" order by ?line ?label")),
				Lists.list(model(edges(shape, increasing(Step.step(ValuesTest.term("line"))), increasing(Step.step(RDFS.LABEL))))));

		Assert.assertEquals("custom on root",

				Lists.list(model(query+" order by desc(?product)")),
				Lists.list(model(edges(shape, decreasing()))));

	}


	//// Stats /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testStatsEmptyResultSet() {

		final Map<Value, Collection<Statement>> matches=stats(trait(RDF.TYPE, All.all(RDF.NIL)));

		Assert.assertEquals("meta focus", Sets.set(Form.meta), matches.keySet());

		ValuesTest.assertIsomorphic(

				model("prefix spec: <"+Form.Namespace+"> construct { spec:meta spec:count 0 } where {}"),

				model(matches));
	}

	@Test public void testStatsEmptyProjection() {

		final Map<Value, Collection<Statement>> matches=stats(clazz(ValuesTest.term("Product")));

		ValuesTest.assertIsomorphic(

				model("prefix spec: <"+Form.Namespace+">\n"
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
						+"}"),

				model(matches));

	}

	@Test public void testStatsRootConstraints() {

		final Map<Value, Collection<Statement>> matches=stats(All.all(ValuesTest.item("employees/1370")), Step.step(ValuesTest.term("account")));

		ValuesTest.assertIsomorphic(

				model("prefix spec: <"+Form.Namespace+">\n"
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
						+"}"),

				model(matches));

	}


	//// Items /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testItemsEmptyResultSet() {

		final Map<Value, Collection<Statement>> matches=items(trait(RDF.TYPE, All.all(RDF.NIL)));

		Assert.assertEquals(Sets.set(Form.meta), matches.keySet());

		ValuesTest.assertIsomorphic(

				model("construct {} where {}"),

				model(matches));
	}

	@Test public void testItemsEmptyProjection() {

		final Map<Value, Collection<Statement>> matches=items(clazz(ValuesTest.term("Product")));

		ValuesTest.assertIsomorphic(

				model("prefix spec: <"+Form.Namespace+">\n"
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
						+"}"),

				model(matches));

	}

	@Test public void testItemsRootConstraints() {

		final Map<Value, Collection<Statement>> matches=items(All.all(ValuesTest.item("employees/1370")), Step.step(ValuesTest.term("account")));

		ValuesTest.assertIsomorphic(

				model("prefix spec: <"+Form.Namespace+">\n"
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
						+"}"),

				model(matches));

	}


	//// Constraints ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testClassConstraint() {
		ValuesTest.assertIsomorphic(

				model("construct where { ?product a :Product }"),

				model(edges(and(
						clazz(ValuesTest.term("Product")),
						trait(RDF.TYPE)
				))));
	}

	@Test public void testDirectUniversalConstraint() {
		ValuesTest.assertIsomorphic(

				model("construct { ?root :product ?product } where {\n"
						+"\t?root :product ?product, <products/S10_2016>, <products/S24_2022>\n"
						+"}"),

				model(edges(trait(
						ValuesTest.term("product"),
						All.all(ValuesTest.item("products/S10_2016"), ValuesTest.item("products/S24_2022"))
				))));
	}

	@Test public void testInverseUniversalConstraint() {
		ValuesTest.assertIsomorphic(

				model("construct { ?product :customer ?customer } where {\n"
						+"\t?customer ^:customer ?product, <products/S10_2016>, <products/S24_2022>.\n"
						+"}"),

				model(edges(trait(
						Step.step(ValuesTest.term("customer"), true),
						All.all(ValuesTest.item("products/S10_2016"), ValuesTest.item("products/S24_2022"))
				))));
	}

	@Test public void testRootUniversalConstraint() {
		ValuesTest.assertIsomorphic(

				model("construct { ?product a ?type } where {\n"
						+"\n"
						+"\tvalues ?product { <products/S18_2248> <products/S24_3969> }\n"
						+"\t\n"
						+"\t?product a ?type\n"
						+"\t\n"
						+"}"),

				model(edges(and(
						All.all(ValuesTest.item("products/S18_2248"), ValuesTest.item("products/S24_3969")),
						trait(RDF.TYPE)
				))));
	}

	@Test public void testSingletonUniversalConstraint() {
		ValuesTest.assertIsomorphic(

				model("construct { ?customer :product ?product } where {\n"
						+"\t?customer :product ?product, <products/S10_2016>\n"
						+"}"),

				model(edges(trait(ValuesTest.term("product"), All.all(ValuesTest.item("products/S10_2016"))))));
	}

	@Test public void testExistentialConstraint() {
		ValuesTest.assertIsomorphic(

				model("construct { ?item :product ?product } where {\n"
						+"\t?item :product ?product, ?value filter (?value in (<products/S18_2248>, <products/S24_3969>))\n"
						+"}"),

				model(edges(trait(ValuesTest.term("product"), any(ValuesTest.item("products/S18_2248"), ValuesTest.item("products/S24_3969"))))));
	}

	@Test public void testSingletonExistentialConstraint() {
		ValuesTest.assertIsomorphic(

				model("construct where { <products/S18_2248> rdfs:label ?label }"),

				model(edges(and(
						any(ValuesTest.item("products/S18_2248")),
						trait(RDFS.LABEL)
				))));
	}

	@Test public void testMinExclusiveConstraint() { // 100.17 is the exact sell price of 'The Titanic'
		ValuesTest.assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell > 100.17)\n"
						+"\t\n"
						+"}"),

				model(edges(trait(ValuesTest.term("sell"), MinExclusive.minExclusive(Values.literal(BigDecimal.valueOf(100.17)))))));
	}

	@Test public void testMaxExclusiveConstraint() { // 100.17 is the exact sell price of 'The Titanic'
		ValuesTest.assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell < 100.17)\n"
						+"\t\n"
						+"}"),

				model(edges(trait(ValuesTest.term("sell"), maxExclusive(Values.literal(BigDecimal.valueOf(100.17)))))));
	}

	@Test public void testMinInclusiveConstraint() {
		ValuesTest.assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell >= 100)\n"
						+"\t\n"
						+"}"),

				model(edges(trait(ValuesTest.term("sell"), MinInclusive.minInclusive(Values.literal(BigDecimal.valueOf(100)))))));
	}

	@Test public void testMaxInclusiveConstraint() {
		ValuesTest.assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell <= 100)\n"
						+"\t\n"
						+"}"),

				model(edges(trait(ValuesTest.term("sell"), maxInclusive(Values.literal(BigDecimal.valueOf(100)))))));
	}

	@Test public void testPattern() {
		ValuesTest.assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?item rdfs:label ?label\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item rdfs:label ?label filter regex(?label, '\\\\bferrari\\\\b', 'i')\n"
						+"\n"
						+"}"),

				model(edges(trait(RDFS.LABEL, pattern("\\bferrari\\b", "i")))));
	}

	@Test public void testLike() {
		ValuesTest.assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?item rdfs:label ?label\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item rdfs:label ?label filter regex(?label, '\\\\bromeo\\\\b', 'i')\n"
						+"\n"
						+"}"),

				model(edges(trait(RDFS.LABEL, like("alf ro")))));
	}

	@Test public void testMinLength() {
		ValuesTest.assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?item birt:sell ?sell\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item birt:sell ?sell filter (strlen(str(?sell)) >= 5)\n"
						+"\n"
						+"}"),

				model(edges(trait(ValuesTest.term("sell"), MinLength.minLength(5)))));
	}

	@Test public void testMaxLength() {
		ValuesTest.assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?item birt:sell ?sell\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item birt:sell ?sell filter (strlen(str(?sell)) <= 5)\n"
						+"\n"
						+"}"),

				model(edges(trait(ValuesTest.term("sell"), MaxLength.maxLength(5)))));
	}


	//// Derivates /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testDerivateEdge() {
		ValuesTest.assertIsomorphic(

				model("construct {\n"
						+"\n"
						+"\t?product a :Product; :price ?price.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?product a :Product; :sell ?price.\n"
						+"\n"
						+"}"),

				model(edges(and(
						trait(RDF.TYPE, All.all(ValuesTest.term("Product"))),
						virtual(trait(ValuesTest.term("price")), Step.step(ValuesTest.term("sell")))
				))));
	}

	// !!! nested expressions

	@Ignore @Test public void testDerivateInternalFiltering() {
		ValuesTest.assertIsomorphic(

				model("construct {\n"
						+"\n"
						+"\t?product a :Product; :price ?price.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?product a :Product; :sell ?price filter ( ?price >= 200 )\n"
						+"\n"
						+"}"),

				model(edges(and(
						trait(RDF.TYPE, All.all(ValuesTest.term("Product"))),
						virtual(
								trait(ValuesTest.term("price"), MinInclusive.minInclusive(Values.literal(Values.integer(200)))),
								Step.step(ValuesTest.term("sell"))
						)
				))));
	}

	@Ignore @Test public void testDerivateExternalFiltering() {
		ValuesTest.assertIsomorphic(

				model("construct {\n"
						+"\n"
						+"\t?product a :Product; :price ?price.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?product a :Product; :sell ?price filter ( ?price >= 200 )\n"
						+"\n"
						+"}"),

				model(edges(and(
						trait(RDF.TYPE, All.all(ValuesTest.term("Product"))),
						virtual(trait(ValuesTest.term("price")), Step.step(ValuesTest.term("sell"))),
						trait(ValuesTest.term("price"), MinInclusive.minInclusive(Values.literal(Values.integer(200))))
				))));
	}


	@Ignore @Test public void testDerivateSorting() {
		Assert.assertEquals(

				// convert to lists to assert ordering

				Lists.list(model("construct {\n"
						+"\n"
						+"\t?product a :Product; :price ?price.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?product a :Product; :sell ?price\n"
						+"\n"
						+"} order by ?price")),

				Lists.list(model(edges(and(

						trait(RDF.TYPE, All.all(ValuesTest.term("Product"))),
						virtual(trait(ValuesTest.term("price")), Step.step(ValuesTest.term("sell")))

				))), increasing(Step.step(ValuesTest.term("price")))));
	}

	@Ignore @Test public void testDerivateStats() {
		ValuesTest.assertIsomorphic(

				model(stats(trait(RDF.TYPE), Step.step(RDF.TYPE))),

				model(stats(virtual(trait(RDF.VALUE), Step.step(RDF.TYPE)), Step.step(RDF.VALUE)))

		);
	}

	@Ignore @Test public void testDerivateItems() {
		ValuesTest.assertIsomorphic(

				model(items(trait(RDF.TYPE), Step.step(RDF.TYPE))),

				model(items(virtual(trait(RDF.VALUE), Step.step(RDF.TYPE)), Step.step(RDF.VALUE)))

		);
	}


	//// Aggregates ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testAggregateCount() {
		ValuesTest.assertIsomorphic(

				model("construct {\n"
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
						+"}"),

				model(edges(and(
						trait(RDF.TYPE, All.all(ValuesTest.term("Employee"))),
						virtual(trait(ValuesTest.term("subordinates")), Count.count(Step.step(ValuesTest.term("subordinate"))))
				))));
	}

	@Test public void testAggregateCountOnSingleton() {
		ValuesTest.assertIsomorphic(

				model("construct {\n"
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
						+"}"),

				model(edges(and(
						All.all(ValuesTest.item("product-lines/ships")),
						virtual(trait(ValuesTest.term("size")), Count.count(Step.step(ValuesTest.term("product"))))
				))));
	}

	// !!! sorting (($) project and reuse aggregates computed for filtering/sorting)
	// !!! grouping
	// !!! filtering
	// !!! stats/items
	// !!! nested expressions


	//// Layout ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testUseIndependentPatternsAndFilters() {
		ValuesTest.assertIsomorphic(

				model("construct {\n"
						+"\n"
						+"\t?office :employee ?employee\n"
						+"\t\n"
						+"} where {\n"
						+"\n"
						+"\t?office :employee ?employee, ?x filter (?x in (<employees/1002>, <employees/1188>))\n"
						+"\n"
						+"}"),

				model(edges(and(
						trait(ValuesTest.term("employee")),
						com.metreeca.form.shapes.Test.test(filter(), trait(ValuesTest.term("employee"), any(ValuesTest.item("employees/1002"), ValuesTest.item("employees/1188"))))
				))));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<Value, Collection<Statement>> edges(final Shape shape, final Query.Order... orders) {
		return process(new Edges(shape, Lists.list(orders), 0, 0));
	}

	private Map<Value, Collection<Statement>> stats(final Shape shape, final Step... path) {
		return process(new Stats(shape, Lists.list(path)));
	}

	private Map<Value, Collection<Statement>> items(final Shape shape, final Step... path) {
		return process(new Items(shape, Lists.list(path)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> model(final Map<Value, Collection<Statement>> matches) {
		return matches.values().stream().reduce(Lists.list(), (x, y) -> Lists.concat(x, y));
	}

	private Map<Value, Collection<Statement>> process(final Query query) {
		try ( final RepositoryConnection connection=sandbox.get()) {
			return new SPARQLReader(connection).process(query);
		}
	}

	private Set<Value> focus(final String query) {
		try ( final RepositoryConnection connection=sandbox.get()) {
			return ValuesTest.select(connection, query)
					.stream()
					.flatMap(tuple -> tuple.values().stream())
					.collect(toSet());
		}
	}

	private Collection<Statement> model(final String query) {
		try ( final RepositoryConnection connection=sandbox.get()) {
			return ValuesTest.construct(connection, query);
		}
	}

}
