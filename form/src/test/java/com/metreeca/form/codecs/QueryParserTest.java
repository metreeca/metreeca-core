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

package com.metreeca.form.codecs;

import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Lists;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import javax.json.JsonException;

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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;


final class QueryParserTest {

	private static final Literal One=Values.literal(ONE);
	private static final Literal Ten=Values.literal(TEN);

	private static final Step first=Step.step(RDF.FIRST);
	private static final Step rest=Step.step(RDF.REST);

	private static final Shape shape=and(
			trait(first, trait(rest)),
			trait(first.invert(), trait(rest))
	);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseEmptyString() {

		final Shape shape=and();

		edges("", shape, edges -> {

			assertThat(shape).as("base shape").isEqualTo(edges.getShape());

			assertThat(0).as("no offset").isEqualTo(edges.getOffset());
			assertThat(0).as("no limit").isEqualTo(edges.getLimit());

		});
	}

	@Test void testParsePaths() {

		stats("{ \"stats\": \"\" }", shape, stats -> assertThat(Lists.list()).as("empty path").isEqualTo(stats.getPath()));

		stats("{ \"stats\": \""+RDF.FIRST+"\" }", shape, stats -> assertThat(Lists.list(first)).as("direct naked iri").isEqualTo(stats.getPath()));

		stats("{ \"stats\": \"^"+RDF.FIRST+"\" }", shape, stats -> assertThat(Lists.list(first.invert())).as("inverse naked iri").isEqualTo(stats.getPath()));

		stats("{ \"stats\": \"<"+RDF.FIRST+">\" }", shape, stats -> assertThat(Lists.list(first)).as("direct brackets iri").isEqualTo(stats.getPath()));

		stats("{ \"stats\": \"^<"+RDF.FIRST+">\" }", shape, stats -> assertThat(Lists.list(first.invert())).as("inverse brackets iri").isEqualTo(stats.getPath()));

		stats("{ \"stats\": \"<"+RDF.FIRST+">/<"+RDF.REST+">\" }", shape, stats -> assertThat(Lists.list(first, rest)).as("iri slash path").isEqualTo(stats.getPath()));

		stats("{ \"stats\": \"first\" }", shape, stats -> assertThat(Lists.list(first)).as("direct alias").isEqualTo(stats.getPath()));

		stats("{ \"stats\": \"firstOf\" }", shape, stats -> assertThat(Lists.list(first.invert())).as("inverse alias").isEqualTo(stats.getPath()));

		stats("{ \"stats\": \"first/rest\" }", shape, stats -> assertThat(Lists.list(first, rest)).as("alias slash path").isEqualTo(stats.getPath()));

		stats("{ \"stats\": \"firstOf.rest\" }", shape, stats -> assertThat(Lists.list(first.invert(), rest)).as("alias dot path").isEqualTo(stats.getPath()));

	}


	@Test void testParseSortingCriteria() {

		edges("{ \"order\": \"\" }", shape, edges -> assertThat(Lists.list(Query.increasing())).as("empty path").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"+\" }", shape, edges -> assertThat(Lists.list(Query.increasing())).as("empty path increasing").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"-\" }", shape, edges -> assertThat(Lists.list(Query.decreasing())).as("empty path decreasing").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"first.rest\" }", shape, edges -> assertThat(Lists.list(Query.increasing(first, rest))).as("path").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"+first.rest\" }", shape, edges -> assertThat(Lists.list(Query.increasing(first, rest))).as("path increasing").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"-first.rest\" }", shape, edges -> assertThat(Lists.list(Query.decreasing(first, rest))).as("path decreasing").isEqualTo(edges.getOrders()));

		edges("{ \"order\": [] }", shape, edges -> assertThat(Lists.list()).as("empty list").isEqualTo(edges.getOrders()));

		edges("{ \"order\": [\"+first\", \"-first.rest\"] }", shape, edges -> assertThat(Lists.list(Query.increasing(first), Query.decreasing(first, rest))).as("list").isEqualTo(edges.getOrders()));

	}

	@Test void testParseSimpleFilters() {

		edges("{ \"filter\": { \">>\": 1 } }", shape, edges -> assertThat(filter(shape, minCount(1))).as("min count").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"<<\": 1 } }", shape, edges -> assertThat(filter(shape, maxCount(1))).as("max count").isEqualTo(edges.getShape()));


		edges("{ \"filter\": { \">=\": 1 } }", shape, edges -> assertThat(filter(shape, MinInclusive.minInclusive(One))).as("min inclusive").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"<=\": 1 } }", shape, edges -> assertThat(filter(shape, maxInclusive(One))).as("max inclusive").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \">\": 1 } }", shape, edges -> assertThat(filter(shape, MinExclusive.minExclusive(One))).as("min exclusive").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"<\": 1 } }", shape, edges -> assertThat(filter(shape, maxExclusive(One))).as("max exclusive").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"~\": \"words\" } }", shape, edges -> assertThat(filter(shape, like("words"))).as("like").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"*\": \"pattern\" } }", shape, edges -> assertThat(filter(shape, pattern("pattern"))).as("pattern").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \">#\": 123 } }", shape, edges -> assertThat(filter(shape, MinLength.minLength(123))).as("min length").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"#<\": 123 } }", shape, edges -> assertThat(filter(shape, maxLength(123))).as("max length").isEqualTo(edges.getShape()));


		edges("{ \"filter\": { \"@\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\" } }", shape, edges -> assertThat(filter(shape, clazz(RDF.NIL))).as("class").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"^\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#nil\" } }", shape, edges -> assertThat(filter(shape, datatype(RDF.NIL))).as("type").isEqualTo(edges.getShape()));


		edges("{ \"filter\": { \"?\": [] } }", shape, edges -> assertThat(filter(shape, and())).as("existential (empty)").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"?\": { \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" } } }", shape, edges -> assertThat(filter(shape, any(RDF.FIRST))).as("existential (singleton)").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"?\": [\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" },\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\" }\n"
				+"] } }", shape, edges -> assertThat(filter(shape, any(RDF.FIRST, RDF.REST))).as("existential (multiple)").isEqualTo(edges.getShape()));


		edges("{ \"filter\": { \"!\": [] } }", shape, edges -> assertThat(filter(shape, and())).as("universal (empty)").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"!\": { \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" } } }", shape, edges -> assertThat(filter(shape, All.all(RDF.FIRST))).as("universal (singleton)").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"!\": [\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#first\" },\n"
				+"\t{ \"this\": \"http://www.w3.org/1999/02/22-rdf-syntax-ns#rest\" }\n"
				+"] } }", shape, edges -> assertThat(filter(shape, All.all(RDF.FIRST, RDF.REST))).as("universal (multiple)").isEqualTo(edges.getShape()));

	}

	@Test void testParseStructuredFilters() {

		edges("{\n\t\"filter\": {}\n}", shape, edges -> assertThat(filter(shape, and())).as("empty filter").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"first.rest\": { \">=\": 1 } } }", shape, edges -> assertThat(filter(shape, trait(first, trait(rest, MinInclusive.minInclusive(One))))).as("nested filter").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"first.rest\": 1 } }", shape, edges -> assertThat(filter(shape, trait(first, trait(rest, any(One))))).as("nested filter singleton shorthand").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"first.rest\": [1, 10] } }", shape, edges -> assertThat(filter(shape, trait(first, trait(rest, any(One, Ten))))).as("nested filter multiple shorthand").isEqualTo(edges.getShape()));

	}


	@Test void testParseEdgesQuery() {

		edges("{ \"filter\": {}, \"offset\": 1, \"limit\": 2 }", shape, edges -> {

			assertThat(filter(shape, and())).as("shape").isEqualTo(edges.getShape());
			assertThat(1).as("offset").isEqualTo(edges.getOffset());
			assertThat(2).as("limit").isEqualTo(edges.getLimit());

		});

	}

	@Test void testParseStatsQuery() {

		stats("{ \"filter\": {}, \"stats\": \"\" }", shape, stats -> {

			assertThat(filter(shape, and())).as("shape").isEqualTo(stats.getShape());
			assertThat(Lists.list()).as("path").isEqualTo(stats.getPath());

		});

	}

	@Test void testParseItemsQuery() {

		items("{ \"filter\": {}, \"items\": \"\" }", shape, items -> {

			assertThat(filter(shape, and())).as("shape").isEqualTo(items.getShape());
			assertThat(Lists.list()).as("path").isEqualTo(items.getPath());

		});

	}


	@Test void testRejectNullFilters() {
		assertThatExceptionOfType(JsonException.class)
				.isThrownBy(() -> edges("{ \"filter\": null }", shape, edges -> {}));
	}

	@Test void testRejectNullValues() {
		assertThatExceptionOfType(JsonException.class)
				.isThrownBy(() -> edges("{ \"filter\": { \"first\": null } }", shape, edges -> {}));
	}

	@Test void testRejectReferencesOutsideShapeEnvelope() {
		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				edges("{ \"filter\": { \"nil\": { \">=\": 1 } } }", shape, edges -> {})
		);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void edges(final String json, final Shape shape, final Consumer<Edges> tester) {
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
		assertThat(new QueryParser(shape).parse(json).accept(probe)).as("query processed").isTrue();
	}

	private Shape filter(final Shape shape, final Shape filter) {
		return and(shape, com.metreeca.form.shapes.Test.test(Shape.filter(), filter));
	}

}
