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

package com.metreeca.form.codecs;

import com.metreeca.form.Form;
import com.metreeca.form.Query;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.queries.Edges;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.form.shapes.MinExclusive;
import com.metreeca.form.shapes.MinLength;
import com.metreeca.form.things.Lists;
import com.metreeca.form.things.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.NoSuchElementException;
import java.util.function.Consumer;

import static com.metreeca.form.Order.decreasing;
import static com.metreeca.form.Order.increasing;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.MaxLength.maxLength;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.MinInclusive.minInclusive;
import static com.metreeca.form.shapes.Pattern.pattern;
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
			field(RDF.FIRST, field(RDF.REST)),
			field(inverse(RDF.FIRST), field(RDF.REST))
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

		stats("{ 'stats': '' }", shape, stats -> assertThat(stats.getPath())
				.as("empty path")
				.isEqualTo(Lists.<IRI>list()));

		stats("{ 'stats': '"+RDF.FIRST+"' }", shape, stats -> assertThat(stats.getPath())
				.as("direct naked iri")
				.isEqualTo(list(RDF.FIRST)));

		stats("{ 'stats': '^"+RDF.FIRST+"' }", shape, stats -> assertThat(stats.getPath())
				.as("inverse naked iri")
				.isEqualTo(list(inverse(RDF.FIRST))));

		stats("{ 'stats': '<"+RDF.FIRST+">' }", shape, stats -> assertThat(stats.getPath())
				.as("direct brackets iri")
				.isEqualTo(list(RDF.FIRST)));

		stats("{ 'stats': '^<"+RDF.FIRST+">' }", shape, stats -> assertThat(stats.getPath())
				.as("inverse brackets iri")
				.isEqualTo(list(inverse(RDF.FIRST))));

		stats("{ 'stats': '<"+RDF.FIRST+">/<"+RDF.REST+">' }", shape, stats -> assertThat(stats.getPath())
				.as("iri slash path")
				.isEqualTo(list(RDF.FIRST, RDF.REST)));

		stats("{ 'stats': 'first' }", shape, stats -> assertThat(stats.getPath())
				.as("direct alias")
				.isEqualTo(list(RDF.FIRST)));

		stats("{ 'stats': 'firstOf' }", shape, stats -> assertThat(stats.getPath())
				.as("inverse alias")
				.isEqualTo(list(inverse(RDF.FIRST))));

		stats("{ 'stats': 'first/rest' }", shape, stats -> assertThat(stats.getPath())
				.as("alias slash path")
				.isEqualTo(list(RDF.FIRST, RDF.REST)));

		stats("{ 'stats': 'firstOf.rest' }", shape, stats -> assertThat(stats.getPath())
				.as("alias dot path")
				.isEqualTo(list(inverse(RDF.FIRST), RDF.REST)));

	}


	@Test void testParseSortingCriteria() {

		edges("{ 'order': '' }", shape, edges -> assertThat(list(increasing())).as("empty path").isEqualTo(edges.getOrders()));

		edges("{ 'order': '+' }", shape, edges -> assertThat(list(increasing())).as("empty path increasing").isEqualTo(edges.getOrders()));

		edges("{ 'order': '-' }", shape, edges -> assertThat(list(decreasing())).as("empty path decreasing").isEqualTo(edges.getOrders()));

		edges("{ 'order': 'first.rest' }", shape, edges -> assertThat(list(increasing(RDF.FIRST, RDF.REST))).as("path").isEqualTo(edges.getOrders()));

		edges("{ 'order': '+first.rest' }", shape, edges -> assertThat(list(increasing(RDF.FIRST, RDF.REST))).as("path increasing").isEqualTo(edges.getOrders()));

		edges("{ 'order': '-first.rest' }", shape, edges -> assertThat(list(decreasing(RDF.FIRST, RDF.REST))).as("path decreasing").isEqualTo(edges.getOrders()));

		edges("{ 'order': [] }", shape, edges -> assertThat(list()).as("empty list").isEqualTo(edges.getOrders()));

		edges("{ 'order': ['+first', '-first.rest'] }", shape, edges -> assertThat(list(increasing(RDF.FIRST), decreasing(RDF.FIRST, RDF.REST))).as("list").isEqualTo(edges.getOrders()));

	}

	@Test void testParseSimpleFilters() {

		edges("{ 'filter': { '>>': 1 } }", shape, edges -> assertThat(edges.getShape())
				.as("min count")
				.isEqualTo(filter(shape, minCount(1)))
		);

		edges("{ 'filter': { '<<': 1 } }", shape, edges -> assertThat(edges.getShape())
				.as("max count")
				.isEqualTo(filter(shape, maxCount(1)))
		);


		edges("{ 'filter': { '>=': 1 } }", shape, edges -> assertThat(edges.getShape())
				.as("min inclusive")
				.isEqualTo(filter(shape, minInclusive(One)))
		);

		edges("{ 'filter': { '<=': 1 } }", shape, edges -> assertThat(edges.getShape())
				.as("max inclusive")
				.isEqualTo(filter(shape, maxInclusive(One)))
		);

		edges("{ 'filter': { '>': 1 } }", shape, edges -> assertThat(edges.getShape())
				.as("min exclusive")
				.isEqualTo(filter(shape, MinExclusive.minExclusive(One)))
		);

		edges("{ 'filter': { '<': 1 } }", shape, edges -> assertThat(edges.getShape())
				.as("max exclusive")
				.isEqualTo(filter(shape, maxExclusive(One)))
		);

		edges("{ 'filter': { '~': 'words' } }", shape, edges -> assertThat(edges.getShape())
				.as("like")
				.isEqualTo(filter(shape, like("words")))
		);

		edges("{ 'filter': { '*': 'pattern' } }", shape, edges -> assertThat(edges.getShape())
				.as("pattern")
				.isEqualTo(filter(shape, pattern("pattern")))
		);

		edges("{ 'filter': { '>#': 123 } }", shape, edges -> assertThat(edges.getShape())
				.as("min length")
				.isEqualTo(filter(shape, MinLength.minLength(123)))
		);

		edges("{ 'filter': { '#<': 123 } }", shape, edges -> assertThat(edges.getShape())
				.as("max length")
				.isEqualTo(filter(shape, maxLength(123)))
		);


		edges("{ 'filter': { '@': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#nil' } }", shape, edges -> assertThat(edges.getShape())
				.as("class")
				.isEqualTo(filter(shape, clazz(RDF.NIL)))
		);

		edges("{ 'filter': { '^': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#nil' } }", shape, edges -> assertThat(edges.getShape())
				.as("type")
				.isEqualTo(filter(shape, datatype(RDF.NIL)))
		);


		edges("{ 'filter': { '!': [] } }", shape, edges -> assertThat(edges.getShape())
				.as("ignore empty universal")
				.isEqualTo(filter(shape, and()))
		);

		edges("{ 'filter': { '!': { '_this': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' } } }", shape, edges -> assertThat(edges.getShape())
				.as("universal (singleton)")
				.isEqualTo(filter(shape, all(RDF.FIRST)))
		);

		edges("{ 'filter': { '!': [\n"
				+"\t{ '_this': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' },\n"
				+"\t{ '_this': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#rest' }\n"
				+"] } }", shape, edges -> assertThat(edges.getShape())
				.as("universal (multiple)")
				.isEqualTo(filter(shape, all(RDF.FIRST, RDF.REST)))
		);


		edges("{ 'filter': { '?': [] } }", shape, edges -> assertThat(edges.getShape())
				.as("ignore empty existential")
				.isEqualTo(filter(shape, and()))
		);

		edges("{ 'filter': { '?':"
				+ "\t{ '_this': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' }\n"
				+ "} }", shape, edges -> assertThat(edges.getShape())
				.as("existential (singleton)")
				.isEqualTo(filter(shape, any(RDF.FIRST)))
		);

		edges("{ 'filter': { '?': [\n"
				+"\t{ '_this': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' },\n"
				+"\t{ '_this': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#rest' }\n"
				+"] } }", shape, edges -> assertThat(edges.getShape())
				.as("existential (multiple)")
				.isEqualTo(filter(shape, any(RDF.FIRST, RDF.REST)))
		);

	}

	@Test void testParseStructuredFilters() {

		edges("{\n\t'filter': {}\n}", shape, edges -> assertThat(edges.getShape())
				.as("empty filter")
				.isEqualTo(filter(shape, and()))
		);

		edges("{ 'filter': { 'first.rest': { '>=': 1 } } }", shape, edges -> assertThat(edges.getShape())
				.as("nested filter")
				.isEqualTo(filter(shape, field(RDF.FIRST, field(RDF.REST, minInclusive(One)))))
		);

		edges("{ 'filter': { 'first.rest': 1 } }", shape, edges -> assertThat(edges.getShape())
				.as("nested filter singleton shorthand")
				.isEqualTo(filter(shape, field(RDF.FIRST, field(RDF.REST, any(One)))))
		);

		edges("{ 'filter': { 'first.rest': [1, 10] } }", shape, edges -> assertThat(edges.getShape())
				.as("nested filter multiple shorthand")
				.isEqualTo(filter(shape, field(RDF.FIRST, field(RDF.REST, any(One, Ten)))))
		);

	}


	@Test void testHandleInlinedProvedIRIs() {

		final Shape shape=datatype(Form.IRIType);

		edges("{ 'filter': { '!': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' } }",
				shape, edges -> assertThat(edges.getShape())
						.as("universal (singleton)")
						.isEqualTo(filter(shape, all(RDF.FIRST)))
		);

		edges("{ 'filter': { '!': ["
						+"'http://www.w3.org/1999/02/22-rdf-syntax-ns#first',"
						+"'http://www.w3.org/1999/02/22-rdf-syntax-ns#rest'"
						+"] } }",
				shape, edges -> assertThat(edges.getShape())
						.as("universal (singleton)")
						.isEqualTo(filter(shape, all(RDF.FIRST, RDF.REST)))
		);

		edges("{ 'filter': { '?': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' } }",
				shape, edges -> assertThat(edges.getShape())
						.as("universal (singleton)")
						.isEqualTo(filter(shape, any(RDF.FIRST)))
		);

		edges("{ 'filter': { '?': ["
						+"'http://www.w3.org/1999/02/22-rdf-syntax-ns#first',"
						+"'http://www.w3.org/1999/02/22-rdf-syntax-ns#rest'"
						+"] } }",
				shape, edges -> assertThat(edges.getShape())
						.as("universal (singleton)")
						.isEqualTo(filter(shape, any(RDF.FIRST, RDF.REST)))
		);

	}

	@Test void testHandleNestedInlineProvedIRIs() {

		final Shape shape=field(RDF.VALUE, datatype(Form.IRIType));

		edges("{ 'filter': { 'value': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' } }",
				shape, edges -> assertThat(edges.getShape())
						.as("universal (singleton)")
						.isEqualTo(filter(shape, field(RDF.VALUE, any(RDF.FIRST))))
		);

		edges("{ 'filter': { 'value': ["
						+"'http://www.w3.org/1999/02/22-rdf-syntax-ns#first',"
						+"'http://www.w3.org/1999/02/22-rdf-syntax-ns#rest'"
						+"] } }",
				shape, edges -> assertThat(edges.getShape())
						.as("universal (singleton)")
						.isEqualTo(filter(shape, field(RDF.VALUE, any(RDF.FIRST, RDF.REST))))
		);

	}


	@Test void testHandleSingletonIRI() {

		final Shape shape=field(RDF.VALUE, datatype(Form.IRIType));

		edges("{ 'filter': { 'value': { '_this': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' } } }",
				shape, edges -> assertThat(edges.getShape())
						.as("universal (singleton)")
						.isEqualTo(filter(shape, field(RDF.VALUE, any(RDF.FIRST))))
		);

	}

	@Test void testResolveRootRelativeIRIs() {

		final Shape shape=datatype(Form.IRIType);

		edges("{ 'filter': { '!': '/1999/02/22-rdf-syntax-ns#first' } }",
				shape, edges -> assertThat(edges.getShape())
						.as("universal (singleton)")
						.isEqualTo(filter(shape, all(RDF.FIRST)))
		);

	}


	@Test void testParseEdgesQuery() {

		edges("{ 'filter': {}, 'offset': 1, 'limit': 2 }", shape, edges -> {

			assertThat(filter(shape, and())).as("shape").isEqualTo(edges.getShape());
			assertThat(1).as("offset").isEqualTo(edges.getOffset());
			assertThat(2).as("limit").isEqualTo(edges.getLimit());

		});

	}

	@Test void testParseStatsQuery() {

		stats("{ 'filter': {}, 'stats': '' }", shape, stats -> {

			assertThat(filter(shape, and())).as("shape").isEqualTo(stats.getShape());
			assertThat(stats.getPath())
					.as("path")
					.isEqualTo(Lists.<IRI>list());

		});

	}

	@Test void testParseItemsQuery() {

		items("{ 'filter': {}, 'items': '' }", shape, items -> {

			assertThat(filter(shape, and())).as("shape").isEqualTo(items.getShape());
			assertThat(items.getPath())
					.as("path")
					.isEqualTo(Lists.<IRI>list());

		});

	}


	@Test void testIgnoreNullFilters() {

		edges("{ 'filter': null }", shape, edges -> assertThat(edges.getShape()).isEqualTo(shape));

	}

	@Test void testIgnoreNullValues() {

		edges("{ 'filter': { 'first': null } }", shape, edges -> assertThat(edges.getShape()).isEqualTo(shape));

	}


	@Test void testRejectReferencesOutsideShapeEnvelope() {

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				stats("{ 'stats': 'nil' }", shape, stats -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				stats("{ 'stats': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#nil' }", shape, stats -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ 'items': 'nil' }", shape, items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ 'items': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#nil' }", shape, items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				edges("{ 'filter': { 'nil': { '>=': 1 } } }", shape, edges -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				edges("{ 'filter': { 'http://www.w3.org/1999/02/22-rdf-syntax-ns#nil': { '>=': 1 } } }", shape, edges -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				edges("{ 'order': 'nil' }", shape, edges -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				edges("{ 'order': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#nil' }", shape, edges -> {})
		);

	}

	@Test void testRejectReferencesForEmptyShape() {

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				stats("{ 'stats': 'first' }", pass(), stats -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				stats("{ 'stats': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' }", pass(), stats -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ 'items': 'first' }", pass(), items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				items("{ 'items': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first' }", pass(), items -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				edges("{ 'filter': { 'first': { '>=': 1 } } }", pass(), edges -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				edges("{ 'filter': { 'http://www.w3.org/1999/02/22-rdf-syntax-ns#first': { '>=': 1 } } }", pass(), edges -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				edges("{ 'order': 'nil' }", pass(), edges -> {})
		);

		assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() ->
				edges("{ 'order': 'http://www.w3.org/1999/02/22-rdf-syntax-ns#nil' }", pass(), edges -> {})
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
		assertThat(new QueryParser(shape, RDF.NAMESPACE).parse(json.replace('\'', '"')).map(probe))
				.as("query processed")
				.isTrue();
	}

	private Shape filter(final Shape shape, final Shape filter) {
		return and(shape, Shape.filter().then(filter)).map(new Optimizer());
	}

}
