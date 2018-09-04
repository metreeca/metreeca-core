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

package com.metreeca.form.codecs;

import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Lists;
import com.metreeca.form.things.Values;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Test;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Assert;

import java.util.function.Consumer;

import javax.json.JsonException;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.MaxLength.maxLength;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.literal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;


public class QueryParserTest {

	private static final Literal One=Values.literal(ONE);
	private static final Literal Ten=Values.literal(TEN);

	private static final Step first=Step.step(RDF.FIRST);
	private static final Step rest=Step.step(RDF.REST);

	private static final Shape shape=and(
			trait(first, trait(rest)),
			trait(first.invert(), trait(rest))
	);


	@org.junit.Test public void testParseEmptyString() {

		final Shape shape=and();

		graph("", shape, edges -> {

			assertEquals("base shape", shape, edges.getShape());

			Assert.assertEquals("no offset", 0, edges.getOffset());
			Assert.assertEquals("no limit", 0, edges.getLimit());

		});
	}

	@org.junit.Test public void testParsePaths() {

		stats("{ \"stats\": \"\" }", shape, stats ->
				Assert.assertEquals("empty path", Lists.list(), stats.getPath()));

		stats("{ \"stats\": \""+RDF.FIRST+"\" }", shape, stats ->
				Assert.assertEquals("direct naked iri", Lists.list(first), stats.getPath()));

		stats("{ \"stats\": \"^"+RDF.FIRST+"\" }", shape, stats ->
				Assert.assertEquals("inverse naked iri", Lists.list(first.invert()), stats.getPath()));

		stats("{ \"stats\": \"<"+RDF.FIRST+">\" }", shape, stats ->
				Assert.assertEquals("direct brackets iri", Lists.list(first), stats.getPath()));

		stats("{ \"stats\": \"^<"+RDF.FIRST+">\" }", shape, stats ->
				Assert.assertEquals("inverse brackets iri", Lists.list(first.invert()), stats.getPath()));

		stats("{ \"stats\": \"<"+RDF.FIRST+">/<"+RDF.REST+">\" }", shape, stats ->
				Assert.assertEquals("iri slash path", Lists.list(first, rest), stats.getPath()));

		stats("{ \"stats\": \"first\" }", shape, stats ->
				Assert.assertEquals("direct alias", Lists.list(first), stats.getPath()));

		stats("{ \"stats\": \"firstOf\" }", shape, stats ->
				Assert.assertEquals("inverse alias", Lists.list(first.invert()), stats.getPath()));

		stats("{ \"stats\": \"first/rest\" }", shape, stats ->
				Assert.assertEquals("alias slash path", Lists.list(first, rest), stats.getPath()));

		stats("{ \"stats\": \"firstOf.rest\" }", shape, stats ->
				Assert.assertEquals("alias dot path", Lists.list(first.invert(), rest), stats.getPath()));

	}


	@org.junit.Test public void testParseSortingCriteria() {

		graph("{ \"order\": \"\" }", shape, edges ->
				assertEquals("empty path", Lists.list(Query.increasing()), edges.getOrders()));

		graph("{ \"order\": \"+\" }", shape, edges ->
				assertEquals("empty path increasing", Lists.list(Query.increasing()), edges.getOrders()));

		graph("{ \"order\": \"-\" }", shape, edges ->
				assertEquals("empty path decreasing", Lists.list(Query.decreasing()), edges.getOrders()));

		graph("{ \"order\": \"first.rest\" }", shape, edges ->
				assertEquals("path", Lists.list(Query.increasing(first, rest)), edges.getOrders()));

		graph("{ \"order\": \"+first.rest\" }", shape, edges ->
				assertEquals("path increasing", Lists.list(Query.increasing(first, rest)), edges.getOrders()));

		graph("{ \"order\": \"-first.rest\" }", shape, edges ->
				assertEquals("path decreasing", Lists.list(Query.decreasing(first, rest)), edges.getOrders()));

		graph("{ \"order\": [] }", shape, edges ->
				Assert.assertEquals("empty list", Lists.list(), edges.getOrders()));

		graph("{ \"order\": [\"+first\", \"-first.rest\"] }", shape, edges ->
				Assert.assertEquals("list", Lists.list(Query.increasing(first), Query.decreasing(first, rest)), edges.getOrders()));

	}

	@org.junit.Test public void testParseSimpleFilters() {

		graph("{ \"filter\": { \">>\": 1 } }", shape, edges ->
				assertEquals("min count", filter(shape, minCount(1)), edges.getShape()));

		graph("{ \"filter\": { \"<<\": 1 } }", shape, edges ->
				assertEquals("max count", filter(shape, maxCount(1)), edges.getShape()));


		graph("{ \"filter\": { \">=\": 1 } }", shape, edges ->
				assertEquals("min inclusive", filter(shape, MinInclusive.minInclusive(One)), edges.getShape()));

		graph("{ \"filter\": { \"<=\": 1 } }", shape, edges ->
				assertEquals("max inclusive", filter(shape, maxInclusive(One)), edges.getShape()));

		graph("{ \"filter\": { \">\": 1 } }", shape, edges ->
				assertEquals("min exclusive", filter(shape, MinExclusive.minExclusive(One)), edges.getShape()));

		graph("{ \"filter\": { \"<\": 1 } }", shape, edges ->
				assertEquals("max exclusive", filter(shape, maxExclusive(One)), edges.getShape()));

		graph("{ \"filter\": { \"~\": \"words\" } }", shape, edges ->
				assertEquals("like", filter(shape, like("words")), edges.getShape()));

		graph("{ \"filter\": { \"*\": \"pattern\" } }", shape, edges ->
				assertEquals("pattern", filter(shape, pattern("pattern")), edges.getShape()));

		graph("{ \"filter\": { \">#\": 123 } }", shape, edges ->
				assertEquals("min length", filter(shape, MinLength.minLength(123)), edges.getShape()));

		graph("{ \"filter\": { \"#<\": 123 } }", shape, edges ->
				assertEquals("max length", filter(shape, maxLength(123)), edges.getShape()));


		graph("{ \"filter\": { \"@\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\" } }", shape, edges ->
				assertEquals("class", filter(shape, clazz(RDF.NIL)), edges.getShape()));

		graph("{ \"filter\": { \"^\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\" } }", shape, edges ->
				assertEquals("type", filter(shape, datatype(RDF.NIL)), edges.getShape()));


		graph("{ \"filter\": { \"?\": [] } }", shape, edges ->
				assertEquals("existential (empty)", filter(shape, and()), edges.getShape()));

		graph("{ \"filter\": { \"?\": { \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" } } }", shape, edges ->
				assertEquals("existential (singleton)", filter(shape, any(RDF.FIRST)), edges.getShape()));

		graph("{ \"filter\": { \"?\": [\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" },\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\" }\n"
				+"] } }", shape, edges ->
				assertEquals("existential (multiple)", filter(shape, any(RDF.FIRST, RDF.REST)), edges.getShape()));


		graph("{ \"filter\": { \"!\": [] } }", shape, edges ->
				assertEquals("universal (empty)", filter(shape, and()), edges.getShape()));

		graph("{ \"filter\": { \"!\": { \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" } } }", shape, edges ->
				assertEquals("universal (singleton)", filter(shape, All.all(RDF.FIRST)), edges.getShape()));

		graph("{ \"filter\": { \"!\": [\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" },\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\" }\n"
				+"] } }", shape, edges ->
				assertEquals("universal (multiple)", filter(shape, All.all(RDF.FIRST, RDF.REST)), edges.getShape()));

	}

	@org.junit.Test public void testParseStructuredFilters() {

		graph("{\n\t\"filter\": {}\n}", shape, edges ->
				assertEquals("empty filter", filter(shape, and()), edges.getShape()));

		graph("{ \"filter\": { \"first.rest\": { \">=\": 1 } } }", shape, edges ->
				assertEquals("nested filter", filter(shape, trait(first, trait(rest, MinInclusive.minInclusive(One)))), edges.getShape()));

		graph("{ \"filter\": { \"first.rest\": 1 } }", shape, edges ->
				assertEquals("nested filter singleton shorthand", filter(shape, trait(first, trait(rest, any(One)))), edges.getShape()));

		graph("{ \"filter\": { \"first.rest\": [1, 10] } }", shape, edges ->
				assertEquals("nested filter multiple shorthand", filter(shape, trait(first, trait(rest, any(One, Ten)))), edges.getShape()));

	}


	@org.junit.Test public void testParseEdgesQuery() {

		graph("{ \"filter\": {}, \"offset\": 1, \"limit\": 2 }", shape, edges -> {

			assertEquals("shape", filter(shape, and()), edges.getShape());
			Assert.assertEquals("offset", 1, edges.getOffset());
			Assert.assertEquals("limit", 2, edges.getLimit());

		});

	}

	@org.junit.Test public void testParseStatsQuery() {

		stats("{ \"filter\": {}, \"stats\": \"\" }", shape, stats -> {

			assertEquals("shape", filter(shape, and()), stats.getShape());
			Assert.assertEquals("path", Lists.list(), stats.getPath());

		});

	}

	@org.junit.Test public void testParseItemsQuery() {

		items("{ \"filter\": {}, \"items\": \"\" }", shape, items -> {

			assertEquals("shape", filter(shape, and()), items.getShape());
			Assert.assertEquals("path", Lists.list(), items.getPath());

		});

	}


	@org.junit.Test(expected=JsonException.class) public void testRejectNullFilters() {
		graph("{ \"filter\": null }", shape, edges -> {});
	}

	@org.junit.Test(expected=JsonException.class) public void testRejectNullValues() {
		graph("{ \"filter\": { \"first\": null } }", shape, edges -> {});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void graph(final String json, final Shape shape, final Consumer<Edges> tester) {
		test(json, shape, new Query.Probe<Boolean>() {
			@Override public Boolean visit(final Edges edges) {

				tester.accept(edges);

				return true;
			}
		});
	}

	private void stats(final String json, final Shape shape, final Consumer<Stats> tester) {
		test(json, shape, new Query.Probe<Boolean>() {
			@Override public Boolean visit(final Stats stats) {

				tester.accept(stats);

				return true;
			}
		});
	}

	private void items(final String json, final Shape shape, final Consumer<Items> tester) {
		test(json, shape, new Query.Probe<Boolean>() {
			@Override public Boolean visit(final Items items) {

				tester.accept(items);

				return true;
			}
		});
	}


	private void test(final String json, final Shape shape, final Query.Probe<Boolean> probe) {
		assertTrue("query processed", new QueryParser(shape).parse(json).accept(probe));
	}

	private Shape filter(final Shape shape, final Shape filter) {
		return and(shape, Test.test(Shape.filter(), filter));
	}

}
