/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.formats;

import com.metreeca.json.Query;
import com.metreeca.json.Shape;
import com.metreeca.json.queries.*;
import com.metreeca.json.shapes.Guard;
import com.metreeca.json.shapes.Range;

import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.JsonException;
import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static com.metreeca.json.Order.decreasing;
import static com.metreeca.json.Order.increasing;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Like.like;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.MinExclusive.minExclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.MinLength.minLength;
import static com.metreeca.json.shapes.Pattern.pattern;
import static java.util.Collections.emptyMap;
import static org.assertj.core.api.Assertions.*;

final class JSONLDParserTest {

	private static final Value One=literal(integer(1));
	private static final Value Ten=literal(integer(10));

	private static final Shape shape=field(RDF.FIRST, field(RDF.REST, and()));


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
		return new JSONLDParser(iri(), shape, emptyMap())
				.parse(query.replace('\'', '"'));
	}


	private Shape filter(final Shape shape, final Shape filter) {
		return and(shape, Guard.filter().then(filter));
	}


	@Nested final class Encodings {

		@Test void testParseEmptyString() {
			items("", shape, items -> {

				assertThat(shape).as("base shape").isEqualTo(items.shape());
				assertThat(items.orders()).as("no orders").isEmpty();
				assertThat(items.offset()).as("no offset").isEqualTo(0);
				assertThat(items.limit()).as("no limit").isEqualTo(0);

			});
		}

		@Test void testParseEmptyObject() {
			items("{}", shape, items -> {

				assertThat(items.shape()).as("base shape").isEqualTo(shape);
				assertThat(items.orders()).as("no orders").isEmpty();
				assertThat(items.offset()).as("no offset").isEqualTo(0);
				assertThat(items.limit()).as("no limit").isEqualTo(0);

			});
		}

		@Test void testParseFormBasedQueries() {

			items("~first=keyword", shape, items -> {

				assertThat(items.shape()).isEqualTo(filter(shape, field(RDF.FIRST, like("keyword", true))));

			});

			items("_order=%2Bfirst.rest&_offset=1&_limit=2", shape, items -> {

				assertThat(items.orders()).containsExactly(increasing(RDF.FIRST, RDF.REST));
				assertThat(items.offset()).isEqualTo(1L);
				assertThat(items.limit()).isEqualTo(2L);

			});

			terms("_terms=first.rest", shape, terms -> {

				assertThat(filter(shape, and())).isEqualTo(terms.shape());
				assertThat(terms.path()).containsExactly(RDF.FIRST, RDF.REST);

			});

			stats("_stats=first.rest", shape, stats -> {

				assertThat(filter(shape, and())).isEqualTo(stats.shape());
				assertThat(stats.path()).containsExactly(RDF.FIRST, RDF.REST);

			});

		}

	}

	@Nested final class Paths {

		@Test void testParseEmptyPath() {
			stats("{ '_stats': '' }", shape, stats -> assertThat(stats.path())
					.isEmpty()
			);
		}

		@Test void testParseDirectSteps() {
			stats("{ '_stats': 'first' }", field(RDF.FIRST), stats -> assertThat(stats.path())
					.containsExactly(RDF.FIRST)
			);
		}

		@Test void testParseInverseSteps() { // !!! inverse?
			stats("{ '_stats': 'firstOf' }", field(inverse(RDF.FIRST)), stats -> assertThat(stats.path())
					.containsExactly(inverse(RDF.FIRST))
			);
		}

		@Test void testParseMultipleSteps() {

			stats("{ '_stats': 'first.rest' }", field(RDF.FIRST, field(RDF.REST)),
					stats -> assertThat(stats.path()).containsExactly(RDF.FIRST, RDF.REST)
			);

			stats("{ '_stats': 'firstOf.rest' }", field(inverse(RDF.FIRST), field(RDF.REST)),
					stats -> assertThat(stats.path()).containsExactly(inverse(RDF.FIRST), RDF.REST)
			);

		}

		@Test void testParseSortingCriteria() {

			items("{ '_order': '' }", shape, items -> assertThat(items.orders())
					.as("empty path")
					.containsExactly(increasing())
			);

			items("{ '_order': '+' }", shape, items -> assertThat(items.orders())
					.as("empty path increasing")
					.containsExactly(increasing())
			);

			items("{ '_order': '-' }", shape, items -> assertThat(items.orders())
					.as("empty path decreasing")
					.containsExactly(decreasing())
			);

			items("{ '_order': 'first.rest' }", shape, items -> assertThat(items.orders())
					.as("path")
					.containsExactly(increasing(RDF.FIRST, RDF.REST))
			);

			items("{ '_order': '+first.rest' }", shape, items -> assertThat(items.orders())
					.as("path increasing")
					.containsExactly(increasing(RDF.FIRST, RDF.REST))
			);

			items("{ '_order': '-first.rest' }", shape, items -> assertThat(items.orders())
					.as("path decreasing")
					.containsExactly(decreasing(RDF.FIRST, RDF.REST)));

			items("{ '_order': [] }", shape, items -> assertThat(items.orders()).
					as("empty list")
					.isEmpty()
			);

			items("{ '_order': ['+first', '-first.rest'] }", shape, items -> assertThat(items.orders())
					.as("list")
					.containsExactly(increasing(RDF.FIRST), decreasing(RDF.FIRST, RDF.REST))
			);

		}


		@Test void testReportMalformedPaths() {
			assertThatThrownBy(() -> parse("{ '_order': '---' }", and())).isInstanceOf(JsonException.class);
		}

		@Test void testReportReferencesOutsideShapeEnvelope() {

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
					items("{ 'first.nil': 1 }", shape, items -> {})
			);

		}

		@Test void testReportReferencesForEmptyShape() {

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					terms("{ '_terms': 'first' }", and(), items -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					stats("{ '_stats': 'first' }", and(), stats -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '_order': 'nil' }", and(), items -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '>= nil': 1 }", and(), items -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ 'first.nil': 1 }", and(), items -> {})
			);

		}

	}

	@Nested final class Filters {

		@Test void testParseRootFilters() {

			items("{ '@': '"+RDF.TYPE+"' }", shape, items -> assertThat(items.shape())
					.as("class")
					.isEqualTo(filter(shape, clazz(RDF.TYPE)))
			);

			items("{ '^': '"+XSD.DATE+"' }", shape, items -> assertThat(items.shape())
					.as("type")
					.isEqualTo(filter(shape, datatype(XSD.DATE)))
			);


			items("{ '>': 1 }", shape, items -> assertThat(items.shape())
					.as("min exclusive")
					.isEqualTo(filter(shape, minExclusive(One)))
			);

			items("{ '<': 1 }", shape, items -> assertThat(items.shape())
					.as("max exclusive")
					.isEqualTo(filter(shape, maxExclusive(One)))
			);

			items("{ '>=': 1 }", shape, items -> assertThat(items.shape())
					.as("min inclusive")
					.isEqualTo(filter(shape, minInclusive(One)))
			);

			items("{ '<=': 1 }", shape, items -> assertThat(items.shape())
					.as("max inclusive")
					.isEqualTo(filter(shape, maxInclusive(One)))
			);


			items("{ '$>': 123 }", shape, items -> assertThat(items.shape())
					.as("min length")
					.isEqualTo(filter(shape, minLength(123)))
			);

			items("{ '$<': 123 }", shape, items -> assertThat(items.shape())
					.as("max length")
					.isEqualTo(filter(shape, maxLength(123)))
			);

			items("{ '*': 'pattern' }", shape, items -> assertThat(items.shape())
					.as("pattern")
					.isEqualTo(filter(shape, pattern("pattern")))
			);

			items("{ '~': 'words' }", shape, items -> assertThat(items.shape())
					.as("like")
					.isEqualTo(filter(shape, like("words", true)))
			);


			items("{ '#>': 1 }", shape, items -> assertThat(items.shape())
					.as("min count")
					.isEqualTo(filter(shape, minCount(1)))
			);

			items("{ '#<': 1 }", shape, items -> assertThat(items.shape())
					.as("max count")
					.isEqualTo(filter(shape, maxCount(1)))
			);


			items("{ '%': [] }", shape, items -> assertThat(items.shape())
					.as("in (empty)")
					.isEqualTo(filter(shape, Range.range()))
			);

			items("{ '%': 'first' }", shape, items -> assertThat(items.shape())
					.as("in (singleton)")
					.isEqualTo(filter(shape, Range.range(literal("first"))))
			);

			items("{ '%': ['first', 'rest'] }", shape, items -> assertThat(items.shape())
					.as("in (multiple)")
					.isEqualTo(filter(shape, Range.range(literal("first"), literal("rest"))))
			);


			items("{ '!': [] }", shape, items -> assertThat(items.shape())
					.as("universal (empty)")
					.isEqualTo(filter(shape, all()))
			);

			items("{ '!': 'first' }", shape, items -> assertThat(items.shape())
					.as("universal (singleton)")
					.isEqualTo(filter(shape, all(literal("first"))))
			);

			items("{ '!': ['first', 'rest'] }", shape, items -> assertThat(items.shape())
					.as("universal (multiple)")
					.isEqualTo(filter(shape, all(literal("first"), literal("rest"))))
			);


			items("{ '?': [] }", shape, items -> assertThat(items.shape())
					.as("existential (empty)")
					.isEqualTo(filter(shape, any()))
			);

			items("{ '?': 'first' }", shape, items -> assertThat(items.shape())
					.as("existential (singleton)")
					.isEqualTo(filter(shape, any(literal("first"))))
			);

			items("{ '?': ['first', 'rest'] }", shape, items -> assertThat(items.shape())
					.as("existential (multiple)")
					.isEqualTo(filter(shape, any(literal("first"), literal("rest"))))
			);

		}

		@Test void testParsePathFilters() {

			items("{ '>= first.rest': 1 }", shape, items -> assertThat(items.shape())
					.as("nested filter")
					.isEqualTo(filter(shape, field(RDF.FIRST, field(RDF.REST, minInclusive(One)))))
			);

			items("{ 'first.rest': 1 }", shape, items -> assertThat(items.shape())
					.as("nested filter singleton shorthand")
					.isEqualTo(filter(shape, field(RDF.FIRST, field(RDF.REST, any(One)))))
			);

			items("{ 'first.rest': [1, 10] }", shape, items -> assertThat(items.shape())
					.as("nested filter multiple shorthand")
					.isEqualTo(filter(shape, field(RDF.FIRST, field(RDF.REST, any(One, Ten)))))
			);

		}

		@Test void testParseShapedFilters() {

			final Shape shape=field(RDF.VALUE, datatype(XSD.LONG));

			items("{ 'value': '4' }", shape, items -> assertThat(items.shape())
					.as("typed value")
					.isEqualTo(filter(shape, field(RDF.VALUE, any(literal("4", XSD.LONG)))))
			);
		}

		@Test void testIgnoreNullFilters() {

			items("{ '>': null }", shape, items -> assertThat(items.shape()).isEqualTo(shape));
			items("{ 'first': null }", shape, items -> assertThat(items.shape()).isEqualTo(shape));

		}

	}

	@Nested final class Queries {

		@Test void testParsePlainQuery() {

			items("first=x&first.rest=y&first.rest=w+z", shape, items -> assertThat(items.shape())
					.isEqualTo(filter(shape, field(RDF.FIRST, and(
							any(literal("x")),
							field(RDF.REST, any(literal("y"), literal("w z")))
					)))));

			items("first=x&first.rest=y&_order=-first.rest&_order=first&_offset=1&_limit=2", shape, items -> {

				assertThat(items.orders())
						.containsExactly(decreasing(RDF.FIRST, RDF.REST), increasing(RDF.FIRST));

				assertThat(items.offset())
						.isEqualTo(1);

				assertThat(items.limit())
						.isEqualTo(2);

				assertThat(items.shape())
						.isEqualTo(filter(shape, field(RDF.FIRST, and(
								any(literal("x")),
								field(RDF.REST, any(literal("y")))
						))));
			});

		}

		@Test void testParseItemsQuery() {

			items("{ '_offset': 1, '_limit': 2 }", shape, items -> {

				assertThat(items.shape()).as("shape").isEqualTo(filter(shape, and()));
				assertThat(items.offset()).as("offset").isEqualTo(1);
				assertThat(items.limit()).as("limit").isEqualTo(2);

			});

		}

		@Test void testParseTermsQuery() {

			terms("{ '_terms': 'first.rest' }", shape, terms -> {

				assertThat(filter(shape, and()))
						.as("shape")
						.isEqualTo(terms.shape());

				assertThat(terms.path())
						.as("path")
						.containsExactly(RDF.FIRST, RDF.REST);

			});

		}

		@Test void testParseStatsQuery() {

			stats("{ '_stats': 'first.rest' }", shape, stats -> {

				assertThat(filter(shape, and()))
						.as("shape")
						.isEqualTo(stats.shape());

				assertThat(stats.path())
						.as("path")
						.containsExactly(RDF.FIRST, RDF.REST);

			});

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private abstract static class TestQueryProbe extends Query.Probe<Boolean> {

		@Override public Boolean probe(final Items items) { return false; }

		@Override public Boolean probe(final Terms terms) { return false; }

		@Override public Boolean probe(final Stats stats) { return false; }

	}

}
