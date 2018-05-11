/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec.codecs;

import com.metreeca.spec.Shape;
import com.metreeca.spec.Values;
import com.metreeca.spec.shapes.Or;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.Test;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;

import javax.json.*;

import static com.metreeca.spec.Shape.required;
import static com.metreeca.spec.Values.bnode;
import static com.metreeca.spec.Values.iri;
import static com.metreeca.spec.ValuesTest.parse;
import static com.metreeca.spec.ValuesTest.term;
import static com.metreeca.spec.shapes.Alias.alias;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Datatype.datatype;
import static com.metreeca.spec.shapes.MaxCount.maxCount;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shifts.Step.step;

import static org.junit.Assert.assertEquals;


public final class JSONWriterTest extends JSONAdapterTest {

	//// Objects ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testNoObjects() {
		assertEquals("no objects",
				json(array()),
				json(parse("")));
	}

	@Test public void testBlankObjects() {
		assertEquals("blank objects",
				json(array(object(
						field("this", "_:x"),
						field(value, array("x"))
				))),
				json(parse("_:x rdf:value 'x'.")));
	}

	@Test public void testNamedObjects() {
		assertEquals("named objects",
				json(array(object(
						field("this", "http://example.com/x"),
						field(value, array("x"))
				))),
				json(parse("<x> rdf:value 'x'.")));
	}

	@Test public void testTypedObjects() {

		assertEquivalent("boolean", json(blanks(true)), json(parse("_:focus rdf:value true .")));
		assertEquivalent("string", json(blanks("string")), json(parse("[] rdf:value 'string' .")));
		assertEquivalent("integer", json(blanks(BigInteger.ONE)), json(parse("[] rdf:value 1 .")));
		assertEquivalent("decimal", json(blanks(new BigDecimal("1.0"))), json(parse("[] rdf:value 1.0 .")));
		assertEquivalent("double", json(blanks(1.0)), json(parse("[] rdf:value 1e0 .")));

		assertEquivalent("numeric",
				json(blanks(object(field("text", "1"), field("type", XMLSchema.INT.stringValue())))),
				json(parse("[] rdf:value '1'^^xsd:int .")));

		assertEquivalent("custom",
				json(blanks(object(field("text", "text"), field("type", term("type").stringValue())))),
				json(parse("[] rdf:value 'text'^^:type .")));

		assertEquivalent("tagged",
				json(blanks(object(field("text", "text"), field("lang", "en")))),
				json(parse("[] rdf:value 'text'@en .")));

		assertEquivalent("malformed",
				json(blanks(object(field("text", "malformed"), field("type", XMLSchema.BOOLEAN.stringValue())))),
				json(parse("[] rdf:value 'malformed'^^xsd:boolean .")));

	}


	//// Focus /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testWriteOnlyFocusNode() {
		assertEquals("focus node only",
				json(object(
						field("this", "http://example.com/x"),
						field(value, array("x"))
				)),
				json(
						parse("<x> rdf:value 'x' . <y> rdf:value 'y' ."),
						iri("http://example.com/x")
				));
	}

	@Test public void testHandleUnknownFocusNode() {
		assertEquivalent("unknown focus",
				json(object()),
				json(
						parse("<x> rdf:value 'x' . <y> rdf:value 'y' ."),
						bnode()
				));
	}


	//// Shared References /////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testExpandSharedTrees() {
		assertEquals("expanded shared trees",
				json(object(
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
				)),
				json(
						parse("<x> rdf:value <w>, <y>. <w> rdf:value <z>. <y> rdf:value <z>."),
						iri("http://example.com/x")
				));
	}

	@Test public void testHandleNamedLoops() {
		assertEquals("named loops",
				json(object(
						field("this", "http://example.com/x"),
						field(value, array(
								object(
										field("this", "http://example.com/y"),
										field(value, array(object(field("this", "http://example.com/x"))))
								)
						))
				)),
				json(
						parse("<x> rdf:value <y>. <y> rdf:value <x>."),
						iri("http://example.com/x")
				));
	}

	@Test public void testHandleBlankLoops() {
		assertEquals("named loops",
				json(object(
						field("this", "_:x"),
						field(value, array(
								object(
										field("this", "_:y"),
										field(value, array(object(field("this", "_:x"))))
								)
						))
				)),
				json(
						parse("_:x rdf:value _:y. _:y rdf:value _:x."),
						bnode("x")
				));
	}


	//// Aliases ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testAliasTraits() {

		assertEquivalent("direct inferred",
				json(object(field("value", array(object())))),
				json(
						parse("_:x rdf:value _:y ."),
						bnode("x"),
						trait(RDF.VALUE)
				));

		assertEquivalent("inverse inferred",
				json(object(field("valueOf", array(object())))),
				json(
						parse("_:y rdf:value _:x ."),
						bnode("x"),
						trait(step(RDF.VALUE, true))
				));

		assertEquivalent("user-defined",
				json(object(field("alias", array(object())))),
				json(
						parse("_:x rdf:value _:y ."),
						bnode("x"),
						trait(RDF.VALUE, alias("alias"))
				));

	}

	@Test public void testAliasNestedTraits() {

		assertEquivalent("aliased nested trait",
				json(object(field("value", array(object(field("alias", array(object()))))))),
				json(
						parse("_:x rdf:value [rdf:value _:y] ."),
						bnode("x"),
						trait(RDF.VALUE, trait(step(RDF.VALUE), alias("alias")))
				));

	}

	@Test public void testHandleAliasClashes() {
		assertEquivalent("clashing aliases",
				json(object(
						field(value, array(object())),
						field(term("value").stringValue(), array(object()))
				)),
				json(
						parse("_:x rdf:value _:y; :value _:z."),
						bnode("x"),
						and(
								trait(RDF.VALUE),
								trait(term("value"))
						)
				));
	}

	@Test public void testIgnoreReservedAliases() {
		assertEquivalent("reserved alias",
				json(object(field("value", array(object())))),
				json(
						parse("_:x rdf:value _:y ."),
						bnode("x"),
						trait(RDF.VALUE, alias("this"))
				));
	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testConsiderDisjunctiveDefinitions() {
		assertEquivalent("simplified literal with known datatype",
				json(object(
						field("first", "x"),
						field("rest", "y")
				)),
				json(
						parse("_:focus rdf:first 'x'; rdf:rest 'y'."), // invalid shape (forces content on both branches)
						bnode("focus"),
						Or.or(
								trait(RDF.FIRST, required()),
								trait(RDF.REST, required())
						)
				));
	}

	@Test public void testWriteNamedReverseLinks() {
		assertEquals("named reverse links",
				json(object(
						field("this", "http://example.com/x"),
						field("valueOf", array(object(
								field("this", "http://example.com/y")
						)))
				)),
				json(
						parse("<y> rdf:value <x> ."),
						iri("http://example.com/x"),
						trait(step(RDF.VALUE, true))
				));
	}

	@Test public void testWriteBlankReverseLinks() {
		assertEquivalent("blank reverse links",
				json(object(field("valueOf", array(object())))),
				json(
						parse("_:y rdf:value _:x ."),
						bnode("x"),
						trait(step(RDF.VALUE, true))
				));
	}

	@Test public void testOmitNullValues() {
		assertEquivalent("omitted empty array",
				json(object()),
				json(
						parse("_:focus rdf:value 'x'."),
						bnode("focus"),
						trait(RDF.TYPE, required())
				));

	}

	@Test public void testOmitEmptyArrays() {
		assertEquivalent("omitted empty array",
				json(object()),
				json(
						parse("_:focus rdf:value 'x'."),
						bnode("focus"),
						trait(RDF.TYPE)
				));

	}

	@Test public void testWriteObjectInsteadOfArrayIfNotRepeatable() {
		assertEquivalent("simplified unrepeatable value",
				json(object(field("value", "x"))),
				json(
						parse("_:focus rdf:value 'x'."),
						bnode("focus"),
						trait(RDF.VALUE, maxCount(1))
				));
	}

	@Test public void testInlineProvedLeafIRIs() {
		assertEquivalent("simplified leaf IRI",
				json(object(field("value", RDF.NIL.stringValue()))),
				json(
						parse("_:focus rdf:value rdf:nil."),
						bnode("focus"),
						trait(RDF.VALUE, and(datatype(Values.IRIType), maxCount(1)))
				));
	}

	@Test public void testInlineProvedTypedLiterals() {
		assertEquivalent("simplified literal with known datatype",
				json(object(field("value", "2016-08-11"))),
				json(
						parse("_:focus rdf:value '2016-08-11'^^xsd:date."),
						bnode("focus"),
						trait(RDF.VALUE, and(datatype(XMLSchema.DATE), maxCount(1)))
				));
	}

	@Test public void testOmitThisForUnreferencedProvedBlanks() {

		assertEquals("unreferenced proved blank",
				json(object(field("value", array(object())))),
				json(
						parse("_:x rdf:value _:y ."),
						bnode("x"),
						and(datatype(Values.BNodeType), trait(RDF.VALUE, datatype(Values.BNodeType)))
				));

		assertEquals("back-referenced proved blank",
				json(object(
						field("this", "_:x"),
						field("value", array(object(field("this", "_:x")))))),
				json(
						parse("_:x rdf:value _:x ."),
						bnode("x"),
						and(datatype(Values.BNodeType), trait(RDF.VALUE, datatype(Values.BNodeType)))
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

			writer.set(JSONAdapter.Focus, focus);
			writer.set(JSONAdapter.Shape, shape);

			Rio.write(model, writer);

			try (final JsonReader reader=Json.createReader(new StringReader(buffer.toString()))) {
				return reader.read();
			}

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	private void assertEquivalent(final String message, final JsonValue expected, final JsonValue actual) {
		assertEquals(message, strip(expected), strip(actual));
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
