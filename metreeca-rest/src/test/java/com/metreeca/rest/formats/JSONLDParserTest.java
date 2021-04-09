/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest.formats;

import com.metreeca.json.Query;
import com.metreeca.json.Shape;
import com.metreeca.json.queries.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import javax.json.JsonException;

import static com.metreeca.json.Order.decreasing;
import static com.metreeca.json.Order.increasing;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.Like.like;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.MinExclusive.minExclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.MinLength.minLength;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.Stem.stem;

import static org.assertj.core.api.Assertions.*;

import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

final class JSONLDParserTest {

	private static final IRI x=item("x");

	private static final Value One=literal(1);
	private static final Value Ten=literal(10);

	private static final Shape first=field(RDF.FIRST);
	private static final Shape rest=field(RDF.REST);

	private static final Shape shape=field(RDF.FIRST, rest);


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
		return new JSONLDParser(item(""), shape, emptyMap()).parse(query
				.replace('\'', '"')
				.replace("\\\"", "'")
		);
	}


	private Shape filtered(final Shape shape, final Shape filter) {
		return and(shape, filter(filter));
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

				assertThat(items.shape()).isEqualTo(filtered(shape, field(RDF.FIRST, like("keyword", true))));

			});

			items(".order=%2Bfirst.rest&.offset=1&.limit=2", shape, items -> {

				assertThat(items.orders()).containsExactly(increasing(RDF.FIRST, RDF.REST));
				assertThat(items.offset()).isEqualTo(1L);
				assertThat(items.limit()).isEqualTo(2L);

			});

			terms(".terms=first.rest", shape, terms -> {

				assertThat(filtered(shape, and())).isEqualTo(terms.shape());
				assertThat(terms.path()).containsExactly(RDF.FIRST, RDF.REST);

			});

			stats(".stats=first.rest", shape, stats -> {

				assertThat(filtered(shape, and())).isEqualTo(stats.shape());
				assertThat(stats.path()).containsExactly(RDF.FIRST, RDF.REST);

			});

		}

	}

	@Nested final class Paths {

		@Test void testParseEmptyPath() {
			stats("{ '.stats': '' }", shape, stats -> assertThat(stats.path())
					.isEmpty()
			);
		}

		@Test void testParseDirectSteps() {
			stats("{ '.stats': 'first' }", first, stats -> assertThat(stats.path())
					.containsExactly(RDF.FIRST)
			);
		}

		@Test void testParseInverseSteps() { // !!! inverse?
			stats("{ '.stats': 'firstOf' }", field(inverse(RDF.FIRST)), stats -> assertThat(stats.path())
					.containsExactly(inverse(RDF.FIRST))
			);
		}

		@Test void testParseMultipleSteps() {

			stats("{ '.stats': 'first.rest' }", field(RDF.FIRST, rest), stats -> assertThat(stats.path())
					.containsExactly(RDF.FIRST, RDF.REST)
			);

			stats("{ '.stats': 'firstOf.rest' }", field(inverse(RDF.FIRST), rest),
					stats -> assertThat(stats.path())
							.containsExactly(inverse(RDF.FIRST), RDF.REST)
			);

		}

		@Test void testParseSortingCriteria() {

			items("{ '.order': '' }", shape, items -> assertThat(items.orders())
					.as("empty path")
					.containsExactly(increasing())
			);

			items("{ '.order': '+' }", shape, items -> assertThat(items.orders())
					.as("empty path increasing")
					.containsExactly(increasing())
			);

			items("{ '.order': '-' }", shape, items -> assertThat(items.orders())
					.as("empty path decreasing")
					.containsExactly(decreasing())
			);

			items("{ '.order': 'first.rest' }", shape, items -> assertThat(items.orders())
					.containsExactly(increasing(RDF.FIRST, RDF.REST))
			);

			items("{ '.order': '+first.rest' }", shape, items -> assertThat(items.orders())
					.as("path increasing")
					.containsExactly(increasing(RDF.FIRST, RDF.REST))
			);

			items("{ '.order': '-first.rest' }", shape, items -> assertThat(items.orders())
					.as("path decreasing")
					.containsExactly(decreasing(RDF.FIRST, RDF.REST)));

			items("{ '.order': [] }", shape, items -> assertThat(items.orders()).
					as("empty list")
					.isEmpty()
			);

			items("{ '.order': ['+first', '-first.rest'] }", shape, items -> assertThat(items.orders())
					.as("list")
					.containsExactly(increasing(RDF.FIRST), decreasing(RDF.FIRST, RDF.REST))
			);

		}


		@Test void testTraverseLinkPaths() {

			terms("{ '.terms' : 'first.rest' }", link(OWL.SAMEAS, field(RDF.FIRST, field(RDF.REST))),
					terms -> assertThat(terms.path()).containsExactly(

					RDF.FIRST, RDF.REST

			));

			terms("{ '.terms' : 'first.rest' }", field(RDF.FIRST, link(OWL.SAMEAS, field(RDF.REST))),
					terms -> assertThat(terms.path()).containsExactly(

					RDF.FIRST, RDF.REST

			));

			terms("{ '.terms' : 'first.rest' }", field(RDF.FIRST, field(RDF.REST, link(OWL.SAMEAS))),
					terms -> assertThat(terms.path()).containsExactly(

					RDF.FIRST, RDF.REST

			));

		}

		@Test void testTraverseLinkFilters() {

			items("{ 'first.rest': 'any' }", link(OWL.SAMEAS, field(RDF.FIRST, field(RDF.REST))),
					items -> assertThat(items.shape()).isEqualTo(filtered(

					link(OWL.SAMEAS, field(RDF.FIRST, field(RDF.REST))),

					link(OWL.SAMEAS, field(RDF.FIRST, field(RDF.REST, any(literal("any")))))

			)));

			items("{ 'first.rest': 'any' }", field(RDF.FIRST, link(OWL.SAMEAS, field(RDF.REST))),
					items -> assertThat(items.shape()).isEqualTo(filtered(

					field(RDF.FIRST, link(OWL.SAMEAS, field(RDF.REST))),

					field(RDF.FIRST, link(OWL.SAMEAS, field(RDF.REST, any(literal("any")))))

			)));

			// link implies Resource object
			items("{ 'first.rest': 'any' }", field(RDF.FIRST, field(RDF.REST, link(OWL.SAMEAS))),
					items -> assertThat(items.shape()).isEqualTo(filtered(

					field(RDF.FIRST, field(RDF.REST, link(OWL.SAMEAS))),

					field(RDF.FIRST, field(RDF.REST, link(OWL.SAMEAS, any(item("any")))))

			)));

		}


		@Test void testReportMalformedPaths() {
			assertThatThrownBy(() -> parse("{ '.order': '---' }", and())).isInstanceOf(JsonException.class);
		}

		@Test void testReportReservedPaths() {
			assertThatThrownBy(() -> parse("{ '.order': '.reserved' }", and())).isInstanceOf(JsonException.class);
		}

		@Test void testReportReferencesOutsideShapeEnvelope() {

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					terms("{ '.terms': 'nil' }", shape, items -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					stats("{ '.stats': 'nil' }", shape, stats -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '.order': 'nil' }", shape, items -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '>= nil': 1 }", shape, items -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ 'first.nil': 1 }", shape, items -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '.order': '-nil' }", shape, items -> {})
			);

		}

		@Test void testReportReferencesForEmptyShape() {

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					terms("{ '.terms': 'first' }", and(), items -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					stats("{ '.stats': 'first' }", and(), stats -> {})
			);

			assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
					items("{ '.order': 'nil' }", and(), items -> {})
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

			final Value first=item("first");
			final Value rest=item("rest");

			items("{ '@': '"+RDF.TYPE+"' }", shape, items -> assertThat(items.shape())
					.as("class")
					.isEqualTo(filtered(shape, clazz(RDF.TYPE)))
			);

			items("{ '^': '"+XSD.DATE+"' }", shape, items -> assertThat(items.shape())
					.as("type")
					.isEqualTo(filtered(shape, datatype(XSD.DATE)))
			);


			items("{ '>': 1 }", shape, items -> assertThat(items.shape())
					.as("min exclusive")
					.isEqualTo(filtered(shape, minExclusive(One)))
			);

			items("{ '<': 1 }", shape, items -> assertThat(items.shape())
					.as("max exclusive")
					.isEqualTo(filtered(shape, maxExclusive(One)))
			);

			items("{ '>=': 1 }", shape, items -> assertThat(items.shape())
					.as("min inclusive")
					.isEqualTo(filtered(shape, minInclusive(One)))
			);

			items("{ '<=': 1 }", shape, items -> assertThat(items.shape())
					.as("max inclusive")
					.isEqualTo(filtered(shape, maxInclusive(One)))
			);


			items("{ '$>': 123 }", shape, items -> assertThat(items.shape())
					.as("min length")
					.isEqualTo(filtered(shape, minLength(123)))
			);

			items("{ '$<': 123 }", shape, items -> assertThat(items.shape())
					.as("max length")
					.isEqualTo(filtered(shape, maxLength(123)))
			);

			items("{ '*': 'pattern' }", shape, items -> assertThat(items.shape())
					.as("pattern")
					.isEqualTo(filtered(shape, pattern("pattern")))
			);

			items("{ '~': 'words' }", shape, items -> assertThat(items.shape())
					.as("like")
					.isEqualTo(filtered(shape, like("words", true)))
			);

			items("{ '\\'': 'stem' }", shape, items -> assertThat(items.shape())
					.as("like")
					.isEqualTo(filtered(shape, stem("stem")))
			);


			items("{ '#>': 1 }", shape, items -> assertThat(items.shape())
					.as("min count")
					.isEqualTo(filtered(shape, minCount(1)))
			);

			items("{ '#<': 1 }", shape, items -> assertThat(items.shape())
					.as("max count")
					.isEqualTo(filtered(shape, maxCount(1)))
			);


			items("{ '%': [] }", shape, items -> assertThat(items.shape())
					.as("range (empty)")
					.isEqualTo(filtered(shape, range()))
			);

			items("{ '%': 'first' }", shape, items -> assertThat(items.shape())
					.as("range (singleton)")
					.isEqualTo(filtered(shape, range(first)))
			);

			items("{ '%': ['first', 'rest'] }", shape, items -> assertThat(items.shape())
					.as("in (multiple)")
					.isEqualTo(filtered(shape, range(first, rest)))
			);


			items("{ '!': [] }", shape, items -> assertThat(items.shape())
					.as("universal (empty)")
					.isEqualTo(filtered(shape, all()))
			);

			items("{ '!': 'first' }", shape, items -> assertThat(items.shape())
					.as("universal (singleton)")
					.isEqualTo(filtered(shape, all(first)))
			);

			items("{ '!': ['first', 'rest'] }", shape, items -> assertThat(items.shape())
					.as("universal (multiple)")
					.isEqualTo(filtered(shape, all(first, rest)))
			);


			items("{ '?': [] }", shape, items -> assertThat(items.shape())
					.as("existential (empty)")
					.isEqualTo(filtered(shape, any()))
			);

			items("{ '?': 'first' }", shape, items -> assertThat(items.shape())
					.as("existential (singleton)")
					.isEqualTo(filtered(shape, any(first)))
			);

			items("{ '?': ['first', 'rest'] }", shape, items -> assertThat(items.shape())
					.as("existential (multiple)")
					.isEqualTo(filtered(shape, any(first, rest)))
			);

		}

		@Test void testParsePathFilters() {

			items("{ '>= first.rest': 1 }", shape, items -> {
						assertThat(items.shape())
								.as("nested filter")
								.isEqualTo(filtered(shape,
										field(RDF.FIRST, field(RDF.REST, minInclusive(One)))));
					}
			);

			items("{ 'first.rest': 1 }", shape, items -> {
						assertThat(items.shape())
								.as("nested filter singleton shorthand")
								.isEqualTo(filtered(shape, field(RDF.FIRST, field(RDF.REST, any(One)))));
					}
			);

			items("{ 'first.rest': [1, 10] }", shape, items -> {
						assertThat(items.shape())
								.as("nested filter multiple shorthand")
								.isEqualTo(filtered(shape, field(RDF.FIRST, field(RDF.REST, any(One, Ten)))));
					}
			);

		}

		@Test void testParseShapedFilters() {

			final Shape shape=field(RDF.VALUE, datatype(XSD.LONG));

			items("{ 'value': '4' }", shape, items -> assertThat(items.shape())
					.isEqualTo(filtered(shape, field(RDF.VALUE, any(literal("4", XSD.LONG)))))
			);
		}

		@Test void testIgnoreEmptyFilters() {

			items("{ 'first': null }", shape, items -> assertThat(items.shape()).isEqualTo(shape));
			items("{ 'first': '' }", shape, items -> assertThat(items.shape()).isEqualTo(shape));
			items("{ 'first': [] }", shape, items -> assertThat(items.shape()).isEqualTo(shape));

		}

		@Test void testParseSliceLeniently() {

			items("{ '.offset': '1', '.limit': '2' }", shape, items -> assertThat(items)
					.isEqualTo(Items.items(shape, emptyList(), 1, 2))
			);

		}

	}

	@Nested final class Queries {

		@Test void testParsePlainQuery() {

			items("first=x&first.rest=y&first.rest=w+z", shape, items -> assertThat(items.shape())
					.isEqualTo(filtered(shape, field(RDF.FIRST,
							any(x),
							field(RDF.REST, any(literal("y"), literal("w z")))
					))));

			items("first=x&first.rest=y&.order=-first.rest&.order=first&.offset=1&.limit=2", shape, items -> {

				assertThat(items.orders())
						.containsExactly(decreasing(RDF.FIRST, RDF.REST), increasing(RDF.FIRST));

				assertThat(items.offset())
						.isEqualTo(1);

				assertThat(items.limit())
						.isEqualTo(2);

				assertThat(items.shape())
						.isEqualTo(filtered(shape, field(RDF.FIRST, and(
								any(x),
								field(RDF.REST, any(literal("y")))
						))));
			});

		}

		@Test void testParseItemsQuery() {

			items("{ '.offset': 1, '.limit': 2 }", shape, items -> {

				assertThat(items.shape()).as("shape").isEqualTo(filtered(shape, and()));
				assertThat(items.offset()).as("offset").isEqualTo(1);
				assertThat(items.limit()).as("limit").isEqualTo(2);

			});

		}

		@Test void testParseTermsQuery() {

			terms("{ '.terms': 'first.rest', '.offset': 1, '.limit': 2 }", shape, terms -> {

				assertThat(filtered(shape, and()))
						.as("shape")
						.isEqualTo(terms.shape());

				assertThat(terms.path())
						.containsExactly(RDF.FIRST, RDF.REST);

				assertThat(terms.offset())
						.as("offset")
						.isEqualTo(1);

				assertThat(terms.limit())
						.as("limit")
						.isEqualTo(2);

			});

		}

		@Test void testParseStatsQuery() {

			stats("{ '.stats': 'first.rest', '.offset': 1, '.limit': 2 }", shape, stats -> {

				assertThat(filtered(shape, and())).isEqualTo(stats.shape());

				assertThat(stats.path()).containsExactly(RDF.FIRST, RDF.REST);

				assertThat(stats.offset()).isEqualTo(1);
				assertThat(stats.limit()).isEqualTo(2);

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
