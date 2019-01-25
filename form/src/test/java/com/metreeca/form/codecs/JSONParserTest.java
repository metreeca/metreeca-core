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

import com.metreeca.form.Shape;
import com.metreeca.form.Shift;
import com.metreeca.form.shapes.All;
import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
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

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Meta.alias;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.Shift.shift;
import static com.metreeca.form.things.ValuesTest.decode;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;


final class JSONParserTest extends JSONCodecTest {

	@Test void testReportRDFParseException() {
		assertThatExceptionOfType(RDFParseException.class).isThrownBy(() ->
				new JSONParser().parse(new StringReader(""), "")
		);
	}


	//// Objects ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testNoObjects() {
		assertThat((Object)decode("")).as("empty object").isEqualTo(rdf(object()));
		assertThat((Object)decode("")).as("empty array").isEqualTo(rdf(array()));
	}

	@Test void testBlankObjects() {

		assertThat((Object)decode("[] rdf:value [] .")).as("blank object").isEqualTo(rdf(blanks(object(field("this", "_:x")))));

		assertThat((Object)decode("[] rdf:value [] .")).as("empty id blank object").isEqualTo(rdf(blanks(object(field("this", "")))));

		assertThat((Object)decode("[] rdf:value [] .")).as("null id blank object").isEqualTo(rdf(blanks(object(field("this", null)))));

		assertThat(rdf(blanks(object(field("this", "_:x")))).stream()
				.allMatch(statement1 -> statement1.getObject().equals(Values.bnode("x")))).as("preserve bnode id").isTrue();

		assertThat(rdf(blanks("_:x"), null, trait(RDF.VALUE, datatype(Values.BNodeType))).stream()
				.allMatch(statement -> statement.getObject().equals(Values.bnode("x")))).as("preserve bnode id / shorthand").isTrue();

	}

	@Test void testNamedObjects() {

		assertThat((Object)decode("<x> rdf:value <y>. <z> rdf:value <x>.")).as("named objects with naked predicate IRIs").isEqualTo(rdf(object(
				field("this", "http://example.com/x"),
				field(value, object(field("this", "http://example.com/y"))),
				field("^"+value, object(field("this", "http://example.com/z")))
		)));

		assertThat((Object)decode("<x> rdf:value <y>. <z> rdf:value <x>.")).as("named objects with bracketed predicate IRIs").isEqualTo(rdf(object(
				field("this", "http://example.com/x"),
				field("<"+value+">", object(field("this", "http://example.com/y"))),
				field("^<"+value+">", object(field("this", "http://example.com/z")))
		)));

	}

	@Test void testTypedObjects() {

		assertThat((Object)decode("")).as("null").isEqualTo(rdf(blanks((Object)null)));
		assertThat((Object)decode("[] rdf:value true .")).as("boolean").isEqualTo(rdf(blanks(true)));
		assertThat((Object)decode("[] rdf:value 'string' .")).as("string").isEqualTo(rdf(blanks("string")));
		assertThat((Object)decode("[] rdf:value 1.0 .")).as("decimal").isEqualTo(rdf(blanks(new BigDecimal("1.0"))));
		assertThat((Object)decode("[] rdf:value 1 .")).as("integer").isEqualTo(rdf(blanks(BigInteger.ONE)));

		// !!! unable to test for exponent presence using JSON-P
		//assertEquals("double", parse("[] rdf:value 1.0E0 ."), // special support for doubles
		//		rdf("[{\"this\": \"_:node1bcl7j42cx10\", \"http://www.w3.org/1999/02/22-rdf-syntax-ns#value\": [1.0e0]}]"));

		assertThat((Object)decode("[] rdf:value '1'^^xsd:int .")).as("numeric").isEqualTo(rdf(blanks(object(field("text", "1"), field("type", XMLSchema.INT.stringValue())))));

		assertThat((Object)decode("[] rdf:value 'text'^^:type .")).as("custom").isEqualTo(rdf(blanks(object(field("text", "text"), field("type", ValuesTest.term("type").stringValue())))));

		assertThat((Object)decode("[] rdf:value 'text'@en .")).as("tagged").isEqualTo(rdf(blanks(object(field("text", "text"), field("lang", "en")))));

	}


	//// Focus /////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseOnlyFocusNode() {
		assertThat((Object)decode("<x> rdf:value 'x' .")).as("focus node only").isEqualTo(rdf(
				array(
						object(field("this", "http://example.com/x"), field(value, "x")),
						object(field("this", "http://example.com/y"), field(value, "y"))
				),
				Values.iri("http://example.com/x")
		));
	}

	@Test void testHandleUnknownFocusNode() {
		assertThat((Object)decode("")).as("unknown focus").isEqualTo(rdf(
				object(field("this", "http://example.com/x"), field(value, "x")),
				Values.bnode()
		));
	}

	@Test void testAssumeFocusAsSubject() {
		assertThat((Object)decode("<x> rdf:value 'x' .")).as("focus assumed as subject").isEqualTo(rdf(
				array(
						object(field(value, "x"))
				),
				Values.iri("http://example.com/x")
		));
	}


	//// Shared References /////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testHandleNamedLoops() {
		assertThat((Object)decode("<x> rdf:value <y>. <y> rdf:value <x>.")).as("named loops").isEqualTo(rdf(object(
				field("this", "http://example.com/x"),
				field(value, object(
						field("this", "http://example.com/y"),
						field(value, array(object(field("this", "http://example.com/x"))))
						)
				)
		)));
	}

	@Test void testHandleBlankLoops() {
		assertThat((Object)decode("_:x rdf:value [rdf:value _:x] .")).as("named loops").isEqualTo(rdf(object(
				field("this", "_:a"),
				field(value, blank(field(value, object(field("this", "_:a")))))
		)));
	}


	//// Aliases ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseAliasedTraits() {

		assertThat((Object)decode("[] rdf:value [] .")).as("direct inferred").isEqualTo(rdf(
				blank(field("value", blank())),
				null,
				trait(RDF.VALUE)
		));

		assertThat((Object)decode("[] rdf:value [] .")).as("inverse inferred").isEqualTo(rdf(
				blank(field("valueOf", blank())),
				null,
				trait(shift(RDF.VALUE).inverse())
		));

		assertThat((Object)decode("[] rdf:value [] .")).as("user-defined").isEqualTo(rdf(
				blank(field("alias", blank())),
				null,
				trait(RDF.VALUE, alias("alias"))
		));

	}

	@Test void testParseAliasedNestedTraits() {

		assertThat(rdf(
				blank(field("value", blank(field("alias", blank())))),
				null,
				trait(RDF.VALUE, trait(Shift.shift(RDF.VALUE), alias("alias")))
		))
				.as("aliased nested trait")
				.isEqualTo(decode("[] rdf:value [rdf:value []] ."));

	}

	@Test void testHandleAliasClashes() {
		assertThatExceptionOfType(RDFParseException.class).isThrownBy(() -> rdf(
				object(field("value", object())),
				null,
				and(
						trait(RDF.VALUE),
						trait(ValuesTest.term("value"))
				)
		));
	}

	@Test void testResolveAliasesOnlyIfShapeIsSet() {
		assertThatExceptionOfType(RDFParseException.class).isThrownBy(() -> rdf(
				object(field("this", "_:x"), field("value", object())),
				null,
				null
		));
	}


	//// Focus-Relative Paths //////////////////////////////////////////////////////////////////////////////////////////

	@Test void testResolveRelativeIRIs() {

		assertThat((Object)decode("<x> rdf:value <y> .")).as("base relative subject").isEqualTo(rdf(
				object(field("this", "x"), field(value, "http://example.com/y")),
				null,
				trait(RDF.VALUE, datatype(Values.IRIType))
		));

		assertThat((Object)decode("<x> rdf:value <y>.")).as("root-relative subject").isEqualTo(rdf(
				object(field("this", "/x"), field(value, "http://example.com/y")),
				null,
				trait(RDF.VALUE, datatype(Values.IRIType))
		));

		assertThat((Object)decode("<x> rdf:value <y> .")).as("base relative object").isEqualTo(rdf(
				object(field("this", "http://example.com/x"), field(value, "y")),
				null,
				trait(RDF.VALUE, datatype(Values.IRIType))
		));

		assertThat((Object)decode("<x> rdf:value <http://example.com/z>.")).as("root-relative object").isEqualTo(rdf(
				object(field("this", "http://example.com/x"), field(value, "/z")),
				null,
				trait(RDF.VALUE, datatype(Values.IRIType))
		));

	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseNamedReverseLinks() {
		assertThat((Object)decode("<y> rdf:value <x> .")).as("named reverse links").isEqualTo(rdf(
				object(
						field("this", "http://example.com/x"),
						field("valueOf", object(
								field("this", "http://example.com/y")
						))
				),
				null,
				trait(shift(RDF.VALUE).inverse())
		));
	}

	@Test void testParseBlankReverseLinks() {
		assertThat((Object)decode("[] rdf:value [] .")).as("blank reverse links").isEqualTo(rdf(
				blank(field("valueOf", blank())),
				null,
				trait(shift(RDF.VALUE).inverse())
		));
	}

	@Test void testParseInlinedProvedTypedLiterals() {
		assertThat((Object)decode("[] rdf:value '2016-08-11'^^xsd:date.")).as("simplified literal with known datatype").isEqualTo(rdf(
				blank(field("value", "2016-08-11")),
				null,
				trait(RDF.VALUE, datatype(XMLSchema.DATE))
		));
	}

	@Test void testParseThisLessProvedBlanks() {
		assertThat((Object)decode("[] rdf:value [] .")).as("proved blanks").isEqualTo(rdf(
				object(field("value", object())),
				null,
				and(datatype(Values.BNodeType), trait(RDF.VALUE, datatype(Values.BNodeType)))
		));
	}

	@Test void testParseThisLessProvedNameds() {
		assertThat((Object)decode("<x> rdf:value [] .")).as("proved named").isEqualTo(rdf(
				object(field("value", blank())),
				null,
				and(All.all(Values.iri("http://example.com/x")), trait(RDF.VALUE))
		));
	}

	@Test void testParseIDOnlyProvedBlanks() {
		assertThat((Object)decode("[] rdf:value [] .")).as("proved blank").isEqualTo(rdf(
				blanks("_:x"),
				null,
				trait(RDF.VALUE, datatype(Values.BNodeType))
		));
	}

	@Test void testParseIRIOnlyProvedNameds() {
		assertThat((Object)decode("[] rdf:value <x> .")).as("proved named").isEqualTo(rdf(
				blanks("http://example.com/x"),
				null,
				trait(RDF.VALUE, datatype(Values.IRIType))
		));
	}

	@Test void testParseStringOnlyProvedResources() {
		assertThat((Object)decode("[] rdf:value [], <x> .")).as("proved resources").isEqualTo(rdf(
				blanks("_:x", "http://example.com/x"),
				null,
				trait(RDF.VALUE, datatype(Values.ResourceType))
		));
	}

	@Test void testParseProvedDecimalsLeniently() {
		assertThat((Object)decode("[] rdf:value 1.0 .")).as("proved decimal").isEqualTo(rdf(
				blanks(Values.decimal(1), Values.integer(1), 1.0),
				null,
				trait(RDF.VALUE, datatype(XMLSchema.DECIMAL))
		));
	}

	@Test void testParseProvedDoublesLeniently() {
		assertThat((Object)decode("[] rdf:value 1.0E0 .")).as("proved decimal").isEqualTo(rdf(
				blanks(1.0, Values.integer(1), Values.decimal(1)),
				null,
				trait(RDF.VALUE, datatype(XMLSchema.DOUBLE))
		));
	}

	@Test void testRejectMalformedLiterals() {
		assertThatExceptionOfType(RDFParseException.class).isThrownBy(() ->
				rdf(blanks("22/5/2018"), null, trait(RDF.VALUE, datatype(XMLSchema.DATETIME)))
		);
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

			parser.set(JSONCodec.Focus, focus);
			parser.set(JSONCodec.Shape, shape);

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
