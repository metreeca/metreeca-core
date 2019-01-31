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
import com.metreeca.form.things.Lists;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import javax.json.JsonException;

import static com.metreeca.form.Order.decreasing;
import static com.metreeca.form.Order.increasing;
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
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.inverse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.math.BigInteger.ONE;
import static java.math.BigInteger.TEN;


final class QueryParserTest {

	private static final Literal One=Values.literal(ONE);
	private static final Literal Ten=Values.literal(TEN);

	private static final Shape shape=and(
			field(RDF.FIRST, Field.field(RDF.REST)),
			field(inverse(RDF.FIRST), Field.field(RDF.REST))
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

		stats("{ \"stats\": \"\" }", shape, stats -> assertThat(stats.getPath())
				.as("empty path")
				.isEqualTo(Lists.<IRI>list()));

		stats("{ \"stats\": \""+RDF.FIRST+"\" }", shape, stats -> assertThat(stats.getPath())
				.as("direct naked iri")
				.isEqualTo(list(RDF.FIRST)));

		stats("{ \"stats\": \"^"+RDF.FIRST+"\" }", shape, stats -> assertThat(stats.getPath())
				.as("inverse naked iri")
				.isEqualTo(list(inverse(RDF.FIRST))));

		stats("{ \"stats\": \"<"+RDF.FIRST+">\" }", shape, stats -> assertThat(stats.getPath())
				.as("direct brackets iri")
				.isEqualTo(list(RDF.FIRST)));

		stats("{ \"stats\": \"^<"+RDF.FIRST+">\" }", shape, stats -> assertThat(stats.getPath())
				.as("inverse brackets iri")
				.isEqualTo(list(inverse(RDF.FIRST))));

		stats("{ \"stats\": \"<"+RDF.FIRST+">/<"+RDF.REST+">\" }", shape, stats -> assertThat(stats.getPath())
				.as("iri slash path")
				.isEqualTo(list(RDF.FIRST, RDF.REST)));

		stats("{ \"stats\": \"first\" }", shape, stats -> assertThat(stats.getPath())
				.as("direct alias")
				.isEqualTo(list(RDF.FIRST)));

		stats("{ \"stats\": \"firstOf\" }", shape, stats -> assertThat(stats.getPath())
				.as("inverse alias")
				.isEqualTo(list(inverse(RDF.FIRST))));

		stats("{ \"stats\": \"first/rest\" }", shape, stats -> assertThat(stats.getPath())
				.as("alias slash path")
				.isEqualTo(list(RDF.FIRST, RDF.REST)));

		stats("{ \"stats\": \"firstOf.rest\" }", shape, stats -> assertThat(stats.getPath())
				.as("alias dot path")
				.isEqualTo(list(inverse(RDF.FIRST), RDF.REST)));

	}


	@Test void testParseSortingCriteria() {

		edges("{ \"order\": \"\" }", shape, edges -> assertThat(list(increasing())).as("empty path").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"+\" }", shape, edges -> assertThat(list(increasing())).as("empty path increasing").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"-\" }", shape, edges -> assertThat(list(decreasing())).as("empty path decreasing").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"first.rest\" }", shape, edges -> assertThat(list(increasing(RDF.FIRST, RDF.REST))).as("path").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"+first.rest\" }", shape, edges -> assertThat(list(increasing(RDF.FIRST, RDF.REST))).as("path increasing").isEqualTo(edges.getOrders()));

		edges("{ \"order\": \"-first.rest\" }", shape, edges -> assertThat(list(decreasing(RDF.FIRST, RDF.REST))).as("path decreasing").isEqualTo(edges.getOrders()));

		edges("{ \"order\": [] }", shape, edges -> assertThat(list()).as("empty list").isEqualTo(edges.getOrders()));

		edges("{ \"order\": [\"+first\", \"-first.rest\"] }", shape, edges -> assertThat(list(increasing(RDF.FIRST), decreasing(RDF.FIRST, RDF.REST))).as("list").isEqualTo(edges.getOrders()));

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

		edges("{ \"filter\": { \"first.rest\": { \">=\": 1 } } }", shape, edges -> assertThat(filter(shape, field(RDF.FIRST, field(RDF.REST, MinInclusive.minInclusive(One))))).as("nested filter").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"first.rest\": 1 } }", shape, edges -> assertThat(filter(shape, field(RDF.FIRST, field(RDF.REST, any(One))))).as("nested filter singleton shorthand").isEqualTo(edges.getShape()));

		edges("{ \"filter\": { \"first.rest\": [1, 10] } }", shape, edges -> assertThat(filter(shape, field(RDF.FIRST, field(RDF.REST, any(One, Ten))))).as("nested filter multiple shorthand").isEqualTo(edges.getShape()));

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
			assertThat(stats.getPath())
					.as("path")
					.isEqualTo(Lists.<IRI>list());

		});

	}

	@Test void testParseItemsQuery() {

		items("{ \"filter\": {}, \"items\": \"\" }", shape, items -> {

			assertThat(filter(shape, and())).as("shape").isEqualTo(items.getShape());
			assertThat(items.getPath())
					.as("path")
					.isEqualTo(Lists.<IRI>list());

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

			@Override public Boolean probe(final Edges edges) {

				tester.accept(edges);

				return true;
			}

			@Override public Boolean probe(final Stats stats) {
				return false;
			}

			@Override public Boolean probe(final Items items) {
				return false;
			}

		});
	}

	private void stats(final String json, final Shape shape, final Consumer<Stats> tester) {
		test(json, shape, new Query.Probe<Boolean>() {

			@Override public Boolean probe(final Edges edges) {
				return false;
			}

			@Override public Boolean probe(final Stats stats) {

				tester.accept(stats);

				return true;
			}

			@Override public Boolean probe(final Items items) {
				return false;
			}
		});
	}

	private void items(final String json, final Shape shape, final Consumer<Items> tester) {
		test(json, shape, new Query.Probe<Boolean>() {

			@Override public Boolean probe(final Edges edges) {
				return false;
			}

			@Override public Boolean probe(final Stats stats) {
				return false;
			}

			@Override public Boolean probe(final Items items) {

				tester.accept(items);

				return true;
			}

		});
	}


	private void test(final String json, final Shape shape, final Query.Probe<Boolean> probe) {
		assertThat(new QueryParser(shape).parse(json).map(probe)).as("query processed").isTrue();
	}

	private Shape filter(final Shape shape, final Shape filter) {
		return and(shape, When.when(Shape.filter(), filter));
	}

}
