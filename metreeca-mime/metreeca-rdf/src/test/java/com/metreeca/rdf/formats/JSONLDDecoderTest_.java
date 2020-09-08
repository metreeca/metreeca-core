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

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonException;
import java.io.StringReader;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.stream.Stream;

import static com.metreeca.core.EitherAssert.assertThat;
import static com.metreeca.json.Shape.required;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.rdf.ModelAssert.assertThat;
import static com.metreeca.rdf.Values.*;
import static com.metreeca.rdf.ValuesTest.*;
import static com.metreeca.rdf.formats.JSONLDCodecTest.*;
import static com.metreeca.rdf.formats.JSONLDFormat.jsonld;
import static java.util.stream.Collectors.toMap;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class JSONLDDecoderTest_ { // !!! merge into JSONLDDecoderTest

	private final String first=RDF.FIRST.stringValue();


	@SafeVarargs private final Map<String, Object> blank(final Map.Entry<String, Object>... fields) {
		return Stream.concat(
				Stream.of(entry("id", format(bnode()))),
				Stream.of(fields)
		).collect(toMap(Map.Entry::getKey, Map.Entry::getValue));
	}


	private Model rdf(final Object json) {
		return rdf(json, null);
	}

	private Model rdf(final Object json, final Resource focus) {
		return rdf(json, focus, null);
	}

	private Model rdf(final Object json, final Resource focus, final Shape shape) {
		return rdf(json, focus, shape, Base);
	}

	private Model rdf(final Object json, final Resource focus, final Shape shape, final String base) {
		try ( final StringReader reader=new StringReader((json instanceof String ? json : json(json)).toString()) ) {

			return new LinkedHashModel(new JSONLDDecoder(base, Context) {}.decode(focus, shape,
					Json.createReader(reader).read()));

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testReportParseException() {
		assertThat(jsonld(new StringReader(""), "", RDF.NIL, and(), Context))
				.hasLeft(e -> assertThat(e).isInstanceOf(JsonException.class));
	}


	//// Objects
	// /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testNoObjects() {

		assertThat(rdf(map(), RDF.NIL))
				.as("empty object")
				.isEqualTo(decode(""));

		assertThat(rdf(array(), RDF.NIL))
				.as("empty array")
				.isEqualTo(decode(""));

	}

	@Test void testBlankObjects() {

		assertThat(rdf(map(entry("id", "_:x"), entry(first, map(entry("id", "_:y"))))))
				.as("blank object")
				.isEqualTo(decode("[] rdf:first [] ."));

		assertThat(rdf(map(entry("id", ""), entry(first, map(entry("id", ""))))))
				.as("empty id blank object")
				.isEqualTo(decode("[] rdf:first [] ."));

		Assertions.assertThat(rdf(map(entry("id", "_:x"), entry(first, map(entry("id", "_:x"))))))
				.as("preserve bnode id")
				.allMatch(statement -> statement.getObject().equals(bnode("x")));

		Assertions.assertThat(rdf(map(entry("id", "_:x"), entry(first, "_:x")), null, field(RDF.FIRST,
				datatype(BNodeType))))
				.as("preserve bnode id / shorthand")
				.allMatch(statement -> statement.getObject().equals(bnode("x")));

	}

	@Test void testNamedObjects() {

		assertThat(rdf(map(
				entry("id", "http://example.com/x"),
				entry(first, map(entry("id", "http://example.com/y"))),
				entry("^"+first, map(entry("id", "http://example.com/z")))
		)))
				.as("named objects with naked predicate IRIs")
				.isEqualTo(decode("<x> rdf:first <y>. <z> rdf:first <x>."));

		assertThat(rdf(map(
				entry("id", "http://example.com/x"),
				entry("<"+first+">", map(entry("id", "http://example.com/y"))),
				entry("^<"+first+">", map(entry("id", "http://example.com/z")))
		)))
				.as("named objects with bracketed predicate IRIs")
				.isEqualTo(decode("<x> rdf:first <y>. <z> rdf:first <x>."));

	}

	@Test void testTypedObjects() {

		assertThat(rdf(map(entry("id", ""), entry(first, true))))
				.as("boolean")
				.isEqualTo(decode("[] rdf:first true ."));

		assertThat(rdf(map(entry("id", ""), entry(first, "string"))))
				.as("string")
				.isEqualTo(decode("[] rdf:first 'string' ."));

		assertThat(rdf(map(entry("id", ""), entry(first, new BigDecimal("1.0")))))
				.as("decimal")
				.isEqualTo(decode("[] rdf:first 1.0 ."));

		assertThat(rdf(map(entry("id", ""), entry(first, BigInteger.ONE))))
				.as("integer")
				.isEqualTo(decode("[] rdf:first 1 ."));

		assertThat(rdf(map(entry("id", ""), entry(first, map(entry("value", "1"), entry("type",
				XSD.INT.stringValue()))))))
				.as("numeric")
				.isEqualTo(decode("[] rdf:first '1'^^xsd:int ."));

		assertThat(rdf(map(entry("id", ""), entry(first, map(entry("value", "text"), entry("type",
				term("type").stringValue()))))))
				.as("custom")
				.isEqualTo(decode("[] rdf:first 'text'^^:type ."));

		assertThat(rdf(map(entry("id", ""), entry(first, map(entry("value", "text"), entry("language", "en"))))))
				.as("tagged")
				.isEqualTo(decode("[] rdf:first 'text'@en ."));

	}


	//// Focus
	// ///////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testAssumeFocusAsSubject() {
		assertThat(rdf(
				list(map(entry(first, "x"))),
				iri("http://example.com/x")
		))
				.as("focus assumed as subject")
				.isEqualTo(decode("<x> rdf:first 'x' ."));
	}

	@Test void testParseOnlyFocusNode() {
		assertThat(rdf(
				list(
						map(entry("id", "http://example.com/x"), entry(first, "x")),
						map(entry("id", "http://example.com/y"), entry(first, "y"))
				),
				iri("http://example.com/x")
		))
				.as("focus node only")
				.isEqualTo(decode("<x> rdf:first 'x' ."));
	}

	@Test void tesIgnoreUnknownFocusNode() {
		assertThat(rdf(
				map(entry("id", "http://example.com/x"), entry(first, "x")),
				bnode()
		))
				.as("unknown focus")
				.isEqualTo(decode(""));
	}


	//// Shared References
	// ///////////////////////////////////////////////////////////////////////////////////////////

	@Test void testHandleNamedLoops() {
		assertThat(rdf(map(
				entry("id", "http://example.com/x"),
				entry(first, map(
						entry("id", "http://example.com/y"),
						entry(first, list(map(entry("id", "http://example.com/x"))))
				))
		)))
				.as("named loops")
				.isEqualTo(decode("<x> rdf:first <y>. <y> rdf:first <x>."));
	}

	@Test void testHandleBlankLoops() {
		assertThat(rdf(map(
				entry("id", "_:a"),
				entry(first, blank(entry(first, map(entry("id", "_:a")))))
		)))
				.as("named loops")
				.isEqualTo(decode("_:x rdf:first [rdf:first _:x] ."));
	}

	@Test void testInlineProvedIRIBackReferences() {
		assertThat(rdf(

				map(
						entry("id", "http://example.com/x"),
						entry(first, "http://example.com/x")
				),

				null,

				field(RDF.FIRST, and(required(), datatype(IRIType)))

		))
				.as("inlined IRI back-reference")
				.isIsomorphicTo(decode("<x> rdf:first <x>."));
	}

	@Test void testInlineProvedBnodeBackReferences() {
		assertThat(rdf(

				map(
						entry("id", "_:x"),
						entry(first, "_:x")
				),

				null,

				field(RDF.FIRST, and(required(), datatype(BNodeType)))

		))
				.as("inlined IRI back-reference")
				.isIsomorphicTo(decode("_:x rdf:first _:x ."));
	}


	//// Aliases
	// /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseAliasedFields() {

		assertThat(rdf(
				blank(entry("first", blank())),
				null,
				field(RDF.FIRST)
		))
				.as("direct inferred")
				.isEqualTo(decode("[] rdf:first [] ."));

		assertThat(rdf(
				blank(entry("firstOf", blank())),
				null,
				field(inverse(RDF.FIRST))
		))
				.as("inverse inferred")
				.isEqualTo(decode("[] rdf:first [] ."));

		assertThat(rdf(
				blank(entry("alias", blank())),
				null,
				field(RDF.FIRST, alias("alias"))
		))
				.as("user-defined")
				.isEqualTo(decode("[] rdf:first [] ."));

	}

	@Test void testParseAliasedNestedFields() {

		Assertions.assertThat(rdf(
				blank(entry("first", blank(entry("alias", blank())))),
				null,
				field(RDF.FIRST, field(RDF.FIRST, alias("alias")))
		))
				.as("aliased nested field")
				.isEqualTo(decode("[] rdf:first [rdf:first []] ."));

	}

	@Test void testHandleAliasClashes() {
		assertThatExceptionOfType(JsonException.class).isThrownBy(() -> rdf(
				map(entry("first", map())),
				null,
				and(
						field(RDF.FIRST),
						field(term("first"))
				)
		));
	}

	@Test void testResolveAliasesOnlyIfShapeIsSet() {
		assertThatExceptionOfType(JsonException.class).isThrownBy(() -> rdf(
				map(entry("id", "_:x"), entry("first", map())),
				null,
				null
		));
	}


	//// Focus-Relative Paths
	// ////////////////////////////////////////////////////////////////////////////////////////

	@Test void testResolveRelativeIRIs() {

		final BiFunction<String, String, Collection<Statement>> statament=(s, o) -> rdf(
				map(entry("id", s), entry(this.first, o)),
				null,
				field(RDF.FIRST, datatype(IRIType)),
				Base+"relative/"
		);

		assertThat(statament.apply("x", "http://example.com/y"))
				.as("base relative subject")
				.isEqualTo(decode("<http://example.com/relative/x> rdf:first <y> ."));

		assertThat(statament.apply("/x", "http://example.com/y"))
				.as("root relative subject")
				.isEqualTo(decode("<http://example.com/x> rdf:first <y>."));

		assertThat(statament.apply("http://example.com/absolute/x", "http://example.com/y"))
				.as("absolute subject")
				.isEqualTo(decode("<http://example.com/absolute/x> rdf:first <y>."));

		assertThat(statament.apply("http://example.com/x", "y"))
				.as("base relative object")
				.isEqualTo(decode("<x> rdf:first <http://example.com/relative/y> ."));

		assertThat(statament.apply("http://example.com/x", "/y"))
				.as("root relative object")
				.isEqualTo(decode("<x> rdf:first <http://example.com/y>."));

		assertThat(statament.apply("http://example.com/x", "http://example.com/absolute/y"))
				.as("absolute object")
				.isEqualTo(decode("<x> rdf:first <http://example.com/absolute/y>."));

	}


	//// Shapes
	// //////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testParseNamedReverseLinks() {
		assertThat(rdf(
				map(
						entry("id", "http://example.com/x"),
						entry("firstOf", map(
								entry("id", "http://example.com/y")
						))
				),
				null,
				field(inverse(RDF.FIRST))
		))
				.as("named reverse links")
				.isEqualTo(decode("<y> rdf:first <x> ."));
	}

	@Test void testParseBlankReverseLinks() {
		assertThat(rdf(
				map(entry("id", ""), entry("firstOf", blank())),
				null,
				field(inverse(RDF.FIRST))
		))
				.as("blank reverse links")
				.isEqualTo(decode("[] rdf:first [] ."));
	}

	@Test void testParseInlinedProvedTypedLiterals() {
		assertThat(rdf(
				map(entry("id", ""), entry("first", "2016-08-11")),
				null,
				field(RDF.FIRST, datatype(XSD.DATE))
		))
				.as("simplified literal with known datatype")
				.isEqualTo(decode("[] rdf:first '2016-08-11'^^xsd:date."));
	}

	@Test void testParseThisLessProvedBlanks() {
		assertThat(rdf(
				map(entry("first", map())),
				null,
				and(datatype(BNodeType), field(RDF.FIRST, datatype(BNodeType)))
		))
				.as("proved blanks")
				.isEqualTo(decode("[] rdf:first [] ."));
	}

	//@Test void testParseThisLessProvedIRIs() {
	//	assertThat(rdf(
	//			map(entry("first", blank())),
	//			null,
	//			and(all(iri("http://example.com/x")), field(RDF.FIRST))
	//	))
	//			.as("proved named")
	//			.isEqualTo(decode("<x> rdf:first [] ."));
	//}

	@Test void testParseIDOnlyProvedBlanks() {
		assertThat(rdf(
				map(entry("id", ""), entry(first, "_:x")),
				null,
				field(RDF.FIRST, datatype(BNodeType))
		))
				.as("proved blank")
				.isEqualTo(decode("[] rdf:first [] ."));
	}

	@Test void testParseIRIOnlyProvedIRIs() {
		assertThat(rdf(
				map(entry("id", ""), entry(first, "http://example.com/x")),
				null,
				field(RDF.FIRST, datatype(IRIType))
		))
				.as("proved named")
				.isEqualTo(decode("[] rdf:first <x> ."));
	}

	@Test void testParseStringOnlyProvedResources() {
		assertThat(rdf(
				map(entry("id", ""), entry(first, list("_:x", "http://example.com/x"))),
				null,
				field(RDF.FIRST, datatype(ResourceType))
		))
				.as("proved resources")
				.isEqualTo(decode("[] rdf:first [], <x> ."));
	}

	@Test void testParseProvedDecimalsLeniently() {

		assertThat(rdf(
				map(entry("id", ""), entry(first, list(1.0, integer(1), decimal(1)))),
				null,
				field(RDF.FIRST, datatype(XSD.DECIMAL))
		))
				.as("proved decimal")
				.isEqualTo(decode("[] rdf:first 1.0, '1'^^xsd:decimal ."));

	}

	@Test void testParseProvedDoublesLeniently() {

		assertThat(rdf(
				map(entry("id", ""), entry(first, list(1.0, integer(1), decimal(1)))),
				null,
				field(RDF.FIRST, datatype(XSD.DOUBLE))
		))
				.as("proved decimal")
				.isEqualTo(decode("[] rdf:first '1.0'^^xsd:double ."));

	}

	@Test void testRejectMalformedLiterals() {

		assertThatThrownBy(() -> rdf(map(entry("id", ""), entry(first, map(

				entry("value", "22/5/2018"),
				entry("type", XSD.DATETIME.stringValue())

		))))).isInstanceOf(JsonException.class);

	}

}
