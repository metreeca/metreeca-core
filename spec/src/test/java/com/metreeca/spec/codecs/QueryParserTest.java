/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec.codecs;

import com.metreeca.jeep.Jeep;
import com.metreeca.spec.Query;
import com.metreeca.spec.Shape;
import com.metreeca.spec.queries.Graph;
import com.metreeca.spec.queries.Items;
import com.metreeca.spec.queries.Stats;
import com.metreeca.spec.shapes.Test;
import com.metreeca.spec.shifts.Step;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.function.Consumer;

import javax.json.JsonException;

import static com.metreeca.jeep.Jeep.list;
import static com.metreeca.spec.Values.literal;
import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Any.any;
import static com.metreeca.spec.shapes.Clazz.clazz;
import static com.metreeca.spec.shapes.Datatype.datatype;
import static com.metreeca.spec.shapes.Like.like;
import static com.metreeca.spec.shapes.MaxCount.maxCount;
import static com.metreeca.spec.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.spec.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.spec.shapes.MaxLength.maxLength;
import static com.metreeca.spec.shapes.MinCount.minCount;
import static com.metreeca.spec.shapes.MinExclusive.minExclusive;
import static com.metreeca.spec.shapes.MinInclusive.minInclusive;
import static com.metreeca.spec.shapes.MinLength.minLength;
import static com.metreeca.spec.shapes.Pattern.pattern;
import static com.metreeca.spec.shapes.Trait.trait;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;


public class QueryParserTest {

	private static final Literal One=literal(ONE);
	private static final Literal Ten=literal(TEN);

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

			assertEquals("no offset", 0, edges.getOffset());
			assertEquals("no limit", 0, edges.getLimit());

		});
	}

	@org.junit.Test public void testParsePaths() {

		stats("{ \"stats\": \"\" }", shape, stats ->
				assertEquals("empty path", list(), stats.getPath()));

		stats("{ \"stats\": \""+RDF.FIRST+"\" }", shape, stats ->
				assertEquals("direct naked iri", list(first), stats.getPath()));

		stats("{ \"stats\": \"^"+RDF.FIRST+"\" }", shape, stats ->
				assertEquals("inverse naked iri", list(first.invert()), stats.getPath()));

		stats("{ \"stats\": \"<"+RDF.FIRST+">\" }", shape, stats ->
				assertEquals("direct brackets iri", list(first), stats.getPath()));

		stats("{ \"stats\": \"^<"+RDF.FIRST+">\" }", shape, stats ->
				assertEquals("inverse brackets iri", list(first.invert()), stats.getPath()));

		stats("{ \"stats\": \"<"+RDF.FIRST+">/<"+RDF.REST+">\" }", shape, stats ->
				assertEquals("iri slash path", list(first, rest), stats.getPath()));

		stats("{ \"stats\": \"first\" }", shape, stats ->
				assertEquals("direct alias", list(first), stats.getPath()));

		stats("{ \"stats\": \"firstOf\" }", shape, stats ->
				assertEquals("inverse alias", list(first.invert()), stats.getPath()));

		stats("{ \"stats\": \"first/rest\" }", shape, stats ->
				assertEquals("alias slash path", list(first, rest), stats.getPath()));

		stats("{ \"stats\": \"firstOf.rest\" }", shape, stats ->
				assertEquals("alias dot path", list(first.invert(), rest), stats.getPath()));

	}


	@org.junit.Test public void testParseSortingCriteria() {

		graph("{ \"order\": \"\" }", shape, edges ->
				assertEquals("empty path", Jeep.list(Query.increasing()), edges.getOrders()));

		graph("{ \"order\": \"+\" }", shape, edges ->
				assertEquals("empty path increasing", Jeep.list(Query.increasing()), edges.getOrders()));

		graph("{ \"order\": \"-\" }", shape, edges ->
				assertEquals("empty path decreasing", Jeep.list(Query.decreasing()), edges.getOrders()));

		graph("{ \"order\": \"first.rest\" }", shape, edges ->
				assertEquals("path", Jeep.list(Query.increasing(first, rest)), edges.getOrders()));

		graph("{ \"order\": \"+first.rest\" }", shape, edges ->
				assertEquals("path increasing", Jeep.list(Query.increasing(first, rest)), edges.getOrders()));

		graph("{ \"order\": \"-first.rest\" }", shape, edges ->
				assertEquals("path decreasing", Jeep.list(Query.decreasing(first, rest)), edges.getOrders()));

		graph("{ \"order\": [] }", shape, edges ->
				assertEquals("empty list", list(), edges.getOrders()));

		graph("{ \"order\": [\"+first\", \"-first.rest\"] }", shape, edges ->
				assertEquals("list", list(Query.increasing(first), Query.decreasing(first, rest)), edges.getOrders()));

	}

	@org.junit.Test public void testParseSimpleFilters() {

		graph("{ \"filter\": { \">>\": 1 } }", shape, edges ->
				assertEquals("min count", filter(shape, minCount(1)), edges.getShape()));

		graph("{ \"filter\": { \"<<\": 1 } }", shape, edges ->
				assertEquals("max count", filter(shape, maxCount(1)), edges.getShape()));


		graph("{ \"filter\": { \">=\": 1 } }", shape, edges ->
				assertEquals("min inclusive", filter(shape, minInclusive(One)), edges.getShape()));

		graph("{ \"filter\": { \"<=\": 1 } }", shape, edges ->
				assertEquals("max inclusive", filter(shape, maxInclusive(One)), edges.getShape()));

		graph("{ \"filter\": { \">\": 1 } }", shape, edges ->
				assertEquals("min exclusive", filter(shape, minExclusive(One)), edges.getShape()));

		graph("{ \"filter\": { \"<\": 1 } }", shape, edges ->
				assertEquals("max exclusive", filter(shape, maxExclusive(One)), edges.getShape()));

		graph("{ \"filter\": { \"~\": \"words\" } }", shape, edges ->
				assertEquals("like", filter(shape, like("words")), edges.getShape()));

		graph("{ \"filter\": { \"*\": \"pattern\" } }", shape, edges ->
				assertEquals("pattern", filter(shape, pattern("pattern")), edges.getShape()));

		graph("{ \"filter\": { \">#\": 123 } }", shape, edges ->
				assertEquals("min length", filter(shape, minLength(123)), edges.getShape()));

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
				assertEquals("universal (singleton)", filter(shape, all(RDF.FIRST)), edges.getShape()));

		graph("{ \"filter\": { \"!\": [\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" },\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\" }\n"
				+"] } }", shape, edges ->
				assertEquals("universal (multiple)", filter(shape, all(RDF.FIRST, RDF.REST)), edges.getShape()));

	}

	@org.junit.Test public void testParseStructuredFilters() {

		graph("{\n\t\"filter\": {}\n}", shape, edges ->
				assertEquals("empty filter", filter(shape, and()), edges.getShape()));

		graph("{ \"filter\": { \"first.rest\": { \">=\": 1 } } }", shape, edges ->
				assertEquals("nested filter", filter(shape, trait(first, trait(rest, minInclusive(One)))), edges.getShape()));

		graph("{ \"filter\": { \"first.rest\": 1 } }", shape, edges ->
				assertEquals("nested filter singleton shorthand", filter(shape, trait(first, trait(rest, any(One)))), edges.getShape()));

		graph("{ \"filter\": { \"first.rest\": [1, 10] } }", shape, edges ->
				assertEquals("nested filter multiple shorthand", filter(shape, trait(first, trait(rest, any(One, Ten)))), edges.getShape()));

	}


	@org.junit.Test public void testParseEdgesQuery() {

		graph("{ \"filter\": {}, \"offset\": 1, \"limit\": 2 }", shape, edges -> {

			assertEquals("shape", filter(shape, and()), edges.getShape());
			assertEquals("offset", 1, edges.getOffset());
			assertEquals("limit", 2, edges.getLimit());

		});

	}

	@org.junit.Test public void testParseStatsQuery() {

		stats("{ \"filter\": {}, \"stats\": \"\" }", shape, stats -> {

			assertEquals("shape", filter(shape, and()), stats.getShape());
			assertEquals("path", list(), stats.getPath());

		});

	}

	@org.junit.Test public void testParseItemsQuery() {

		items("{ \"filter\": {}, \"items\": \"\" }", shape, items -> {

			assertEquals("shape", filter(shape, and()), items.getShape());
			assertEquals("path", list(), items.getPath());

		});

	}


	@org.junit.Test(expected=JsonException.class) public void testRejectNullFilters() {
		graph("{ \"filter\": null }", shape, edges -> {});
	}

	@org.junit.Test(expected=JsonException.class) public void testRejectNullValues() {
		graph("{ \"filter\": { \"first\": null } }", shape, edges -> {});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void graph(final String json, final Shape shape, final Consumer<Graph> tester) {
		test(json, shape, new Query.Probe<Boolean>() {
			@Override public Boolean visit(final Graph graph) {

				tester.accept(graph);

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
