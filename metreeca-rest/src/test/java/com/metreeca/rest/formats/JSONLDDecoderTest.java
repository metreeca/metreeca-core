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

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;
import com.metreeca.rest.Xtream;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.*;
import java.math.BigDecimal;
import java.util.Collection;
import java.util.Map;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Shape.optional;
import static com.metreeca.json.Shape.required;
import static com.metreeca.json.ValueAssert.assertThat;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Lang.lang;
import static com.metreeca.json.shapes.Localized.localized;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.json.shapes.Meta.meta;
import static java.util.Collections.emptyMap;
import static javax.json.Json.*;
import static javax.json.JsonValue.EMPTY_JSON_OBJECT;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

final class JSONLDDecoderTest {

	private static final String base="http://example.com/";

	private final BNode a=bnode();
	private final BNode b=bnode();

	private final IRI w=iri(base, "w");
	private final IRI x=iri(base, "x");
	private final IRI y=iri(base, "y");
	private final IRI z=iri(base, "z");


	private Collection<Statement> decode(
			final IRI focus, final Shape shape, final JsonObjectBuilder object
	) {
		return decode(focus, shape, emptyMap(), object);
	}

	private Collection<Statement> decode(
			final IRI focus, final Shape shape, final Map<String, String> keywords, final JsonObjectBuilder object
	) {
		return new JSONLDDecoder(focus, shape, keywords).decode(object.build());
	}


	@Nested final class Syntax {

		@Test void testReportMalformedKeywords() {
			assertThatThrownBy(() -> decode(x, and(), createObjectBuilder()

					.add("@id", createValue(1))

			)).isInstanceOf(JsonException.class);
		}

		@Test void testReportConflictingKeywords() {
			assertThatThrownBy(() -> decode(x, meta("@id", "id"), createObjectBuilder()

					.add("id", "/x")
					.add("@id", "/y")

			)).isInstanceOf(JsonException.class);
		}

		@Test void testIgnoreNullFields() {
			assertThat(decode(x, field(RDF.VALUE, and()), createObjectBuilder()

					.addNull("value")

			)).isIsomorphicTo();
		}

		@Test void testHandleArrays() {
			assertThat(decode(x, field(RDF.VALUE, and()), createObjectBuilder()

					.add("value", createArrayBuilder()

							.add("x")
							.add("y")
					)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, literal("x")),
					statement(x, RDF.VALUE, literal("y"))
			);
		}

	}

	@Nested final class Values {

		private Value decode(final JsonObjectBuilder value) {
			return decode(value.build());
		}

		private Value decode(final JsonValue value) {
			return new JSONLDDecoder(iri(base), field(RDF.VALUE, optional()), emptyMap())

					.decode(createObjectBuilder().add("value", value).build())

					.stream()
					.filter(s -> s.getPredicate().equals(RDF.VALUE))
					.findFirst()
					.map(Statement::getObject)
					.orElse(null);
		}


		@Test void testBNodeAnonymous() {
			assertThat(decode(EMPTY_JSON_OBJECT)).isInstanceOf(BNode.class);
		}

		@Test void testBNodeLabelled() {
			assertThat(decode(createObjectBuilder().add("@id", "_:x"))).isEqualTo(bnode("x"));
		}

		@Test void testBNodeEmptyLabel() {
			assertThat(decode(createObjectBuilder().add("@id", ""))).isInstanceOf(BNode.class);
		}


		@Test void testIRIRelative() {
			assertThat(decode(createObjectBuilder().add("@id", "x"))).isEqualTo(iri(base, "x"));
		}

		@Test void testIRIAbsolute() {
			assertThat(decode(createObjectBuilder().add("@id", "http://x.net/"))).isEqualTo(iri("http://x.net/"));
		}


		@Test void testBoolean() {
			assertThat(decode(JsonValue.TRUE)).isEqualTo(literal(true));
			assertThat(decode(JsonValue.FALSE)).isEqualTo(literal(false));
		}

		@Test void testString() {
			assertThat(decode(createValue("string"))).isEqualTo(literal("string"));
		}

		@Test void testInteger() {
			assertThat(decode(createValue(1))).isEqualTo(literal(integer(1)));
		}

		@Test void testDecimal() {
			assertThat(decode(createValue(1.0))).isEqualTo(literal(decimal(1.0)));
		}


		@Test void testTypedLiterals() {
			assertThat(decode(createObjectBuilder()

					.add("@value", createValue("value"))
					.add("@type", createValue(iri(base, "type").stringValue()))

			)).isEqualTo(literal("value", iri(base, "type")));
		}

		@Test void testTypedLiteralsWithRelativeDatatype() {
			assertThat(decode(createObjectBuilder()

					.add("@value", createValue("value"))
					.add("@type", createValue("type"))

			)).isEqualTo(literal("value", iri(base, "type")));
		}

		@Test void testTaggedLiterals() {
			assertThat(decode(createObjectBuilder()

					.add("@value", createValue("value"))
					.add("@language", createValue("en"))

			)).isEqualTo(literal("value", "en"));
		}


		@Test void testMalformedLiterals() {
			assertThat(decode(createObjectBuilder()

					.add("@value", createValue("none"))
					.add("@type", createValue(XSD.BOOLEAN.toString()))

			)).isEqualTo(literal("none", XSD.BOOLEAN));
		}

	}

	@Nested final class Focus {

		@Test void testDecodeEmptyObject() {
			assertThat(decode(x,

					and(),

					createObjectBuilder()

			)).isIsomorphicTo();
		}

		@Test void testAssumeFocusAsSubject() {
			assertThat(decode(x,

					field(RDF.VALUE, optional()),

					createObjectBuilder()
							.add("value", createObjectBuilder()
									.add("@id", "/y")
							)

			)).isIsomorphicTo(
					statement(x, RDF.VALUE, y)
			);
		}

		@Test void testReportConflictingFocus() {
			assertThatThrownBy(() -> decode(x,

					and(),

					createObjectBuilder()
							.add("@id", "/z")

			)).isInstanceOf(JsonException.class);
		}

	}

	@Nested final class References {

		@Test void testHandleNamedLoops() {
			assertThat(decode(x,

					field(RDF.VALUE, and(required(),
							field(RDF.VALUE, required())
					)),

					createObjectBuilder()
							.add("value", createObjectBuilder()
									.add("@id", "y")
									.add("value", createObjectBuilder()
											.add("@id", "x")
									)
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, y),
					statement(y, RDF.VALUE, x)

			);
		}

		@Test void testHandleBlankLoops() {
			assertThat(decode(x,

					field(RDF.VALUE, and(required(),
							field(RDF.VALUE,
									field(RDF.VALUE, required())
							)
					)),

					createObjectBuilder()
							.add("value", createObjectBuilder()
									.add("@id", "_:x")
									.add("value", createObjectBuilder()
											.add("value", createObjectBuilder()
													.add("@id", "_:x")
											)
									)
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, a),
					statement(a, RDF.VALUE, b),
					statement(b, RDF.VALUE, a)

			);
		}

	}

	@Nested final class Aliases {

		@Test void testDecodeAbsoluteFieldIRIs() {
			assertThat(decode(x,

					field(RDF.VALUE, required()),

					createObjectBuilder()
							.add("value", createObjectBuilder()
									.add("@id", "y")
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, y)

			);
		}

		@Test void testDecodeDirectInferredAliases() {
			assertThat(decode(x,

					field(RDF.VALUE, and(required())),

					createObjectBuilder()
							.add("value", createObjectBuilder()
									.add("@id", "y")
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, y)

			);
		}

		@Test void testDecodeInverseInferredAliases() {
			assertThat(decode(x,

					field(inverse(RDF.VALUE), and(required())),

					createObjectBuilder()
							.add("valueOf", createObjectBuilder() // !!! valueOf?
									.add("@id", "y")
							)

			)).isIsomorphicTo(

					statement(y, RDF.VALUE, x)

			);
		}

		@Test void testDecodeDirectUserDefinedAliases() {
			assertThat(decode(x,

					field(RDF.VALUE, and(alias("alias"), required())),

					createObjectBuilder()
							.add("alias", createObjectBuilder()
									.add("@id", "y")
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, y)

			);
		}

		@Test void testDecodeInverseUserDefinedAliases() {
			assertThat(decode(x,

					field(inverse(RDF.VALUE), and(alias("alias"), required())),

					createObjectBuilder()
							.add("alias", createObjectBuilder()
									.add("@id", "y")
							)

			)).isIsomorphicTo(

					statement(y, RDF.VALUE, x)

			);
		}


		@Test void testReportCLashingAliases() {
			assertThatThrownBy(() -> decode(x,

					and(
							field(iri("http://example.org/value"), and()),
							field(iri("http://example.net/value"), and())
					),

					createObjectBuilder()

			)).isInstanceOf(IllegalArgumentException.class);
		}


	}

	@Nested final class Shapes {

		@Test void testDecodeProvedResources() {
			assertThat(decode(x,

					field(RDF.VALUE, datatype(ResourceType)),

					createObjectBuilder()

							.add("value", createArrayBuilder()

									.add("x")
									.add("_:x")
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, x),
					statement(x, RDF.VALUE, bnode("_:x"))

			);
		}


		@Test void testDecodedProvedBNodes() {
			assertThat(decode(x,

					field(RDF.VALUE, datatype(BNodeType)),

					createObjectBuilder()

							.add("value", "x")

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, bnode("_:x"))

			);
		}

		@Test void testDecodedProvedIRIs() {
			assertThat(decode(x,

					field(RDF.VALUE, datatype(IRIType)),

					createObjectBuilder()

							.add("value", "x")

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, x)

			);
		}


		@Test void testDecodeProvedBNodeBackReferences() {
			assertThat(decode(x,

					field(RDF.VALUE, and(required(),
							field(RDF.VALUE, and(required(), datatype(BNodeType)))
					)),

					createObjectBuilder()
							.add("value", createObjectBuilder()
									.add("@id", "_:x")
									.add("value", "_:x")
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, b),
					statement(b, RDF.VALUE, b)

			);
		}

		@Test void testDecodeProvedIRIBackReferences() {
			assertThat(decode(x,

					field(RDF.VALUE, and(required(), datatype(IRIType))),

					createObjectBuilder()
							.add("value", "x")

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, x)

			);
		}


		@Test void testDecodeProvedTypedLiterals() {
			assertThat(decode(x,

					field(RDF.VALUE, and(required(), datatype(XSD.DATE))),

					createObjectBuilder()

							.add("value", "2016-08-11")

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, literal("2016-08-11", XSD.DATE))

			);
		}

		@Test void testDecodeProvedDecimalsLeniently() {
			assertThat(decode(x,

					field(RDF.VALUE, and(required(), datatype(XSD.DECIMAL))),

					createObjectBuilder()

							.add("value", 1)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, literal(new BigDecimal("1")))

			);
		}

		@Test void testDecodeProvedDoublesLeniently() {
			assertThat(decode(x,

					field(RDF.VALUE, and(required(), datatype(XSD.DOUBLE))),

					createObjectBuilder()

							.add("value", 1)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, literal(1.0))

			);
		}


		@Test void testDecodeProvedTaggedValues() {
			assertThat(decode(x,

					field(RDF.VALUE, datatype(RDF.LANGSTRING)),

					createObjectBuilder()
							.add("@id", "/x")
							.add("value", createObjectBuilder()
									.add("en", createArrayBuilder().add("one").add("two"))
									.add("it", createArrayBuilder().add("uno"))
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, literal("one", "en")),
					statement(x, RDF.VALUE, literal("two", "en")),
					statement(x, RDF.VALUE, literal("uno", "it"))

			);
		}

		@Test void testDecodeProvedLocalizedValues() {
			assertThat(decode(x,

					field(RDF.VALUE, localized()),

					createObjectBuilder()
							.add("@id", "/x")
							.add("value", createObjectBuilder()
									.add("en", createValue("one"))
									.add("it", createValue("uno"))
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, literal("one", "en")),
					statement(x, RDF.VALUE, literal("uno", "it"))

			);
		}

		@Test void testDecodeProvedTaggedValuesWithKnownLanguage() {
			assertThat(decode(x,

					field(RDF.VALUE, lang("en")),

					createObjectBuilder()
							.add("@id", "/x")
							.add("value", createArrayBuilder()
									.add("one")
									.add("two")
							)

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, literal("one", "en")),
					statement(x, RDF.VALUE, literal("two", "en"))

			);
		}

		@Test void testDecodeProvedLocalizedValuesWithKnownLanguage() {
			assertThat(decode(x,

					field(RDF.VALUE, localized(), lang("en")),

					createObjectBuilder()
							.add("@id", "/x")
							.add("value", createValue("one"))

			)).isIsomorphicTo(

					statement(x, RDF.VALUE, literal("one", "en"))

			);
		}

	}

	@Nested final class Keywords {

		@Test void testHandleKeywordAliases() {
			assertThat(decode(x,

					field(RDF.NIL),

					Xtream.map(
							Xtream.entry("@id", "id"),
							Xtream.entry("@value", "value"),
							Xtream.entry("@type", "type"),
							Xtream.entry("@language", "language")
					),

					createObjectBuilder()
							.add("id", "/x")
							.add("nil", createArrayBuilder() // keyword alias overrides field alias
									.add(createObjectBuilder()
											.add("value", "string")
											.add("language", "en")
									)
									.add(createObjectBuilder()
											.add("value", "2020-09-10")
											.add("type", XSD.DATE.stringValue())
									)
							)

			)).isIsomorphicTo(

					statement(x, RDF.NIL, literal("string", "en")),
					statement(x, RDF.NIL, literal("2020-09-10", XSD.DATE))

			);
		}

		@Test void testIgnoreDuplicateKeywords() {
			assertThat(decode(x, and(), Xtream.map(Xtream.entry("@id", "id")), createObjectBuilder()

					.add("id", "/x")
					.add("@id", "/x")

			)).isIsomorphicTo();
		}

	}

}