
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

package com.metreeca.json;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;
import java.util.Arrays;
import java.util.Comparator;

import static com.metreeca.json.Values.*;

import static org.assertj.core.api.Assertions.assertThat;

import static java.time.ZoneOffset.UTC;


final class ValueComparatorTest {

	private final Comparator<Value> comparator=new ValueComparator();

	private int compare(final Value x, final Value y) {
		return comparator.compare(x, y);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testNull() {

		assertThat(compare(null, null)).isEqualTo(0);

		final Value lesser=null;
		final Value greater=bnode();

		assertThat(compare(lesser, greater)).isLessThan(0);
		assertThat(compare(greater, lesser)).isGreaterThan(0);

	}

	@Test void testBNode() {

		assertThat(compare(bnode("x"), bnode("x"))).isEqualTo(0);
		assertThat(compare(bnode("x"), bnode("y"))).isLessThan(0);
		assertThat(compare(bnode("y"), bnode("x"))).isGreaterThan(0);

		final Value lesser=bnode();
		final Value greater=iri();

		assertThat(compare(lesser, greater)).isLessThan(0);
		assertThat(compare(greater, lesser)).isGreaterThan(0);

	}

	@Test void testIRI() {

		assertThat(compare(iri("http://script.example/Latin"), iri("http://script.example/Latin"))).isEqualTo(0);
		assertThat(compare(iri("http://script.example/Latin"), iri("http://script.example/Кириллица"))).isLessThan(0);
		assertThat(compare(iri("http://script.example/日本語"), iri("http://script.example/Кириллица"))).isGreaterThan(0);

		final Value lesser=iri();
		final Value greater=literal(true);

		assertThat(compare(lesser, greater)).isLessThan(0);
		assertThat(compare(greater, lesser)).isGreaterThan(0);

	}

	@Test void testBoolean() {

		assertThat(compare(literal(true), literal(true))).isEqualTo(0);
		assertThat(compare(literal(false), literal(true))).isLessThan(0);
		assertThat(compare(literal(true), literal(false))).isGreaterThan(0);

		assertThat(compare(literal(true), literal("malformed", XSD.BOOLEAN))).isLessThan(0);
		assertThat(compare(literal("malformed", XSD.BOOLEAN), literal(true))).isGreaterThan(0);
		assertThat(compare(literal("x", XSD.BOOLEAN), literal("y", XSD.BOOLEAN))).isLessThan(0);

		final Value lesser=literal(true);
		final Value greater=literal(0);

		assertThat(compare(lesser, greater)).isLessThan(0);
		assertThat(compare(greater, lesser)).isGreaterThan(0);

	}

	@Test void testNumeric() {

		assertThat(compare(literal(0), literal(0))).isEqualTo(0);
		assertThat(compare(literal(0), literal(1))).isLessThan(0);
		assertThat(compare(literal(1), literal(0))).isGreaterThan(0);

		final Literal[] numbers={
				literal((byte)1),
				literal((short)2),
				literal(3),
				literal(4L),
				literal(5.0f),
				literal(6.0d, false),
				literal(BigInteger.valueOf(7)),
				literal(BigDecimal.valueOf(8)),
		};

		final Literal[] clones=numbers.clone();

		Arrays.sort(clones, this::compare);

		assertThat(clones).isEqualTo(numbers);

		assertThat(compare(literal(Float.NaN), literal(Float.NaN))).isEqualTo(0);
		assertThat(compare(literal(0.0f), literal(Float.NaN))).isLessThan(0);
		assertThat(compare(literal(Float.NaN), literal(0.0f))).isGreaterThan(0);
		assertThat(compare(literal(Float.NEGATIVE_INFINITY), literal(0.0f))).isLessThan(0);
		assertThat(compare(literal(0.0f), literal(Float.POSITIVE_INFINITY))).isLessThan(0);
		assertThat(compare(literal(Float.NEGATIVE_INFINITY), literal(Float.POSITIVE_INFINITY))).isLessThan(0);

		assertThat(compare(literal(Double.NaN, false), literal(Double.NaN, false))).isEqualTo(0);
		assertThat(compare(literal(0.0d, false), literal(Double.NaN, false))).isLessThan(0);
		assertThat(compare(literal(Double.NaN, false), literal(0.0d, false))).isGreaterThan(0);
		assertThat(compare(literal(Double.NEGATIVE_INFINITY, false), literal(0.0d, false))).isLessThan(0);
		assertThat(compare(literal(0.0d, false), literal(Double.POSITIVE_INFINITY, false))).isLessThan(0);
		assertThat(compare(literal(Double.NEGATIVE_INFINITY, false), literal(Double.POSITIVE_INFINITY, false))).isLessThan(0);

		assertThat(compare(literal(1), literal("malformed", XSD.INTEGER))).isLessThan(0);
		assertThat(compare(literal("malformed", XSD.INTEGER), literal(1))).isGreaterThan(0);
		assertThat(compare(literal("x", XSD.INTEGER), literal("y", XSD.INTEGER))).isLessThan(0);

		final Value lesser=literal(1);
		final Value greater=literal(Instant.now().atZone(UTC));

		assertThat(compare(lesser, greater)).isLessThan(0);
		assertThat(compare(greater, literal(1))).isGreaterThan(0);

	}

	@Test void testTemporal() {

		final OffsetDateTime x=OffsetDateTime.now();
		final OffsetDateTime y=x.plusSeconds(1);

		assertThat(compare(literal(x), literal(x))).isEqualTo(0);
		assertThat(compare(literal(x), literal(y))).isLessThan(0);
		assertThat(compare(literal(y), literal(x))).isGreaterThan(0);

		final Value lesser=literal(Instant.now().atZone(UTC));
		final Value greater=literal(Duration.ZERO);

		assertThat(compare(lesser, greater)).isLessThan(0);
		assertThat(compare(greater, lesser)).isGreaterThan(0);

	}

	@Test void testDuration() {

		final Duration x=Duration.ofDays(1);
		final Duration y=x.plusSeconds(1);

		assertThat(compare(literal(x), literal(x))).isEqualTo(0);
		assertThat(compare(literal(x), literal(y))).isLessThan(0);
		assertThat(compare(literal(y), literal(x))).isGreaterThan(0);

		final Value lesser=literal(Duration.ZERO);
		final Value greater=literal("x");

		assertThat(compare(lesser, greater)).isLessThan(0);
		assertThat(compare(greater, lesser)).isGreaterThan(0);

	}

	@Test void testPlain() {

		assertThat(compare(literal("x"), literal("x"))).isEqualTo(0);
		assertThat(compare(literal("x"), literal("y"))).isLessThan(0);
		assertThat(compare(literal("y"), literal("x"))).isGreaterThan(0);

		final Value lesser=literal("x");
		final Value greater=literal("x", "en");

		assertThat(compare(lesser, greater)).isLessThan(0);
		assertThat(compare(greater, lesser)).isGreaterThan(0);

	}

	@Test void testTagged() {

		assertThat(compare(literal("x", "en"), literal("x", "en"))).isEqualTo(0);
		assertThat(compare(literal("x", "en"), literal("x", "it"))).isLessThan(0);
		assertThat(compare(literal("x", "en"), literal("y", "en"))).isLessThan(0);
		assertThat(compare(literal("x", "it"), literal("x", "en"))).isGreaterThan(0);
		assertThat(compare(literal("y", "en"), literal("x", "en"))).isGreaterThan(0);

		final Value lesser=literal("x", "en");
		final Value greater=literal("x", RDF.FIRST);

		assertThat(compare(lesser, greater)).isLessThan(0);
		assertThat(compare(greater, lesser)).isGreaterThan(0);

	}

	@Test void testTyped() {

		assertThat(compare(literal("x", RDF.FIRST), literal("x", RDF.FIRST))).isEqualTo(0);
		assertThat(compare(literal("x", RDF.FIRST), literal("x", RDF.REST))).isLessThan(0);
		assertThat(compare(literal("x", RDF.FIRST), literal("y", RDF.FIRST))).isLessThan(0);
		assertThat(compare(literal("x", RDF.REST), literal("x", RDF.FIRST))).isGreaterThan(0);
		assertThat(compare(literal("y", RDF.FIRST), literal("x", RDF.FIRST))).isGreaterThan(0);

	}

}
