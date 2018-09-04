/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

import com.metreeca.form.shapes.All;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.Alias;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.rio.RDFParseException;
import org.eclipse.rdf4j.rio.RDFParser;
import org.eclipse.rdf4j.rio.helpers.BasicParserSettings;
import org.eclipse.rdf4j.rio.helpers.StatementCollector;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.BigInteger;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.things.ValuesTest.decode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;


public final class JSONParserTest extends JSONAdapterTest {

	@Test(expected=RDFParseException.class) public void testReportRDFParseException() {
		new JSONParser().parse(new StringReader(""), "");
	}


	//// Objects ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testNoObjects() {
		Assert.assertEquals("empty object", ValuesTest.decode(""), rdf(object()));
		assertEquals("empty array", ValuesTest.decode(""), rdf(array()));
	}

	@Test public void testBlankObjects() {

		assertEquals("blank object",
				ValuesTest.decode("[] rdf:value [] ."),
				rdf(blanks(object(field("this", "_:x")))));

		assertEquals("empty id blank object",
				ValuesTest.decode("[] rdf:value [] ."),
				rdf(blanks(object(field("this", "")))));

		assertEquals("null id blank object",
				ValuesTest.decode("[] rdf:value [] ."),
				rdf(blanks(object(field("this", null)))));

		assertTrue("preserve bnode id", rdf(blanks(object(field("this", "_:x")))).stream()
				.allMatch(statement -> statement.getObject().equals(Values.bnode("x"))));

		assertTrue("preserve bnode id / shorthand",
				rdf(blanks("_:x"), null, trait(RDF.VALUE, datatype(Values.BNodeType))).stream()
						.allMatch(statement -> statement.getObject().equals(Values.bnode("x"))));

	}

	@Test public void testNamedObjects() {

		assertEquals("named objects with naked predicate IRIs",
				ValuesTest.decode("<x> rdf:value <y>. <z> rdf:value <x>."),
				rdf(object(
						field("this", "http://example.com/x"),
						field(value, object(field("this", "http://example.com/y"))),
						field("^"+value, object(field("this", "http://example.com/z")))
				)));

		assertEquals("named objects with bracketed predicate IRIs",
				ValuesTest.decode("<x> rdf:value <y>. <z> rdf:value <x>."),
				rdf(object(
						field("this", "http://example.com/x"),
						field("<"+value+">", object(field("this", "http://example.com/y"))),
						field("^<"+value+">", object(field("this", "http://example.com/z")))
				)));

	}

	@Test public void testTypedObjects() {

		assertEquals("null", ValuesTest.decode(""), rdf(blanks((Object)null)));
		assertEquals("boolean", ValuesTest.decode("[] rdf:value true ."), rdf(blanks(true)));
		assertEquals("string", ValuesTest.decode("[] rdf:value 'string' ."), rdf(blanks("string")));
		assertEquals("decimal", ValuesTest.decode("[] rdf:value 1.0 ."), rdf(blanks(new BigDecimal("1.0"))));
		assertEquals("integer", ValuesTest.decode("[] rdf:value 1 ."), rdf(blanks(BigInteger.ONE)));

		// !!! unable to test for exponent presence using JSON-P
		//assertEquals("double", parse("[] rdf:value 1.0E0 ."), // special support for doubles
		//		rdf("[{\"this\": \"_:node1bcl7j42cx10\", \"http://www.w3.org/1999/02/22-rdf-syntax-ns#value\": [1.0e0]}]"));

		assertEquals("numeric",
				ValuesTest.decode("[] rdf:value '1'^^xsd:int ."),
				rdf(blanks(object(field("text", "1"), field("type", XMLSchema.INT.stringValue())))));

		assertEquals("custom",
				ValuesTest.decode("[] rdf:value 'text'^^:type ."),
				rdf(blanks(object(field("text", "text"), field("type", ValuesTest.term("type").stringValue())))));

		assertEquals("tagged",
				ValuesTest.decode("[] rdf:value 'text'@en ."),
				rdf(blanks(object(field("text", "text"), field("lang", "en")))));

	}


	//// Focus /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testParseOnlyFocusNode() {
		assertEquals("focus node only",
				ValuesTest.decode("<x> rdf:value 'x' ."),
				rdf(
						array(
								object(field("this", "http://example.com/x"), field(value, "x")),
								object(field("this", "http://example.com/y"), field(value, "y"))
						),
						Values.iri("http://example.com/x")
				));
	}

	@Test public void testHandleUnknownFocusNode() {
		assertEquals("unknown focus",
				ValuesTest.decode(""),
				rdf(
						object(field("this", "http://example.com/x"), field(value, "x")),
						Values.bnode()
				));
	}

	@Test public void testAssumeFocusAsSubject() {
		assertEquals("focus assumed as subject",
				ValuesTest.decode("<x> rdf:value 'x' ."),
				rdf(
						array(
								object(field(value, "x"))
						),
						Values.iri("http://example.com/x")
				));
	}


	//// Shared References /////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testHandleNamedLoops() {
		assertEquals("named loops",
				ValuesTest.decode("<x> rdf:value <y>. <y> rdf:value <x>."),
				rdf(object(
						field("this", "http://example.com/x"),
						field(value, object(
								field("this", "http://example.com/y"),
								field(value, array(object(field("this", "http://example.com/x"))))
								)
						)
				)));
	}

	@Test public void testHandleBlankLoops() {
		assertEquals("named loops",
				ValuesTest.decode("_:x rdf:value [rdf:value _:x] ."),
				rdf(object(
						field("this", "_:a"),
						field(value, blank(field(value, object(field("this", "_:a")))))
				)));
	}


	//// Aliases ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testParseAliasedTraits() {

		assertEquals("direct inferred",
				ValuesTest.decode("[] rdf:value [] ."),
				rdf(
						blank(field("value", blank())),
						null,
						trait(RDF.VALUE)
				));

		assertEquals("inverse inferred",
				ValuesTest.decode("[] rdf:value [] ."),
				rdf(
						blank(field("valueOf", blank())),
						null,
						trait(Step.step(RDF.VALUE, true))
				));

		assertEquals("user-defined",
				ValuesTest.decode("[] rdf:value [] ."),
				rdf(
						blank(field("alias", blank())),
						null,
						trait(RDF.VALUE, Alias.alias("alias"))
				));

	}

	@Test public void testParseAliasedNestedTraits() {

		assertEquals("aliased nested trait",
				ValuesTest.decode("[] rdf:value [rdf:value []] ."),
				rdf(
						blank(field("value", blank(field("alias", blank())))),
						null,
						trait(RDF.VALUE, trait(Step.step(RDF.VALUE), Alias.alias("alias")))
				));

	}

	@Test(expected=RDFParseException.class) public void testHandleAliasClashes() {
		rdf(
				object(field("value", object())),
				null,
				and(
						trait(RDF.VALUE),
						trait(ValuesTest.term("value"))
				)
		);
	}

	@Test(expected=RDFParseException.class) public void testResolveAliasesOnlyIfShapeIsSet() {
		rdf(
				object(field("this", "_:x"), field("value", object())),
				null,
				null
		);
	}


	//// Focus-Relative Paths //////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testResolveRelativeIRIs() {

		assertEquals("base relative subject",
				ValuesTest.decode("<x> rdf:value <y> ."),
				rdf(
						object(field("this", "x"), field(value, "http://example.com/y")),
						null,
						trait(RDF.VALUE, datatype(Values.IRIType))
				));

		assertEquals("root-relative subject",
				ValuesTest.decode("<x> rdf:value <y>."),
				rdf(
						object(field("this", "/x"), field(value, "http://example.com/y")),
						null,
						trait(RDF.VALUE, datatype(Values.IRIType))
				));

		assertEquals("base relative object",
				ValuesTest.decode("<x> rdf:value <y> ."),
				rdf(
						object(field("this", "http://example.com/x"), field(value, "y")),
						null,
						trait(RDF.VALUE, datatype(Values.IRIType))
				));

		assertEquals("root-relative object",
				ValuesTest.decode("<x> rdf:value <http://example.com/z>."),
				rdf(
						object(field("this", "http://example.com/x"), field(value, "/z")),
						null,
						trait(RDF.VALUE, datatype(Values.IRIType))
				));

	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testParseNamedReverseLinks() {
		assertEquals("named reverse links",
				ValuesTest.decode("<y> rdf:value <x> ."),
				rdf(
						object(
								field("this", "http://example.com/x"),
								field("valueOf", object(
										field("this", "http://example.com/y")
								))
						),
						null,
						trait(Step.step(RDF.VALUE, true))
				));
	}

	@Test public void testParseBlankReverseLinks() {
		assertEquals("blank reverse links",
				ValuesTest.decode("[] rdf:value [] ."),
				rdf(
						blank(field("valueOf", blank())),
						null,
						trait(Step.step(RDF.VALUE, true))
				));
	}

	@Test public void testParseInlinedProvedTypedLiterals() {
		assertEquals("simplified literal with known datatype",
				ValuesTest.decode("[] rdf:value '2016-08-11'^^xsd:date."),
				rdf(
						blank(field("value", "2016-08-11")),
						null,
						trait(RDF.VALUE, datatype(XMLSchema.DATE))
				));
	}

	@Test public void testParseThisLessProvedBlanks() {
		assertEquals("proved blanks",
				ValuesTest.decode("[] rdf:value [] ."),
				rdf(
						object(field("value", object())),
						null,
						and(datatype(Values.BNodeType), trait(RDF.VALUE, datatype(Values.BNodeType)))
				));
	}

	@Test public void testParseThisLessProvedNameds() {
		assertEquals("proved named",
				ValuesTest.decode("<x> rdf:value [] ."),
				rdf(
						object(field("value", blank())),
						null,
						and(All.all(Values.iri("http://example.com/x")), trait(RDF.VALUE))
				));
	}

	@Test public void testParseIDOnlyProvedBlanks() {
		assertEquals("proved blank",
				ValuesTest.decode("[] rdf:value [] ."),
				rdf(
						blanks("_:x"),
						null,
						trait(RDF.VALUE, datatype(Values.BNodeType))
				));
	}

	@Test public void testParseIRIOnlyProvedNameds() {
		assertEquals("proved named",
				ValuesTest.decode("[] rdf:value <x> ."),
				rdf(
						blanks("http://example.com/x"),
						null,
						trait(RDF.VALUE, datatype(Values.IRIType))
				));
	}

	@Test public void testParseStringOnlyProvedResources() {
		assertEquals("proved resources",
				ValuesTest.decode("[] rdf:value [], <x> ."),
				rdf(
						blanks("_:x", "http://example.com/x"),
						null,
						trait(RDF.VALUE, datatype(Values.ResoureType))
				));
	}

	@Test public void testParseProvedDecimalsLeniently() {
		assertEquals("proved decimal",
				ValuesTest.decode("[] rdf:value 1.0 ."),
				rdf(
						blanks(Values.decimal(1), Values.integer(1), 1.0),
						null,
						trait(RDF.VALUE, datatype(XMLSchema.DECIMAL))
				));
	}

	@Test public void testParseProvedDoublesLeniently() {
		assertEquals("proved decimal",
				ValuesTest.decode("[] rdf:value 1.0E0 ."),
				rdf(
						blanks(1.0, Values.integer(1), Values.decimal(1)),
						null,
						trait(RDF.VALUE, datatype(XMLSchema.DOUBLE))
				));
	}

	@Test(expected=RDFParseException.class) public void testRejectMalformedLiterals() {
		rdf(blanks("22/5/2018"), null, trait(RDF.VALUE, datatype(XMLSchema.DATETIME)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Model rdf(final Object json) {
		return rdf(json, null);
	}

	private Model rdf(final Object json, final Resource focus) {
		return rdf(json, focus, null);
	}

	private Model rdf(final Object json, final Resource focus, final Shape shape) {
		try (final StringReader reader=new StringReader((json instanceof String ? json : json(json)).toString())) {

			final StatementCollector collector=new StatementCollector();

			final RDFParser parser=new JSONParser();

			parser.set(JSONAdapter.Focus, focus);
			parser.set(JSONAdapter.Shape, shape);

			parser.set(BasicParserSettings.PRESERVE_BNODE_IDS, true);

			parser.set(BasicParserSettings.VERIFY_DATATYPE_VALUES, true);
			parser.set(BasicParserSettings.NORMALIZE_DATATYPE_VALUES, true);

			parser.set(BasicParserSettings.VERIFY_LANGUAGE_TAGS, true);
			parser.set(BasicParserSettings.NORMALIZE_LANGUAGE_TAGS, true);

			parser.setRDFHandler(collector);

			parser.parse(reader, ValuesTest.Base);

			return new LinkedHashModel(collector.getStatements());

		} catch ( final IOException e ) {
			throw new UncheckedIOException(e);
		}
	}

}
