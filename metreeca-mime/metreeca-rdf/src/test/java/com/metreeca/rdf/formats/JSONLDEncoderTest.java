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

package com.metreeca.rdf.formats;

import com.metreeca.json.Shape;
import com.metreeca.rdf.ValuesTest;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.JsonValue;

import static com.metreeca.core.formats.JSONAssert.assertThat;
import static com.metreeca.json.Shape.required;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.ValuesTest.item;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;


final class JSONLDEncoderTest {

	private final IRI focus=iri("app:/");


	private JSONLDEncoder encoder() {
		return new JSONLDEncoder(ValuesTest.Base, JSONLDCodecTest.Context) {};
	}


	private JsonValue expected(final Object value) {
		return JSONLDCodecTest.object(JSONLDCodecTest.map(
				JSONLDCodecTest.entry("id", ((Value)focus).toString()),
				JSONLDCodecTest.entry("value", asList(value))
		));
	}


	private JsonValue actual(final Value values) {
		return actual(values, and());
	}

	private JsonValue actual(final Value value, final Shape shape) {
		return encoder().json(
				singletonList(statement(focus, RDF.VALUE, value)),
				field(RDF.VALUE, shape),
				focus
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Nested final class Resources {

		@Test void testBNode() {
			assertThat(actual(bnode()))
					.isEqualTo(expected(JSONLDCodecTest.map()));
		}

		@Test void testBNodeWithBackLink() {

			final BNode x=bnode("x");
			final BNode y=bnode("y");

			assertThat(encoder().json(
					asList(statement(x, RDF.VALUE, y), statement(y, RDF.VALUE, x)),
					field(RDF.VALUE, and(required(), field(RDF.VALUE, required()))),
					x
			))
					.isEqualTo(JSONLDCodecTest.object(JSONLDCodecTest.map(
							JSONLDCodecTest.entry("id", "_:x"),
							JSONLDCodecTest.entry("value", JSONLDCodecTest.map(
									JSONLDCodecTest.entry("value", JSONLDCodecTest.map(
											JSONLDCodecTest.entry("id", "_:x")
									))
							))
					)));
		}

		@Test void testBNodeWithBackLinkToProvedResource() {

			final BNode x=bnode("x");
			final BNode y=bnode("y");

			assertThat(encoder().json(
					asList(statement(x, RDF.VALUE, y), statement(y, RDF.VALUE, x)),
					field(RDF.VALUE, and(required(), field(RDF.VALUE, and(required(), datatype(ResourceType))))),
					x
			))
					.isEqualTo(JSONLDCodecTest.object(JSONLDCodecTest.map(
							JSONLDCodecTest.entry("id", "_:x"),
							JSONLDCodecTest.entry("value", JSONLDCodecTest.map(
									JSONLDCodecTest.entry("value", "_:x")
							))
					)));
		}


		@Test void testIRI() {
			assertThat(actual(item("id")))
					.isEqualTo(expected(JSONLDCodecTest.map(JSONLDCodecTest.entry("id", "/id"))));
		}

		@Test void testProvedIRI() {

			assertThat(actual(item("id"), datatype(IRIType)))
					.isEqualTo(expected("/id"));

		}

	}


	@Nested final class Literals {

		@Test void testTypedString() {
			assertThat(actual(literal("2019-04-03", XSD.DATE)))
					.isEqualTo(expected(JSONLDCodecTest.map(
							JSONLDCodecTest.entry("value", "2019-04-03"),
							JSONLDCodecTest.entry("type", XSD.DATE.stringValue())
					)));
		}

		@Test void testTaggedString() {
			assertThat(actual(literal("string", "en")))
					.isEqualTo(expected(JSONLDCodecTest.map(
							JSONLDCodecTest.entry("value", "string"),
							JSONLDCodecTest.entry("language", "en")
					)));
		}

		@Test void testPlainString() {
			assertThat(actual(literal("string")))
					.isEqualTo(expected("string"));
		}

		@Test void testInteger() {
			assertThat(actual(literal(integer(123))))
					.isEqualTo(expected(123));
		}

		@Test void testDecimal() {
			assertThat(actual(literal(decimal(123))))
					.isEqualTo(expected(123.0));
		}

		@Test void testDouble() {
			assertThat(actual(literal(123.0)))
					.isEqualTo(expected(JSONLDCodecTest.map(
							JSONLDCodecTest.entry("value", "123.0"),
							JSONLDCodecTest.entry("type", XSD.DOUBLE.stringValue())
					)));
		}

		@Test void testBoolean() {

			assertThat(actual(literal(true)))
					.isEqualTo(expected(true));

			assertThat(actual(literal(false)))
					.isEqualTo(expected(false));

		}

		@Test void testLiteralWithKnownDatatype() {
			assertThat(actual(literal("2019-04-03", XSD.DATE), datatype(XSD.DATE)))
					.isEqualTo(expected("2019-04-03"));
		}

	}

}
