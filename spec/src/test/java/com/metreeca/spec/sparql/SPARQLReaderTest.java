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

package com.metreeca.spec.sparql;

import com.metreeca.spec.Query;
import com.metreeca.spec.Shape;
import com.metreeca.spec.Spec;
import com.metreeca.spec.queries.Edges;
import com.metreeca.spec.queries.Items;
import com.metreeca.spec.queries.Stats;
import com.metreeca.spec.shapes.MaxLength;
import com.metreeca.spec.shapes.MinLength;
import com.metreeca.spec.shifts.Count;
import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.Ignore;
import org.junit.Test;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;
import java.util.Set;

import static com.metreeca.spec.Query.decreasing;
import static com.metreeca.spec.Query.increasing;
import static com.metreeca.spec.Shape.filter;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Any.any;
import static com.metreeca.spec.shapes.Clazz.clazz;
import static com.metreeca.spec.shapes.Like.like;
import static com.metreeca.spec.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.spec.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.spec.shapes.MinExclusive.minExclusive;
import static com.metreeca.spec.shapes.MinInclusive.minInclusive;
import static com.metreeca.spec.shapes.Pattern.pattern;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shapes.Virtual.virtual;
import static com.metreeca.spec.shifts.Step.step;
import static com.metreeca.spec.things.Lists.concat;
import static com.metreeca.spec.things.Lists.list;
import static com.metreeca.spec.things.Sets.set;
import static com.metreeca.spec.things.Values.integer;
import static com.metreeca.spec.things.Values.literal;
import static com.metreeca.spec.things.ValuesTest.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.util.stream.Collectors.toSet;


public class SPARQLReaderTest {

	//// Edges /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testEdgesEmptyShape() {

		final Map<Value, Collection<Statement>> matches=edges(and());

		assertTrue("empty focus", matches.isEmpty());

	}

	@Test public void testEdgesEmptyResultSet() {

		final Map<Value, Collection<Statement>> matches=edges(trait(RDF.TYPE, all(RDF.NIL)));

		assertTrue("empty focus", matches.isEmpty());

	}

	@Test public void testEdgesEmptyProjection() {

		final Map<Value, Collection<Statement>> matches=edges(clazz(term("Product")));

		assertEquals("matching focus", focus(

				"select * where { ?product a :Product }"

		), matches.keySet());

		assertTrue("empty model", model(matches).isEmpty());

	}

	@Test public void testEdgesMatching() {

		final Map<Value, Collection<Statement>> matches=edges(trait(RDF.TYPE, all(term("Product"))));

		assertEquals("matching focus", focus(

				"select * where { ?product a :Product }"

		), set(matches.keySet()));

		assertIsomorphic("matching model", model(

				"construct where { ?product a :Product }"

		), model(matches));

	}

	@Test public void testEdgesSorting() {

		final String query="construct { ?product a :Product }"
				+" where { ?product a :Product; rdfs:label ?label; :line ?line }";

		final Shape shape=trait(RDF.TYPE, all(term("Product")));

		// convert to lists to assert ordering

		assertEquals("default (on value)",

				list(model(query+" order by ?product")),
				list(model(edges(shape))));

		assertEquals("custom increasing",

				list(model(query+" order by ?label")),
				list(model(edges(shape, increasing(step(RDFS.LABEL))))));

		assertEquals("custom decreasing",

				list(model(query+" order by desc(?label)")),
				list(model(edges(shape, decreasing(step(RDFS.LABEL))))));

		assertEquals("custom combined",

				list(model(query+" order by ?line ?label")),
				list(model(edges(shape, increasing(step(term("line"))), increasing(step(RDFS.LABEL))))));

		assertEquals("custom on root",

				list(model(query+" order by desc(?product)")),
				list(model(edges(shape, decreasing()))));

	}


	//// Stats /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testStatsEmptyResultSet() {

		final Map<Value, Collection<Statement>> matches=stats(trait(RDF.TYPE, all(RDF.NIL)));

		assertEquals("meta focus", set(Spec.meta), matches.keySet());

		assertIsomorphic(

				model("prefix spec: <"+Spec.Namespace+"> construct { spec:meta spec:count 0 } where {}"),

				model(matches));
	}

	@Test public void testStatsEmptyProjection() {

		final Map<Value, Collection<Statement>> matches=stats(clazz(term("Product")));

		assertIsomorphic(

				model("prefix spec: <"+Spec.Namespace+">\n"
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

		final Map<Value, Collection<Statement>> matches=stats(all(item("employees/1370")), step(term("account")));

		assertIsomorphic(

				model("prefix spec: <"+Spec.Namespace+">\n"
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

		final Map<Value, Collection<Statement>> matches=items(trait(RDF.TYPE, all(RDF.NIL)));

		assertEquals(set(Spec.meta), matches.keySet());

		assertIsomorphic(

				model("construct {} where {}"),

				model(matches));
	}

	@Test public void testItemsEmptyProjection() {

		final Map<Value, Collection<Statement>> matches=items(clazz(term("Product")));

		assertIsomorphic(

				model("prefix spec: <"+Spec.Namespace+">\n"
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

		final Map<Value, Collection<Statement>> matches=items(all(item("employees/1370")), step(term("account")));

		assertIsomorphic(

				model("prefix spec: <"+Spec.Namespace+">\n"
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
		assertIsomorphic(

				model("construct where { ?product a :Product }"),

				model(edges(and(
						clazz(term("Product")),
						trait(RDF.TYPE)
				))));
	}

	@Test public void testDirectUniversalConstraint() {
		assertIsomorphic(

				model("construct { ?root :product ?product } where {\n"
						+"\t?root :product ?product, <products/S10_2016>, <products/S24_2022>\n"
						+"}"),

				model(edges(trait(
						term("product"),
						all(item("products/S10_2016"), item("products/S24_2022"))
				))));
	}

	@Test public void testInverseUniversalConstraint() {
		assertIsomorphic(

				model("construct { ?product :customer ?customer } where {\n"
						+"\t?customer ^:customer ?product, <products/S10_2016>, <products/S24_2022>.\n"
						+"}"),

				model(edges(trait(
						step(term("customer"), true),
						all(item("products/S10_2016"), item("products/S24_2022"))
				))));
	}

	@Test public void testRootUniversalConstraint() {
		assertIsomorphic(

				model("construct { ?product a ?type } where {\n"
						+"\n"
						+"\tvalues ?product { <products/S18_2248> <products/S24_3969> }\n"
						+"\t\n"
						+"\t?product a ?type\n"
						+"\t\n"
						+"}"),

				model(edges(and(
						all(item("products/S18_2248"), item("products/S24_3969")),
						trait(RDF.TYPE)
				))));
	}

	@Test public void testSingletonUniversalConstraint() {
		assertIsomorphic(

				model("construct { ?customer :product ?product } where {\n"
						+"\t?customer :product ?product, <products/S10_2016>\n"
						+"}"),

				model(edges(trait(term("product"), all(item("products/S10_2016"))))));
	}

	@Test public void testExistentialConstraint() {
		assertIsomorphic(

				model("construct { ?item :product ?product } where {\n"
						+"\t?item :product ?product, ?value filter (?value in (<products/S18_2248>, <products/S24_3969>))\n"
						+"}"),

				model(edges(trait(term("product"), any(item("products/S18_2248"), item("products/S24_3969"))))));
	}

	@Test public void testSingletonExistentialConstraint() {
		assertIsomorphic(

				model("construct where { <products/S18_2248> rdfs:label ?label }"),

				model(edges(and(
						any(item("products/S18_2248")),
						trait(RDFS.LABEL)
				))));
	}

	@Test public void testMinExclusiveConstraint() { // 100.17 is the exact sell price of 'The Titanic'
		assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell > 100.17)\n"
						+"\t\n"
						+"}"),

				model(edges(trait(term("sell"), minExclusive(literal(BigDecimal.valueOf(100.17)))))));
	}

	@Test public void testMaxExclusiveConstraint() { // 100.17 is the exact sell price of 'The Titanic'
		assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell < 100.17)\n"
						+"\t\n"
						+"}"),

				model(edges(trait(term("sell"), maxExclusive(literal(BigDecimal.valueOf(100.17)))))));
	}

	@Test public void testMinInclusiveConstraint() {
		assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell >= 100)\n"
						+"\t\n"
						+"}"),

				model(edges(trait(term("sell"), minInclusive(literal(BigDecimal.valueOf(100)))))));
	}

	@Test public void testMaxInclusiveConstraint() {
		assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?product :sell ?sell.\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?product :sell ?sell filter (?sell <= 100)\n"
						+"\t\n"
						+"}"),

				model(edges(trait(term("sell"), maxInclusive(literal(BigDecimal.valueOf(100)))))));
	}

	@Test public void testPattern() {
		assertIsomorphic(

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
		assertIsomorphic(

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
		assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?item birt:sell ?sell\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item birt:sell ?sell filter (strlen(str(?sell)) >= 5)\n"
						+"\n"
						+"}"),

				model(edges(trait(term("sell"), MinLength.minLength(5)))));
	}

	@Test public void testMaxLength() {
		assertIsomorphic(

				model("construct { \n"
						+"\n"
						+"\t?item birt:sell ?sell\n"
						+"\t \n"
						+"} where { \n"
						+"\n"
						+"\t?item birt:sell ?sell filter (strlen(str(?sell)) <= 5)\n"
						+"\n"
						+"}"),

				model(edges(trait(term("sell"), MaxLength.maxLength(5)))));
	}


	//// Derivates /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testDerivateEdge() {
		assertIsomorphic(

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
						trait(RDF.TYPE, all(term("Product"))),
						virtual(trait(term("price")), step(term("sell")))
				))));
	}

	// !!! nested expressions

	@Ignore @Test public void testDerivateInternalFiltering() {
		assertIsomorphic(

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
						trait(RDF.TYPE, all(term("Product"))),
						virtual(
								trait(term("price"), minInclusive(literal(integer(200)))),
								step(term("sell"))
						)
				))));
	}

	@Ignore @Test public void testDerivateExternalFiltering() {
		assertIsomorphic(

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
						trait(RDF.TYPE, all(term("Product"))),
						virtual(trait(term("price")), step(term("sell"))),
						trait(term("price"), minInclusive(literal(integer(200))))
				))));
	}


	@Ignore @Test public void testDerivateSorting() {
		assertEquals(

				// convert to lists to assert ordering

				list(model("construct {\n"
						+"\n"
						+"\t?product a :Product; :price ?price.\n"
						+"\n"
						+"} where {\n"
						+"\n"
						+"\t?product a :Product; :sell ?price\n"
						+"\n"
						+"} order by ?price")),

				list(model(edges(and(

						trait(RDF.TYPE, all(term("Product"))),
						virtual(trait(term("price")), step(term("sell")))

				))), increasing(step(term("price")))));
	}

	@Ignore @Test public void testDerivateStats() {
		assertIsomorphic(

				model(stats(trait(RDF.TYPE), step(RDF.TYPE))),

				model(stats(virtual(trait(RDF.VALUE), step(RDF.TYPE)), step(RDF.VALUE)))

		);
	}

	@Ignore @Test public void testDerivateItems() {
		assertIsomorphic(

				model(items(trait(RDF.TYPE), step(RDF.TYPE))),

				model(items(virtual(trait(RDF.VALUE), step(RDF.TYPE)), step(RDF.VALUE)))

		);
	}


	//// Aggregates ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testAggregateCount() {
		assertIsomorphic(

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
						trait(RDF.TYPE, all(term("Employee"))),
						virtual(trait(term("subordinates")), Count.count(step(term("subordinate"))))
				))));
	}

	@Test public void testAggregateCountOnSingleton() {
		assertIsomorphic(

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
						all(item("product-lines/ships")),
						virtual(trait(term("size")), Count.count(step(term("product"))))
				))));
	}

	// !!! sorting (($) project and reuse aggregates computed for filtering/sorting)
	// !!! grouping
	// !!! filtering
	// !!! stats/items
	// !!! nested expressions


	//// Layout ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testUseIndependentPatternsAndFilters() {
		assertIsomorphic(

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
						trait(term("employee")),
						test(filter(), trait(term("employee"), any(item("employees/1002"), item("employees/1188"))))
				))));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Map<Value, Collection<Statement>> edges(final Shape shape, final Query.Order... orders) {
		return process(new Edges(shape, list(orders), 0, 0));
	}

	private Map<Value, Collection<Statement>> stats(final Shape shape, final Step... path) {
		return process(new Stats(shape, list(path)));
	}

	private Map<Value, Collection<Statement>> items(final Shape shape, final Step... path) {
		return process(new Items(shape, list(path)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Collection<Statement> model(final Map<Value, Collection<Statement>> matches) {
		return matches.values().stream().reduce(list(), (x, y) -> concat(x, y));
	}

	private Map<Value, Collection<Statement>> process(final Query query) {
		return connection(BIRT, connection -> new SPARQLReader(connection).process(query));
	}

	private Set<Value> focus(final String query) {
		return select(BIRT, query).stream().flatMap(tuple -> tuple.values().stream()).collect(toSet());
	}

	private Collection<Statement> model(final String query) {
		return construct(BIRT, query);
	}

}
