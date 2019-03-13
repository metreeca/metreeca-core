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
import com.metreeca.form.shapes.All;
import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;

import static com.metreeca.form.Shape.required;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.things.JsonValues.array;
import static com.metreeca.form.things.JsonValues.json;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Maps.union;
import static com.metreeca.form.things.Values.*;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.truths.ModelAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


final class JSONParserTest {

	private final String value=RDF.VALUE.stringValue();


	private Object blanks(final Object... json) {
		return list(blank(entry(value, list(json))));
	}

	@SafeVarargs private final Map<String, Object> blank(final Map.Entry<String, Object>... fields) {
		return union(
				map(entry("this", format(bnode()))),
				map(fields)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testReportRDFParseException() {
		assertThatExceptionOfType(RDFParseException.class).isThrownBy(() ->
				new JSONParser().parse(new StringReader(""), "")
		);
	}


	//// Objects ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testNoObjects() {
		assertThat(rdf(map()))
				.as("empty object")
				.isEqualTo(decode(""));
		assertThat(rdf(array()))
				.as("empty array")
				.isEqualTo(decode(""));
	}

	@Test void testBlankObjects() {

		assertThat(rdf(blanks(map(entry("this", "_:x")))))
				.as("blank object")
				.isEqualTo(decode("[] rdf:value [] ."));

		assertThat(rdf(blanks(map(entry("this", "")))))
				.as("empty id blank object")
				.isEqualTo(decode("[] rdf:value [] ."));

		assertThat(rdf(blanks(map(entry("this", null)))))
				.as("null id blank object")
				.isEqualTo(decode("[] rdf:value [] ."));

		assertThat(rdf(blanks(map(entry("this", "_:x")))).stream()
				.allMatch(statement -> statement.getObject().equals(bnode("x"))))
				.as("preserve bnode id")
				.isTrue();

		assertThat(rdf(blanks("_:x"), null, field(RDF.VALUE, datatype(Form.BNodeType))).stream()
				.allMatch(statement -> statement.getObject().equals(bnode("x"))))
				.as("preserve bnode id / shorthand")
				.isTrue();

	}

	@Test void testNamedObjects() {

		assertThat(rdf(map(
				entry("this", "http://example.com/x"),
				entry(value, map(entry("this", "http://example.com/y"))),
				entry("^"+value, map(entry("this", "http://example.com/z")))
		)))
				.as("named objects with naked predicate IRIs")
				.isEqualTo(decode("<x> rdf:value <y>. <z> rdf:value <x>."));

		assertThat(rdf(map(
				entry("this", "http://example.com/x"),
				entry("<"+value+">", map(entry("this", "http://example.com/y"))),
				entry("^<"+value+">", map(entry("this", "http://example.com/z")))
		)))
				.as("named objects with bracketed predicate IRIs")
				.isEqualTo(decode("<x> rdf:value <y>. <z> rdf:value <x>."));

	}

	@Test void testTypedObjects() {

		assertThat(rdf(blanks((Object)null)))
				.as("null")
				.isEqualTo(decode(""));
		assertThat(rdf(blanks(true)))
				.as("boolean")
				.isEqualTo(decode("[] rdf:value true ."));
		assertThat(rdf(blanks("string")))
				.as("string")
				.isEqualTo(decode("[] rdf:value 'string' ."));
		assertThat(rdf(blanks(new BigDecimal("1.0"))))
				.as("decimal")
				.isEqualTo(decode("[] rdf:value 1.0 ."));
		assertThat(rdf(blanks(BigInteger.ONE)))
				.as("integer")
				.isEqualTo(decode("[] rdf:value 1 ."));

		// !!! unable to test for exponent presence using JSON-P
		//assertEquals("double", parse("[] rdf:value 1.0E0 ."), // special support for doubles
		//		rdf("[{\"this\": \"_:node1bcl7j42cx10\", \"http://www.w3.org/1999/02/22-rdf-syntax-ns#value\": [1.0e0]}]"));

		assertThat(rdf(blanks(map(entry("text", "1"), entry("type", XMLSchema.INT.stringValue())))))
				.as("numeric")
				.isEqualTo(decode("[] rdf:value '1'^^xsd:int ."));

		assertThat(rdf(blanks(map(entry("text", "text"), entry("type", ValuesTest.term("type").stringValue())))))
				.as("custom")
				.isEqualTo(decode("[] rdf:value 'text'^^:type ."));

		assertThat(rdf(blanks(map(entry("text", "text"), entry("lang", "en")))))
				.as("tagged")
				.isEqualTo(decode("[] rdf:value 'text'@en ."));

	}


	//// Focus /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseOnlyFocusNode() {
		assertThat(rdf(
				list(map(entry("this", "http://example.com/x"), entry(value, "x")), map(entry("this", "http://example.com/y"), entry(value, "y"))),
				iri("http://example.com/x")
		))
				.as("focus node only")
				.isEqualTo(decode("<x> rdf:value 'x' ."));
	}

	@Test void testHandleUnknownFocusNode() {
		assertThat(rdf(
				map(entry("this", "http://example.com/x"), entry(value, "x")),
				bnode()
		))
				.as("unknown focus")
				.isEqualTo(decode(""));
	}

	@Test void testAssumeFocusAsSubject() {
		assertThat(rdf(
				list(map(entry(value, "x"))),
				iri("http://example.com/x")
		))
				.as("focus assumed as subject")
				.isEqualTo(decode("<x> rdf:value 'x' ."));
	}


	//// Shared References /////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testHandleNamedLoops() {
		assertThat(rdf(map(
				entry("this", "http://example.com/x"),
				entry(value, map(
						entry("this", "http://example.com/y"),
						entry(value, list(map(entry("this", "http://example.com/x"))))
				))
		)))
				.as("named loops")
				.isEqualTo(decode("<x> rdf:value <y>. <y> rdf:value <x>."));
	}

	@Test void testHandleBlankLoops() {
		assertThat(rdf(map(
				entry("this", "_:a"),
				entry(value, blank(entry(value, map(entry("this", "_:a")))))
		)))
				.as("named loops")
				.isEqualTo(decode("_:x rdf:value [rdf:value _:x] ."));
	}

	@Test void testInlineProvedIRIBackReferences() {
		assertThat(rdf(

				map(
						entry("this", "http://example.com/x"),
						entry(value, "http://example.com/x")
				),

				null,

				field(RDF.VALUE, and(required(), datatype(Form.IRIType)))

		))
				.as("inlined IRI back-reference")
				.isIsomorphicTo(decode("<x> rdf:value <x>."));
	}

	@Test void testInlineProvedBnodeBackReferences() {
		assertThat(rdf(

				map(
						entry("this", "_:x"),
						entry(value, "_:x")
				),

				null,

				field(RDF.VALUE, and(required(), datatype(Form.BNodeType)))

		))
				.as("inlined IRI back-reference")
				.isIsomorphicTo(decode("_:x rdf:value _:x ."));
	}


	//// Aliases ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseAliasedFields() {

		assertThat(rdf(
				blank(entry("value", blank())),
				null,
				field(RDF.VALUE)
		))
				.as("direct inferred")
				.isEqualTo(decode("[] rdf:value [] ."));

		assertThat(rdf(
				blank(entry("valueOf", blank())),
				null,
				field(inverse(RDF.VALUE))
		))
				.as("inverse inferred")
				.isEqualTo(decode("[] rdf:value [] ."));

		assertThat(rdf(
				blank(entry("alias", blank())),
				null,
				field(RDF.VALUE, alias("alias"))
		))
				.as("user-defined")
				.isEqualTo(decode("[] rdf:value [] ."));

	}

	@Test void testParseAliasedNestedFields() {

		Assertions.assertThat(rdf(
				blank(entry("value", blank(entry("alias", blank())))),
				null,
				field(RDF.VALUE, field(RDF.VALUE, alias("alias")))
		))
				.as("aliased nested field")
				.isEqualTo(decode("[] rdf:value [rdf:value []] ."));

	}

	@Test void testHandleAliasClashes() {
		assertThatExceptionOfType(RDFParseException.class).isThrownBy(() -> rdf(
				map(entry("value", map())),
				null,
				and(
						field(RDF.VALUE),
						field(ValuesTest.term("value"))
				)
		));
	}

	@Test void testResolveAliasesOnlyIfShapeIsSet() {
		assertThatExceptionOfType(RDFParseException.class).isThrownBy(() -> rdf(
				map(entry("this", "_:x"), entry("value", map())),
				null,
				null
		));
	}


	//// Focus-Relative Paths //////////////////////////////////////////////////////////////////////////////////////////

	@Test void testResolveRelativeIRIs() {

		final BiFunction<String, String, Collection<Statement>> statament=(s, o) -> rdf(
				map(entry("this", s), entry(this.value, o)),
				null,
				field(RDF.VALUE, datatype(Form.IRIType)),
				ValuesTest.Base+"relative/"
		);

		assertThat(statament.apply("x", "http://example.com/y"))
				.as("base relative subject")
				.isEqualTo(decode("<http://example.com/relative/x> rdf:value <y> ."));

		assertThat(statament.apply("/x", "http://example.com/y"))
				.as("root relative subject")
				.isEqualTo(decode("<http://example.com/x> rdf:value <y>."));

		assertThat(statament.apply("http://example.com/absolute/x", "http://example.com/y"))
				.as("absolute subject")
				.isEqualTo(decode("<http://example.com/absolute/x> rdf:value <y>."));

		assertThat(statament.apply("http://example.com/x", "y"))
				.as("base relative object")
				.isEqualTo(decode("<x> rdf:value <http://example.com/relative/y> ."));

		assertThat(statament.apply("http://example.com/x", "/y"))
				.as("root relative object")
				.isEqualTo(decode("<x> rdf:value <http://example.com/y>."));

		assertThat(statament.apply("http://example.com/x", "http://example.com/absolute/y"))
				.as("absolute object")
				.isEqualTo(decode("<x> rdf:value <http://example.com/absolute/y>."));

	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseNamedReverseLinks() {
		assertThat(rdf(
				map(
						entry("this", "http://example.com/x"),
						entry("valueOf", map(
								entry("this", "http://example.com/y")
						))
				),
				null,
				field(inverse(RDF.VALUE))
		))
				.as("named reverse links")
				.isEqualTo(decode("<y> rdf:value <x> ."));
	}

	@Test void testParseBlankReverseLinks() {
		assertThat(rdf(
				blank(entry("valueOf", blank())),
				null,
				field(inverse(RDF.VALUE))
		))
				.as("blank reverse links")
				.isEqualTo(decode("[] rdf:value [] ."));
	}

	@Test void testParseInlinedProvedTypedLiterals() {
		assertThat(rdf(
				blank(entry("value", "2016-08-11")),
				null,
				field(RDF.VALUE, datatype(XMLSchema.DATE))
		))
				.as("simplified literal with known datatype")
				.isEqualTo(decode("[] rdf:value '2016-08-11'^^xsd:date."));
	}

	@Test void testParseThisLessProvedBlanks() {
		assertThat(rdf(
				map(entry("value", map())),
				null,
				and(datatype(Form.BNodeType), field(RDF.VALUE, datatype(Form.BNodeType)))
		))
				.as("proved blanks")
				.isEqualTo(decode("[] rdf:value [] ."));
	}

	@Test void testParseThisLessProvedNameds() {
		assertThat(rdf(
				map(entry("value", blank())),
				null,
				and(All.all(iri("http://example.com/x")), field(RDF.VALUE))
		))
				.as("proved named")
				.isEqualTo(decode("<x> rdf:value [] ."));
	}

	@Test void testParseIDOnlyProvedBlanks() {
		assertThat(rdf(
				blanks("_:x"),
				null,
				field(RDF.VALUE, datatype(Form.BNodeType))
		))
				.as("proved blank")
				.isEqualTo(decode("[] rdf:value [] ."));
	}

	@Test void testParseIRIOnlyProvedNameds() {
		assertThat(rdf(
				blanks("http://example.com/x"),
				null,
				field(RDF.VALUE, datatype(Form.IRIType))
		))
				.as("proved named")
				.isEqualTo(decode("[] rdf:value <x> ."));
	}

	@Test void testParseStringOnlyProvedResources() {
		assertThat(rdf(
				blanks("_:x", "http://example.com/x"),
				null,
				field(RDF.VALUE, datatype(Form.ResourceType))
		))
				.as("proved resources")
				.isEqualTo(decode("[] rdf:value [], <x> ."));
	}

	@Test void testParseProvedDecimalsLeniently() {
		assertThat(rdf(
				blanks(Values.decimal(1), Values.integer(1), 1.0),
				null,
				field(RDF.VALUE, datatype(XMLSchema.DECIMAL))
		))
				.as("proved decimal")
				.isEqualTo(decode("[] rdf:value 1.0 ."));
	}

	@Test void testParseProvedDoublesLeniently() {
		assertThat(rdf(
				blanks(1.0, Values.integer(1), Values.decimal(1)),
				null,
				field(RDF.VALUE, datatype(XMLSchema.DOUBLE))
		))
				.as("proved decimal")
				.isEqualTo(decode("[] rdf:value 1.0E0 ."));
	}

	@Test void testRejectMalformedLiterals() {
		assertThatExceptionOfType(RDFParseException.class).isThrownBy(() ->
				rdf(blanks("22/5/2018"), null, field(RDF.VALUE, datatype(XMLSchema.DATETIME)))
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Model rdf(final Object json) {
		return rdf(json, null);
	}

	private Model rdf(final Object json, final Resource focus) {
		return rdf(json, focus, null);
	}

	private Model rdf(final Object json, final Resource focus, final Shape shape) {return rdf(json, focus, shape, ValuesTest.Base);}

	private Model rdf(final Object json, final Resource focus, final Shape shape, final String base) {
		try (final StringReader reader=new StringReader((json instanceof String ? json : json(json)).toString())) {

			final StatementCollector collector=new StatementCollector();

			final RDFParser parser=new JSONParser();

			parser.set(JSONCodec.Focus, focus);
			parser.set(JSONCodec.Shape, shape);

			parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

			parser.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
			parser.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, true);

			parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
			parser.set(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS, true);

			parser.setRDFHandler(collector);

			parser.parse(reader, base);

			return new LinkedHashModel(collector.getStatements());

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

}
