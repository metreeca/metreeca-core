/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest;

import com.metreeca.tree.Query;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Optimizer;
import com.metreeca.tree.queries.Items;
import com.metreeca.tree.queries.Stats;
import com.metreeca.tree.queries.Terms;
import com.metreeca.tree.shapes.Field;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import javax.json.*;

import static com.metreeca.tree.Order.decreasing;
import static com.metreeca.tree.Order.increasing;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Any.any;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.In.in;
import static com.metreeca.tree.shapes.Like.like;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.tree.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.tree.shapes.MaxLength.maxLength;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.MinExclusive.minExclusive;
import static com.metreeca.tree.shapes.MinInclusive.minInclusive;
import static com.metreeca.tree.shapes.MinLength.minLength;
import static com.metreeca.tree.shapes.Pattern.pattern;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import static java.util.Arrays.asList;


final class QueryParserTest {

	private static final Long One=1L;
	private static final Long Ten=10L;

	private static final Shape shape=field("head", field("tail", and()));


	private void items(final String query, final Shape shape, final Consumer<Items> tester) {
		query(query, shape, new TestQueryProbe() {

			@Override public Boolean probe(final Items items) {

				tester.accept(items);

				return true;
			}

		});
	}

	private void terms(final String query, final Shape shape, final Consumer<Terms> tester) {
		query(query, shape, new TestQueryProbe() {

			@Override public Boolean probe(final Terms terms) {

				tester.accept(terms);

				return true;
			}

		});
	}

	private void stats(final String query, final Shape shape, final Consumer<Stats> tester) {
		query(query, shape, new TestQueryProbe() {

			@Override public Boolean probe(final Stats stats) {

				tester.accept(stats);

				return true;
			}

		});
	}


	private void query(final String query, final Shape shape, final Query.Probe<Boolean> probe) {
		assertThat(parse(query, shape).map(probe))
				.as("query processed")
				.isTrue();
	}

	private Query parse(final String query, final Shape shape) {
		return new QueryParser("app:/", shape, new TestFormat())
				.parse(query.replace('\'', '"'));
	}


	private Shape filter(final Shape shape, final Shape filter) {
		return and(shape, Shape.filter().then(filter)).map(new Optimizer());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseEmptyString() {
		items("", shape, items -> {

			assertThat(shape).as("base shape").isEqualTo(items.getShape());
			assertThat(items.getOrders()).as("no orders").isEmpty();
			assertThat(items.getOffset()).as("no offset").isEqualTo(0);
			assertThat(items.getLimit()).as("no limit").isEqualTo(0);

		});
	}

	@Test void testParseEmptyObject() {
		items("{}", shape, items -> {

			assertThat(items.getShape()).as("base shape").isEqualTo(shape);
			assertThat(items.getOrders()).as("no orders").isEmpty();
			assertThat(items.getOffset()).as("no offset").isEqualTo(0);
			assertThat(items.getLimit()).as("no limit").isEqualTo(0);

		});
	}


	@Test void testParsePaths() {

		stats("{ '_stats': '' }", shape, stats -> assertThat(stats.getPath())
				.as("empty path")
				.isEmpty()
		);

		stats("{ '_stats': 'head' }", shape, stats -> assertThat(stats.getPath())
				.as("direct naked step")
				.containsExactly("head")
		);

		stats("{ '_stats': 'head.tail' }", shape, stats -> assertThat(stats.getPath())
				.as("naked dot path")
				.containsExactly("head", "tail")
		);

		stats("{ '_stats': 'head.tail' }", shape, stats -> assertThat(stats.getPath())
				.as("naked dot path")
				.containsExactly("head", "tail")
		);

	}

	@Test void testRejectMalformedPaths() {
		Assertions.assertThatThrownBy(() -> parse("{ '_order': '---' }", and()))
				.isInstanceOf(RuntimeException.class);
	}

	@Test void testParseSortingCriteria() {

		items("{ '_order': '' }", shape, items -> assertThat(items.getOrders())
				.as("empty path")
				.containsExactly(increasing())
		);

		items("{ '_order': '+' }", shape, items -> assertThat(items.getOrders())
				.as("empty path increasing")
				.containsExactly(increasing())
		);

		items("{ '_order': '-' }", shape, items -> assertThat(items.getOrders())
				.as("empty path decreasing")
				.containsExactly(decreasing())
		);

		items("{ '_order': 'head.tail' }", shape, items -> assertThat(items.getOrders())
				.as("path")
				.containsExactly(increasing("head", "tail"))
		);

		items("{ '_order': '+head.tail' }", shape, items -> assertThat(items.getOrders())
				.as("path increasing")
				.containsExactly(increasing("head", "tail"))
		);

		items("{ '_order': '-head.tail' }", shape, items -> assertThat(items.getOrders())
				.as("path decreasing")
				.containsExactly(decreasing("head", "tail")));

		items("{ '_order': [] }", shape, items -> assertThat(items.getOrders()).
				as("empty list")
				.isEmpty()
		);

		items("{ '_order': ['+head', '-head.tail'] }", shape, items -> assertThat(items.getOrders())
				.as("list")
				.containsExactly(increasing("head"), decreasing("head", "tail"))
		);

	}


	@Test void testParseRootFilters() {

		items("{ '@': 'class' }", shape, items -> assertThat(items.getShape())
				.as("class")
				.isEqualTo(filter(shape, clazz("class")))
		);

		items("{ '^': 'datatype' }", shape, items -> assertThat(items.getShape())
				.as("type")
				.isEqualTo(filter(shape, datatype("datatype")))
		);


		items("{ '>': 1 }", shape, items -> assertThat(items.getShape())
				.as("min exclusive")
				.isEqualTo(filter(shape, minExclusive(One)))
		);

		items("{ '<': 1 }", shape, items -> assertThat(items.getShape())
				.as("max exclusive")
				.isEqualTo(filter(shape, maxExclusive(One)))
		);

		items("{ '>=': 1 }", shape, items -> assertThat(items.getShape())
				.as("min inclusive")
				.isEqualTo(filter(shape, minInclusive(One)))
		);

		items("{ '<=': 1 }", shape, items -> assertThat(items.getShape())
				.as("max inclusive")
				.isEqualTo(filter(shape, maxInclusive(One)))
		);


		items("{ '$>': 123 }", shape, items -> assertThat(items.getShape())
				.as("min length")
				.isEqualTo(filter(shape, minLength(123)))
		);

		items("{ '$<': 123 }", shape, items -> assertThat(items.getShape())
				.as("max length")
				.isEqualTo(filter(shape, maxLength(123)))
		);

		items("{ '*': 'pattern' }", shape, items -> assertThat(items.getShape())
				.as("pattern")
				.isEqualTo(filter(shape, pattern("pattern")))
		);

		items("{ '~': 'words' }", shape, items -> assertThat(items.getShape())
				.as("like")
				.isEqualTo(filter(shape, like("words")))
		);


		items("{ '#>': 1 }", shape, items -> assertThat(items.getShape())
				.as("min count")
				.isEqualTo(filter(shape, minCount(1)))
		);

		items("{ '#<': 1 }", shape, items -> assertThat(items.getShape())
				.as("max count")
				.isEqualTo(filter(shape, maxCount(1)))
		);


		items("{ '%': [] }", shape, items -> assertThat(items.getShape())
				.as("in (empty)")
				.isEqualTo(filter(shape, in()))
		);

		items("{ '%': 'head' }", shape, items -> assertThat(items.getShape())
				.as("in (singleton)")
				.isEqualTo(filter(shape, in("head")))
		);

		items("{ '%': ['head', 'tail'] }", shape, items -> assertThat(items.getShape())
				.as("in (multiple)")
				.isEqualTo(filter(shape, in("head", "tail")))
		);


		items("{ '!': [] }", shape, items -> assertThat(items.getShape())
				.as("universal (empty)")
				.isEqualTo(filter(shape, all()))
		);

		items("{ '!': 'head' }", shape, items -> assertThat(items.getShape())
				.as("universal (singleton)")
				.isEqualTo(filter(shape, all("head")))
		);

		items("{ '!': ['head', 'tail'] }", shape, items -> assertThat(items.getShape())
				.as("universal (multiple)")
				.isEqualTo(filter(shape, all("head", "tail")))
		);


		items("{ '?': [] }", shape, items -> assertThat(items.getShape())
				.as("existential (empty)")
				.isEqualTo(filter(shape, any()))
		);

		items("{ '?': 'head' }", shape, items -> assertThat(items.getShape())
				.as("existential (singleton)")
				.isEqualTo(filter(shape, any("head")))
		);

		items("{ '?': ['head', 'tail'] }", shape, items -> assertThat(items.getShape())
				.as("existential (multiple)")
				.isEqualTo(filter(shape, any("head", "tail")))
		);

	}

	@Test void testParsePathFilters() {

		items("{ '>= head.tail': 1 }", shape, items -> assertThat(items.getShape())
				.as("nested filter")
				.isEqualTo(filter(shape, field("head", field("tail", minInclusive(One)))))
		);

		items("{ 'head.tail': 1 }", shape, items -> assertThat(items.getShape())
				.as("nested filter singleton shorthand")
				.isEqualTo(filter(shape, field("head", field("tail", any(One)))))
		);

		items("{ 'head.tail': [1, 10] }", shape, items -> assertThat(items.getShape())
				.as("nested filter multiple shorthand")
				.isEqualTo(filter(shape, field("head", field("tail", any(One, Ten)))))
		);

	}

	@Test void testParseShapedFilters() {

		final Field shape=field("value", datatype("type"));

		items("{ 'value': '4' }", shape, items -> assertThat(items.getShape())
				.as("typed value")
				.isEqualTo(filter(shape, field("value", any("4^type"))))
		);
	}


	@Test void testIgnoreNullFilters() {

		items("{ '>': null }", shape, items -> assertThat(items.getShape()).isEqualTo(shape));
		items("{ 'head': null }", shape, items -> assertThat(items.getShape()).isEqualTo(shape));

	}


	@Test void testRejectReferencesOutsideShapeEnvelope() {

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				terms("{ '_terms': 'nil' }", shape, items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				stats("{ '_stats': 'nil' }", shape, stats -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ '_order': 'nil' }", shape, items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ '>= nil': 1 }", shape, items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ 'head.nil': 1 }", shape, items -> {})
		);

	}

	@Test void testRejectReferencesForEmptyShape() {

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				terms("{ '_terms': 'head' }", and(), items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				stats("{ '_stats': 'head' }", and(), stats -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ '_order': 'nil' }", and(), items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ '>= nil': 1 }", and(), items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ 'head.nil': 1 }", and(), items -> {})
		);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParsePlainQuery() {

		items("head=x&head.tail=y&head.tail=w+z", shape, items -> assertThat(items.getShape())
				.isEqualTo(filter(shape, field("head", and(
						any("x"),
						field("tail", any("y", "w z"))
				)))));

		items("head=x&head.tail=y&_order=-head.tail&_order=head&_offset=1&_limit=2", shape, items -> {

			assertThat(items.getOrders())
					.containsExactly(decreasing("head", "tail"), increasing("head"));

			assertThat(items.getOffset())
					.isEqualTo(1);

			assertThat(items.getLimit())
					.isEqualTo(2);

			assertThat(items.getShape())
					.isEqualTo(filter(shape, field("head", and(
							any("x"),
							field("tail", any("y"))
					))));
		});

	}

	@Test void testParseItemsQuery() {

		items("{ '_offset': 1, '_limit': 2 }", shape, items -> {

			assertThat(items.getShape()).as("shape").isEqualTo(filter(shape, and()));
			assertThat(items.getOffset()).as("offset").isEqualTo(1);
			assertThat(items.getLimit()).as("limit").isEqualTo(2);

		});

	}

	@Test void testParseTermsQuery() {

		terms("{ '_terms': 'head.tail' }", shape, terms -> {

			assertThat(filter(shape, and()))
					.as("shape")
					.isEqualTo(terms.getShape());

			assertThat(terms.getPath())
					.as("path")
					.containsExactly("head", "tail");

		});

	}

	@Test void testParseStatsQuery() {

		stats("{ '_stats': 'head.tail' }", shape, stats -> {

			assertThat(filter(shape, and()))
					.as("shape")
					.isEqualTo(stats.getShape());

			assertThat(stats.getPath())
					.as("path")
					.containsExactly("head", "tail");

		});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseFormBasedQueries() {

		items("~head=keyword", shape, items -> {

			assertThat(items.getShape()).isEqualTo(filter(shape, field("head", like("keyword"))));

		});

		items("_order=%2Bhead.tail&_offset=1&_limit=2", shape, items -> {

			assertThat(items.getOrders()).containsExactly(increasing("head", "tail"));
			assertThat(items.getOffset()).isEqualTo(1L);
			assertThat(items.getLimit()).isEqualTo(2L);

		});

		terms("_terms=head.tail", shape, terms -> {

			assertThat(filter(shape, and())).isEqualTo(terms.getShape());
			assertThat(terms.getPath()).containsExactly("head", "tail");

		});

		stats("_stats=head.tail", shape, stats -> {

			assertThat(filter(shape, and())).isEqualTo(stats.getShape());
			assertThat(stats.getPath()).containsExactly("head", "tail");

		});

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestFormat implements Format<Object> {

		@Override public List<Object> path(final String base, final Shape shape, final String path) {
			return path.isEmpty() ? Collections.emptyList() : asList((Object[])path.split("\\."));
		}

		@Override public Object value(final String base, final Shape shape, final JsonValue value) {
			return value.equals(JsonValue.TRUE) ? true
					: value.equals(JsonValue.FALSE) ? false
					: value instanceof JsonNumber && ((JsonNumber)value).isIntegral() ? ((JsonNumber)value).longValue()
					: value instanceof JsonNumber ? ((JsonNumber)value).doubleValue()
					: value instanceof JsonString ? ((JsonString)value).getString()+datatype(shape).map(t -> "^"+t).orElse("")
					: null;			}
	}


	private abstract static class TestQueryProbe implements Query.Probe<Boolean> {

		@Override public Boolean probe(final Items items) { return false; }

		@Override public Boolean probe(final Terms terms) { return false; }

		@Override public Boolean probe(final Stats stats) { return false; }

	}

}