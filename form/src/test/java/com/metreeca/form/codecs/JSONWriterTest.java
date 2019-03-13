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
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Field;
import com.metreeca.form.shapes.Or;
import com.metreeca.form.things.ValuesTest;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFWriter;
import org.eclipse.rdf4j.rio.Rio;
import org.junit.jupiter.api.Test;

import java.io.*;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Map;
import java.util.function.Function;

import javax.json.*;

import static com.metreeca.form.Shape.multiple;
import static com.metreeca.form.Shape.required;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.things.JsonValues.array;
import static com.metreeca.form.things.JsonValues.object;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Maps.union;
import static com.metreeca.form.things.Values.*;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.form.truths.JsonAssert.assertThat;


final class JSONWriterTest  {

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
						entry("this", "_:x"),
						entry(value, list("x"))
				)));
	}

	@Test void testNamedObjects() {
		assertThat(write(decode("<x> rdf:value 'x'.")))
				.as("named objects")
				.isEqualTo(array(map(
						entry("this", "/x"),
						entry(value, list("x"))
				)));
	}

	@Test void testTypedObjects() {

		final Function<Object, JsonValue> values=(v) -> array(union(
				map(entry("this", format(bnode()))),
				map(entry(value, list(v)))
		));

		assertEquivalent("boolean", values.apply(true), write(decode("_:focus rdf:value true .")));
		assertEquivalent("string", values.apply("string"), write(decode("[] rdf:value 'string' .")));
		assertEquivalent("integer", values.apply(BigInteger.ONE), write(decode("[] rdf:value 1 .")));
		assertEquivalent("decimal", values.apply(new BigDecimal("1.0")), write(decode("[] rdf:value 1.0 .")));
		assertEquivalent("double", values.apply(1.0), write(decode("[] rdf:value 1e0 .")));

		assertEquivalent("numeric",
				values.apply(map(entry("text", "1"), entry("type", XMLSchema.INT.stringValue()))),
				write(decode("[] rdf:value '1'^^xsd:int .")));

		assertEquivalent("custom",
				values.apply(map(entry("text", "text"), entry("type", ValuesTest.term("type").stringValue()))),
				write(decode("[] rdf:value 'text'^^:type .")));

		assertEquivalent("tagged",
				values.apply(map(entry("text", "text"), entry("lang", "en"))),
				write(decode("[] rdf:value 'text'@en .")));

		assertEquivalent("malformed",
				values.apply(map(entry("text", "malformed"), entry("type", XMLSchema.BOOLEAN.stringValue()))),
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
						entry("this", "/x"),
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
						entry("this", "/x"),
						entry(value, list(map(
								entry("this", "/w"),
								entry(value, list(map(entry("this", "/z"))))
						), map(
								entry("this", "/y"),
								entry(value, list(map(entry("this", "/z"))))
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
						entry("this", "/x"),
						entry(value, list(map(
								entry("this", "/y"),
								entry(value, list(map(entry("this", "/x"))))
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
						entry("this", "_:x"),
						entry(value, list(map(
								entry("this", "_:y"),
								entry(value, list(map(entry("this", "_:x"))))
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
						Field.field(RDF.VALUE)
				));

		assertEquivalent("inverse inferred",
				object(map(entry("valueOf", list(map())))),
				write(
						decode("_:y rdf:value _:x ."),
						bnode("x"),
						Field.field(inverse(RDF.VALUE))
				));

		assertEquivalent("user-defined",
				object(map(entry("alias", list(map())))),
				write(
						decode("_:x rdf:value _:y ."),
						bnode("x"),
						Field.field(RDF.VALUE, alias("alias"))
				));

	}

	@Test void testAliasNestedFields() {

		assertEquivalent("aliased nested field",
				object(map(entry("value", list(map(entry("alias", list(map()))))))),
				write(
						decode("_:x rdf:value [rdf:value _:y] ."),
						bnode("x"),
						Field.field(RDF.VALUE, Field.field(RDF.VALUE, alias("alias")))
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
								Field.field(RDF.VALUE),
								Field.field(ValuesTest.term("value"))
						)
				));
	}

	@Test void testIgnoreReservedAliases() {
		assertEquivalent("reserved alias",
				object(map(entry("value", list(map())))),
				write(
						decode("_:x rdf:value _:y ."),
						bnode("x"),
						Field.field(RDF.VALUE, alias("this"))
				));
	}


	//// IRIs //////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRelativizeProvedIRIs() {

		final IRI focus=item("/container/");

		final JsonStructure json=write(
				decode("</container/> ldp:contains </container/x>, </container/y>."),
				focus,
				Field.field(LDP.CONTAINS, and(multiple(), datatype(Form.IRIType))),
				focus.stringValue()
		);

		assertThat(json)
				.isEqualTo(object(
						entry("this", "/container/"),
						entry("contains", list(
								"/container/x",
								"/container/y"
						))
				));
	}

	@Test void testRelativizeProvedIRIBackReferences() {

		final IRI focus=item("/container/");

		final JsonStructure json=write(
				decode("</container/> rdf:value </container/>."),
				focus,
				Field.field(RDF.VALUE, and(required(), datatype(Form.IRIType))),
				focus.stringValue()
		);

		assertThat(json)
				.isEqualTo(object(map(
						entry("this", "/container/"),
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
						decode("_:focus rdf:first 'x'; rdf:rest 'y'."), // invalid shape (forces content on both branches)
						bnode("focus"),
						Or.or(
								Field.field(RDF.FIRST, required()),
								Field.field(RDF.REST, required())
						)
				));
	}

	@Test void testWriteNamedReverseLinks() {
		assertThat(write(
				decode("<y> rdf:value <x> ."),
				iri("http://example.com/x"),
				Field.field(inverse(RDF.VALUE))
		))
				.as("named reverse links")
				.isEqualTo(object(map(
						entry("this", "/x"),
						entry("valueOf", list(map(
								entry("this", "/y")
						)))
				)));
	}

	@Test void testWriteBlankReverseLinks() {
		assertEquivalent("blank reverse links",
				object(map(entry("valueOf", list(map())))),
				write(
						decode("_:y rdf:value _:x ."),
						bnode("x"),
						Field.field(inverse(RDF.VALUE))
				));
	}

	@Test void testOmitNullValues() {
		assertEquivalent("omitted empty array",
				object(map()),
				write(
						decode("_:focus rdf:value 'x'."),
						bnode("focus"),
						Field.field(RDF.TYPE, required())
				));

	}

	@Test void testOmitEmptyArrays() {
		assertEquivalent("omitted empty array",
				object(map()),
				write(
						decode("_:focus rdf:value 'x'."),
						bnode("focus"),
						Field.field(RDF.TYPE)
				));

	}

	@Test void testWriteObjectInsteadOfArrayIfNotRepeatable() {
		assertEquivalent("simplified unrepeatable value",
				object(map(entry("value", "x"))),
				write(
						decode("_:focus rdf:value 'x'."),
						bnode("focus"),
						Field.field(RDF.VALUE, maxCount(1))
				));
	}

	@Test void testInlineProvedLeafIRIs() {
		assertEquivalent("simplified leaf IRI",
				object(map(entry("value", RDF.NIL.stringValue()))),
				write(
						decode("_:focus rdf:value rdf:nil."),
						bnode("focus"),
						Field.field(RDF.VALUE, and(datatype(Form.IRIType), maxCount(1)))
				));
	}

	@Test void testInlineProvedTypedLiterals() {
		assertEquivalent("simplified literal with known datatype",
				object(map(entry("value", "2016-08-11"))),
				write(
						decode("_:focus rdf:value '2016-08-11'^^xsd:date."),
						bnode("focus"),
						Field.field(RDF.VALUE, and(datatype(XMLSchema.DATE), maxCount(1)))
				));
	}

	@Test void testOmitThisForUnreferencedProvedBlanks() {

		assertThat(write(
				decode("_:x rdf:value _:y ."),
				bnode("x"),
				and(datatype(Form.BNodeType), Field.field(RDF.VALUE, datatype(Form.BNodeType)))
		))
				.as("unreferenced proved blank")
				.isEqualTo(object(map(entry("value", list(map())))));

		assertThat(write(
				decode("_:x rdf:value _:x ."),
				bnode("x"),
				and(datatype(Form.BNodeType), Field.field(RDF.VALUE, datatype(Form.BNodeType)))
		))
				.as("back-referenced proved blank")
				.isEqualTo(object(map(
						entry("this", "_:x"),
						entry("value", list(map(entry("this", "_:x")))))));

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

	private JsonStructure write(final Iterable<Statement> model, final Resource focus, final Shape shape, final String base) {
		try (final StringWriter buffer=new StringWriter(1000)) {

			final RDFWriter writer=new JSONWriter(buffer, base);

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

			if ( !(key.equals("this") && value instanceof JsonString && ((JsonString)value).getString().startsWith("_:")) ) {
				builder.add(key, strip(value));
			}
		}

		return builder.build();
	}

}
