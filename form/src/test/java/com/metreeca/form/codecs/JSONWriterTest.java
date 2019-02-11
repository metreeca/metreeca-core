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

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Field;
import com.metreeca.form.shapes.Or;
import com.metreeca.form.things.ValuesTest;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import javax.json.*;

import static com.metreeca.form.Shape.required;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.ValuesTest.decode;

import static org.assertj.core.api.Assertions.assertThat;


final class JSONWriterTest extends JSONCodecTest {

	//// Objects ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testNoObjects() {
		assertThat((Object)json(array())).as("no objects").isEqualTo(json(decode("")));
	}

	@Test void testBlankObjects() {
		assertThat((Object)json(array(object(
				field("this", "_:x"),
				field(value, array("x"))
		)))).as("blank objects").isEqualTo(json(decode("_:x rdf:value 'x'.")));
	}

	@Test void testNamedObjects() {
		assertThat((Object)json(array(object(
				field("this", "http://example.com/x"),
				field(value, array("x"))
		)))).as("named objects").isEqualTo(json(decode("<x> rdf:value 'x'.")));
	}

	@Test void testTypedObjects() {

		assertEquivalent("boolean", json(blanks(true)), json(decode("_:focus rdf:value true .")));
		assertEquivalent("string", json(blanks("string")), json(decode("[] rdf:value 'string' .")));
		assertEquivalent("integer", json(blanks(BigInteger.ONE)), json(decode("[] rdf:value 1 .")));
		assertEquivalent("decimal", json(blanks(new BigDecimal("1.0"))), json(decode("[] rdf:value 1.0 .")));
		assertEquivalent("double", json(blanks(1.0)), json(decode("[] rdf:value 1e0 .")));

		assertEquivalent("numeric",
				json(blanks(object(field("text", "1"), field("type", XMLSchema.INT.stringValue())))),
				json(decode("[] rdf:value '1'^^xsd:int .")));

		assertEquivalent("custom",
				json(blanks(object(field("text", "text"), field("type", ValuesTest.term("type").stringValue())))),
				json(decode("[] rdf:value 'text'^^:type .")));

		assertEquivalent("tagged",
				json(blanks(object(field("text", "text"), field("lang", "en")))),
				json(decode("[] rdf:value 'text'@en .")));

		assertEquivalent("malformed",
				json(blanks(object(field("text", "malformed"), field("type", XMLSchema.BOOLEAN.stringValue())))),
				json(decode("[] rdf:value 'malformed'^^xsd:boolean .")));

	}


	//// Focus /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testWriteOnlyFocusNode() {
		assertThat((Object)json(object(
				field("this", "http://example.com/x"),
				field(value, array("x"))
		))).as("focus node only").isEqualTo(json(
				decode("<x> rdf:value 'x' . <y> rdf:value 'y' ."),
				iri("http://example.com/x")
		));
	}

	@Test void testHandleUnknownFocusNode() {
		assertEquivalent("unknown focus",
				json(object()),
				json(
						decode("<x> rdf:value 'x' . <y> rdf:value 'y' ."),
						bnode()
				));
	}


	//// Shared References /////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testExpandSharedTrees() {
		assertThat((Object)json(object(
				field("this", "http://example.com/x"),
				field(value, array(
						object(
								field("this", "http://example.com/w"),
								field(value, array(object(field("this", "http://example.com/z"))))
						),
						object(
								field("this", "http://example.com/y"),
								field(value, array(object(field("this", "http://example.com/z"))))
						)
				))
		))).as("expanded shared trees").isEqualTo(json(
				decode("<x> rdf:value <w>, <y>. <w> rdf:value <z>. <y> rdf:value <z>."),
				iri("http://example.com/x")
		));
	}

	@Test void testHandleNamedLoops() {
		assertThat((Object)json(object(
				field("this", "http://example.com/x"),
				field(value, array(
						object(
								field("this", "http://example.com/y"),
								field(value, array(object(field("this", "http://example.com/x"))))
						)
				))
		))).as("named loops").isEqualTo(json(
				decode("<x> rdf:value <y>. <y> rdf:value <x>."),
				iri("http://example.com/x")
		));
	}

	@Test void testHandleBlankLoops() {
		assertThat((Object)json(object(
				field("this", "_:x"),
				field(value, array(
						object(
								field("this", "_:y"),
								field(value, array(object(field("this", "_:x"))))
						)
				))
		))).as("named loops").isEqualTo(json(
				decode("_:x rdf:value _:y. _:y rdf:value _:x."),
				bnode("x")
		));
	}


	//// Aliases ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testAliasFields() {

		assertEquivalent("direct inferred",
				json(object(field("value", array(object())))),
				json(
						decode("_:x rdf:value _:y ."),
						bnode("x"),
						Field.field(RDF.VALUE)
				));

		assertEquivalent("inverse inferred",
				json(object(field("valueOf", array(object())))),
				json(
						decode("_:y rdf:value _:x ."),
						bnode("x"),
						Field.field(inverse(RDF.VALUE))
				));

		assertEquivalent("user-defined",
				json(object(field("alias", array(object())))),
				json(
						decode("_:x rdf:value _:y ."),
						bnode("x"),
						Field.field(RDF.VALUE, alias("alias"))
				));

	}

	@Test void testAliasNestedFields() {

		assertEquivalent("aliased nested field",
				json(object(field("value", array(object(field("alias", array(object()))))))),
				json(
						decode("_:x rdf:value [rdf:value _:y] ."),
						bnode("x"),
						Field.field(RDF.VALUE, Field.field(RDF.VALUE, alias("alias")))
				));

	}

	@Test void testHandleAliasClashes() {
		assertEquivalent("clashing aliases",
				json(object(
						field(value, array(object())),
						field(ValuesTest.term("value").stringValue(), array(object()))
				)),
				json(
						decode("_:x rdf:value _:y; :value _:z."),
						bnode("x"),
						and(
								Field.field(RDF.VALUE),
								Field.field(ValuesTest.term("value"))
						)
				));
	}

	@Test void testIgnoreReservedAliases() {
		assertEquivalent("reserved alias",
				json(object(field("value", array(object())))),
				json(
						decode("_:x rdf:value _:y ."),
						bnode("x"),
						Field.field(RDF.VALUE, alias("this"))
				));
	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testConsiderDisjunctiveDefinitions() {
		assertEquivalent("simplified literal with known datatype",
				json(object(
						field("first", "x"),
						field("rest", "y")
				)),
				json(
						decode("_:focus rdf:first 'x'; rdf:rest 'y'."), // invalid shape (forces content on both branches)
						bnode("focus"),
						Or.or(
								Field.field(RDF.FIRST, required()),
								Field.field(RDF.REST, required())
						)
				));
	}

	@Test void testWriteNamedReverseLinks() {
		assertThat((Object)json(object(
				field("this", "http://example.com/x"),
				field("valueOf", array(object(
						field("this", "http://example.com/y")
				)))
		))).as("named reverse links").isEqualTo(json(
				decode("<y> rdf:value <x> ."),
				iri("http://example.com/x"),
				Field.field(inverse(RDF.VALUE))
		));
	}

	@Test void testWriteBlankReverseLinks() {
		assertEquivalent("blank reverse links",
				json(object(field("valueOf", array(object())))),
				json(
						decode("_:y rdf:value _:x ."),
						bnode("x"),
						Field.field(inverse(RDF.VALUE))
				));
	}

	@Test void testOmitNullValues() {
		assertEquivalent("omitted empty array",
				json(object()),
				json(
						decode("_:focus rdf:value 'x'."),
						bnode("focus"),
						Field.field(RDF.TYPE, required())
				));

	}

	@Test void testOmitEmptyArrays() {
		assertEquivalent("omitted empty array",
				json(object()),
				json(
						decode("_:focus rdf:value 'x'."),
						bnode("focus"),
						Field.field(RDF.TYPE)
				));

	}

	@Test void testWriteObjectInsteadOfArrayIfNotRepeatable() {
		assertEquivalent("simplified unrepeatable value",
				json(object(field("value", "x"))),
				json(
						decode("_:focus rdf:value 'x'."),
						bnode("focus"),
						Field.field(RDF.VALUE, maxCount(1))
				));
	}

	@Test void testInlineProvedLeafIRIs() {
		assertEquivalent("simplified leaf IRI",
				json(object(field("value", RDF.NIL.stringValue()))),
				json(
						decode("_:focus rdf:value rdf:nil."),
						bnode("focus"),
						Field.field(RDF.VALUE, and(datatype(Form.IRIType), maxCount(1)))
				));
	}

	@Test void testInlineProvedTypedLiterals() {
		assertEquivalent("simplified literal with known datatype",
				json(object(field("value", "2016-08-11"))),
				json(
						decode("_:focus rdf:value '2016-08-11'^^xsd:date."),
						bnode("focus"),
						Field.field(RDF.VALUE, and(datatype(XMLSchema.DATE), maxCount(1)))
				));
	}

	@Test void testOmitThisForUnreferencedProvedBlanks() {

		assertThat((Object)json(object(field("value", array(object()))))).as("unreferenced proved blank").isEqualTo(json(
				decode("_:x rdf:value _:y ."),
				bnode("x"),
				and(datatype(Form.BNodeType), Field.field(RDF.VALUE, datatype(Form.BNodeType)))
		));

		assertThat((Object)json(object(
				field("this", "_:x"),
				field("value", array(object(field("this", "_:x"))))))).as("back-referenced proved blank").isEqualTo(json(
				decode("_:x rdf:value _:x ."),
				bnode("x"),
				and(datatype(Form.BNodeType), Field.field(RDF.VALUE, datatype(Form.BNodeType)))
		));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonStructure json(final Iterable<Statement> model) {
		return json(model, null);
	}

	private JsonStructure json(final Iterable<Statement> model, final Resource focus) {
		return json(model, focus, null);
	}

	private JsonStructure json(final Iterable<Statement> model, final Resource focus, final Shape shape) {
		try (final StringWriter buffer=new StringWriter(1000)) {

			final RDFWriter writer=new JSONWriter(buffer);

			writer.set(JSONCodec.Focus, focus);
			writer.set(JSONCodec.Shape, shape);

			Rio.write(model, writer);

			try (final JsonReader reader=Json.createReader(new StringReader(buffer.toString()))) {
				return reader.read();
			}

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	private void assertEquivalent(final String message, final JsonValue expected, final JsonValue actual) {
		assertThat((Object)strip(expected)).as(message).isEqualTo(strip(actual));
	}


	private JsonValue strip(final JsonValue value) {
		return value instanceof JsonArray ? strip((JsonArray)value)
				: value instanceof JsonObject ? strip((JsonObject)value)
				: value;
	}

	private JsonArray strip(final JsonArray array) {

		final JsonArrayBuilder builder=Json.createArrayBuilder();

		for (final JsonValue value : array) {
			builder.add(strip(value));
		}

		return builder.build();
	}

	private JsonObject strip(final JsonObject object) {

		final JsonObjectBuilder builder=Json.createObjectBuilder();

		for (final Map.Entry<String, JsonValue> entry : object.entrySet()) {

			final String key=entry.getKey();
			final JsonValue value=entry.getValue();

			if ( !(key.equals("this") && value instanceof JsonString && ((JsonString)value).getString().startsWith("_:")) ) {
				builder.add(key, strip(value));
			}
		}

		return builder.build();
	}

}
