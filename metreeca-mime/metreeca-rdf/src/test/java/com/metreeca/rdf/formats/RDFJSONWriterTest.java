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

import com.metreeca.rdf.ValuesTest;
import com.metreeca.tree.Shape;
import com.metreeca.tree.shapes.Or;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

import javax.json.*;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;

import static com.metreeca.rest.formats.JSONAssert.assertThat;
import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.ValuesTest.decode;
import static com.metreeca.rdf.ValuesTest.item;
import static com.metreeca.rdf.formats.RDFJSONTest.*;
import static com.metreeca.tree.Shape.multiple;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Datatype.datatype;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.Meta.alias;


final class RDFJSONWriterTest {

	private final String value=RDF.VALUE.stringValue();


	//// Objects ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testNoObjects() {
		assertThat(write(decode("")))
				.as("no objects")
				.isEqualTo(array());
	}

	@Test void testBlankObjects() {
		assertThat(write(decode("_:x rdf:value 'x'.")))
				.as("blank objects")
				.isEqualTo(array(map(
						entry(value, list("x"))
				)));
	}

	@Test void testNamedObjects() {
		assertThat(write(decode("<x> rdf:value 'x'.")))
				.as("named objects")
				.isEqualTo(array(map(
						entry("id", "/x"),
						entry(value, list("x"))
				)));
	}

	@Test void testTypedObjects() {

		final Function<Object, JsonValue> values=v -> array(map(
				entry("id", format(bnode())),
				entry(value, list(v))
		));

		assertEquivalent("boolean", values.apply(true), write(decode("_:focus rdf:value true .")));
		assertEquivalent("string", values.apply("string"), write(decode("[] rdf:value 'string' .")));
		assertEquivalent("integer", values.apply(BigInteger.ONE), write(decode("[] rdf:value 1 .")));
		assertEquivalent("decimal", values.apply(new BigDecimal("1.0")), write(decode("[] rdf:value 1.0 .")));

		assertEquivalent("numeric",
				values.apply(map(entry("value", "1"), entry("type", XMLSchema.INT.stringValue()))),
				write(decode("[] rdf:value '1'^^xsd:int .")));

		assertEquivalent("custom",
				values.apply(map(entry("value", "text"), entry("type", ValuesTest.term("type").stringValue()))),
				write(decode("[] rdf:value 'text'^^:type .")));

		assertEquivalent("tagged",
				values.apply(map(entry("value", "text"), entry("language", "en"))),
				write(decode("[] rdf:value 'text'@en .")));

		assertEquivalent("malformed",
				values.apply(map(entry("value", "malformed"), entry("type", XMLSchema.BOOLEAN.stringValue()))),
				write(decode("[] rdf:value 'malformed'^^xsd:boolean .")));

	}


	//// Focus /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testWriteOnlyFocusNode() {
		assertThat(write(
				decode("<x> rdf:value 'x' . <y> rdf:value 'y' ."),
				iri("http://example.com/x")
		))
				.as("focus node only")
				.isEqualTo(object(map(
						entry("id", "/x"),
						entry(value, list("x"))
				)));
	}

	@Test void testHandleUnknownFocusNode() {
		assertEquivalent("unknown focus",
				object(map()),
				write(
						decode("<x> rdf:value 'x' . <y> rdf:value 'y' ."),
						bnode()
				));
	}


	//// Shared References /////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testExpandSharedTrees() {
		assertThat(write(
				decode("<x> rdf:value <w>, <y>. <w> rdf:value <z>. <y> rdf:value <z>."),
				iri("http://example.com/x")
		))
				.as("expanded shared trees")
				.isEqualTo(object(map(
						entry("id", "/x"),
						entry(value, list(map(
								entry("id", "/w"),
								entry(value, list(map(entry("id", "/z"))))
						), map(
								entry("id", "/y"),
								entry(value, list(map(entry("id", "/z"))))
						)))
				)));
	}

	@Test void testHandleNamedLoops() {
		assertThat(write(
				decode("<x> rdf:value <y>. <y> rdf:value <x>."),
				iri("http://example.com/x")
		))
				.as("named loops")
				.isEqualTo(object(map(
						entry("id", "/x"),
						entry(value, list(map(
								entry("id", "/y"),
								entry(value, list(map(entry("id", "/x"))))
						)))
				)));
	}

	@Test void testHandleBlankLoops() {
		assertThat(write(
				decode("_:x rdf:value _:y. _:y rdf:value _:x."),
				bnode("x")
		))
				.as("named loops")
				.isEqualTo(object(map(
						entry("id", "_:x"),
						entry(value, list(map(
								entry(value, list(map(entry("id", "_:x"))))
						)))
				)));
	}


	//// Aliases ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testAliasFields() {

		assertEquivalent("direct inferred",
				object(map(entry("value", list(map())))),
				write(
						decode("_:x rdf:value _:y ."),
						bnode("x"),
						field(RDF.VALUE)
				));

		assertEquivalent("inverse inferred",
				object(map(entry("valueOf", list(map())))),
				write(
						decode("_:y rdf:value _:x ."),
						bnode("x"),
						field(inverse(RDF.VALUE))
				));

		assertEquivalent("user-defined",
				object(map(entry("alias", list(map())))),
				write(
						decode("_:x rdf:value _:y ."),
						bnode("x"),
						field(RDF.VALUE, alias("alias"))
				));

	}

	@Test void testAliasNestedFields() {

		assertEquivalent("aliased nested field",
				object(map(entry("value", list(map(entry("alias", list(map()))))))),
				write(
						decode("_:x rdf:value [rdf:value _:y] ."),
						bnode("x"),
						field(RDF.VALUE, field(RDF.VALUE, alias("alias")))
				));

	}

	@Test void testHandleAliasClashes() {
		assertEquivalent("clashing aliases",
				object(map(
						entry(value, list(map())),
						entry(ValuesTest.term("value").stringValue(), list(map()))
				)),
				write(
						decode("_:x rdf:value _:y; :value _:z."),
						bnode("x"),
						and(
								field(RDF.VALUE),
								field(ValuesTest.term("value"))
						)
				));
	}

	@Test void testIgnoreReservedAliases() {
		assertEquivalent("reserved alias",
				object(map(entry("value", list(map())))),
				write(
						decode("_:x rdf:value _:y ."),
						bnode("x"),
						field(RDF.VALUE, alias("@id"))
				));
	}


	//// IRIs //////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRelativizeProvedIRIs() {

		final IRI focus=item("/container/");

		final JsonStructure json=write(
				decode("</container/> ldp:contains </container/x>, </container/y>."),
				focus,
				field(LDP.CONTAINS, and(multiple(), datatype(IRIType))),
				focus.stringValue()
		);

		assertThat(json)
				.isEqualTo(object(map(
						entry("id", "/container/"),
						entry("contains", list(
								"/container/x",
								"/container/y"
						))
				)));
	}

	@Test void testRelativizeProvedIRIBackReferences() {

		final IRI focus=item("/container/");

		final JsonStructure json=write(
				decode("</container/> rdf:value </container/>."),
				focus,
				field(RDF.VALUE, and(required(), datatype(IRIType))),
				focus.stringValue()
		);

		assertThat(json)
				.isEqualTo(object(map(
						entry("id", "/container/"),
						entry("value", "/container/")
				)));
	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testConsiderDisjunctiveDefinitions() {
		assertEquivalent("simplified literal with known datatype",
				object(map(
						entry("first", "x"),
						entry("rest", "y")
				)),
				write(
						decode("_:focus rdf:first 'x'; rdf:rest 'y'."), // invalid shape (forces content on both 
						// branches)
						bnode("focus"),
						Or.or(
								field(RDF.FIRST, required()),
								field(RDF.REST, required())
						)
				));
	}

	@Test void testWriteNamedReverseLinks() {
		assertThat(write(
				decode("<y> rdf:value <x> ."),
				iri("http://example.com/x"),
				field(inverse(RDF.VALUE))
		))
				.as("named reverse links")
				.isEqualTo(object(map(
						entry("id", "/x"),
						entry("valueOf", list("/y"))
				)));
	}

	@Test void testWriteBlankReverseLinks() {
		assertEquivalent("blank reverse links",
				object(map(entry("valueOf", list(map())))),
				write(
						decode("_:y rdf:value _:x ."),
						bnode("x"),
						field(inverse(RDF.VALUE))
				));
	}

	@Test void testOmitNullValues() {
		assertEquivalent("omitted empty array",
				object(map()),
				write(
						decode("_:focus rdf:value 'x'."),
						bnode("focus"),
						field(RDF.TYPE, required())
				));

	}

	@Test void testOmitEmptyArrays() {
		assertEquivalent("omitted empty array",
				object(map()),
				write(
						decode("_:focus rdf:value 'x'."),
						bnode("focus"),
						field(RDF.TYPE)
				));

	}

	@Test void testWriteObjectInsteadOfArrayIfNotRepeatable() {
		assertEquivalent("simplified unrepeatable value",
				object(map(entry("value", "x"))),
				write(
						decode("_:focus rdf:value 'x'."),
						bnode("focus"),
						field(RDF.VALUE, maxCount(1))
				));
	}

	@Test void testInlineProvedLeafIRIs() {
		assertEquivalent("simplified leaf IRI",
				object(map(entry("value", RDF.NIL.stringValue()))),
				write(
						decode("_:focus rdf:value rdf:nil."),
						bnode("focus"),
						field(RDF.VALUE, and(datatype(IRIType), maxCount(1)))
				));
	}

	@Test void testInlineProvedTypedLiterals() {
		assertEquivalent("simplified literal with known datatype",
				object(map(entry("value", "2016-08-11"))),
				write(
						decode("_:focus rdf:value '2016-08-11'^^xsd:date."),
						bnode("focus"),
						field(RDF.VALUE, and(datatype(XMLSchema.DATE), maxCount(1)))
				));
	}

	@Test void testOmitThisForUnreferencedProvedBlanks() {

		assertThat(write(
				decode("_:x rdf:value _:y ."),
				bnode("x"),
				and(datatype(BNodeType), field(RDF.VALUE, datatype(BNodeType)))
		))
				.as("unreferenced proved blank")
				.isEqualTo(object(map(entry("value", list(map())))));

		assertThat(write(
				decode("_:x rdf:value _:x ."),
				bnode("x"),
				and(datatype(BNodeType), field(RDF.VALUE, datatype(BNodeType)))
		))
				.as("back-referenced proved blank")
				.isEqualTo(object(map(
						entry("id", "_:x"),
						entry("value", list("_:x")))
				));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private JsonStructure write(final Iterable<Statement> model) {
		return write(model, null);
	}

	private JsonStructure write(final Iterable<Statement> model, final Resource focus) {
		return write(model, focus, null);
	}

	private JsonStructure write(final Iterable<Statement> model, final Resource focus, final Shape shape) {
		return write(model, focus, shape, ValuesTest.Base);
	}

	private JsonStructure write(final Iterable<Statement> model, final Resource focus, final Shape shape,
			final String base) {
		try ( final StringWriter buffer=new StringWriter(1000) ) {

			final RDFWriter writer=new RDFJSONWriter(buffer, base);

			writer.set(RDFFormat.RioFocus, focus);
			writer.set(RDFFormat.RioShape, shape);
			writer.set(RDFFormat.RioContext, RDFJSONCodecTest.Context);

			Rio.write(model, writer);

			try ( final JsonReader reader=Json.createReader(new StringReader(buffer.toString())) ) {
				return reader.read();
			}

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}


	//// !!! review ////////////////////////////////////////////////////////////////////////////////////////////////////

	private void assertEquivalent(final String message, final JsonValue expected, final JsonValue actual) {
		assertThat(strip(actual))
				.as(message)
				.isEqualTo(strip(expected));
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

			if ( !(key.equals("id") && value instanceof JsonString && ((JsonString)value).getString().startsWith("_:")) ) {
				builder.add(key, strip(value));
			}
		}

		return builder.build();
	}

}
