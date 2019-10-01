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

package com.metreeca.rdf.codecs;

import com.metreeca.rdf.ValuesTest;
import com.metreeca.tree.Shape;

import org.eclipse.rdf4j.model.BNode;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.JsonValue;

import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.ValuesTest.item;
import static com.metreeca.rest.formats.JSONAssert.assertThat;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;


final class JSONEncoderTest {

	private final IRI focus=iri("app:/");


	private JSONEncoder encoder() {
		return new JSONEncoder(ValuesTest.Base) {};
	}


	private JsonValue expected(final Object value) {
		return JSONCodecTest.object(JSONCodecTest.map(
				JSONCodecTest.entry("_this", ((Value)focus).toString()),
				JSONCodecTest.entry("value", asList(value))
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
					.isEqualTo(expected(JSONCodecTest.map()));
		}

		@Test void testBNodeWithBackLink() {

			final BNode x=bnode("x");
			final BNode y=bnode("y");

			assertThat(encoder().json(
					asList(statement(x, RDF.VALUE, y), statement(y, RDF.VALUE, x)),
					field(RDF.VALUE, and(required(), field(RDF.VALUE, required()))),
					x
			))
					.isEqualTo(JSONCodecTest.object(JSONCodecTest.map(
							JSONCodecTest.entry("_this", "_:x"),
							JSONCodecTest.entry("value", JSONCodecTest.map(
									JSONCodecTest.entry("value", JSONCodecTest.map(
											JSONCodecTest.entry("_this","_:x")
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
					.isEqualTo(JSONCodecTest.object(JSONCodecTest.map(
							JSONCodecTest.entry("_this", "_:x"),
							JSONCodecTest.entry("value", JSONCodecTest.map(
									JSONCodecTest.entry("value", "_:x")
							))
					)));
		}


		@Test void testIRI() {
			assertThat(actual(item("id")))
					.isEqualTo(expected(JSONCodecTest.map(JSONCodecTest.entry("_this", "/id"))));
		}

		@Test void testProvedIRI() {

			assertThat(actual(item("id"), datatype(IRIType)))
					.isEqualTo(expected("/id"));

		}

	}


	@Nested final class Literals {

		@Test void testTypedString() {
			assertThat(actual(literal("2019-04-03", XMLSchema.DATE)))
					.isEqualTo(expected(JSONCodecTest.map(
							JSONCodecTest.entry("_this", "2019-04-03"),
							JSONCodecTest.entry("_type", XMLSchema.DATE.stringValue())
					)));
		}

		@Test void testTaggedString() {
			assertThat(actual(literal("string", "en")))
					.isEqualTo(expected(JSONCodecTest.map(
							JSONCodecTest.entry("_this", "string"),
							JSONCodecTest.entry("_type", "@en")
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
					.isEqualTo(expected(JSONCodecTest.map(
							JSONCodecTest.entry("_this", "123.0"),
							JSONCodecTest.entry("_type", XMLSchema.DOUBLE.stringValue())
					)));
		}

		@Test void testBoolean() {

			assertThat(actual(literal(true)))
					.isEqualTo(expected(true));

			assertThat(actual(literal(false)))
					.isEqualTo(expected(false));

		}

		@Test void testLiteralWithKnownDatatype() {
			assertThat(actual(literal("2019-04-03", XMLSchema.DATE), datatype(XMLSchema.DATE)))
					.isEqualTo(expected("2019-04-03"));
		}

	}

}
