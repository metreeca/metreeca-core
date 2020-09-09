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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.JsonObject;
import javax.json.JsonValue;

import static com.metreeca.core.formats.JSONAssert.assertThat;
import static com.metreeca.json.Shape.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.Meta.alias;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.rdf.Values.*;
import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static javax.json.Json.*;
import static javax.json.JsonValue.EMPTY_JSON_OBJECT;

final class JSONLDEncoderTest {

	private final String base="http://example.com/";

	private final IRI w=iri(base, "w");
	private final IRI z=iri(base, "z");
	private final IRI y=iri(base, "y");
	private final IRI x=iri(base, "x");


	private JsonObject encode(final IRI focus, final Shape shape, final Statement... model) {
		return new JSONLDEncoder(focus, shape).encode(asList(model));
	}


	@Nested final class Values {

		private JsonValue encode(final Value value) {
			return new JSONLDEncoder(iri(base), field(RDF.VALUE, optional()))
					.encode(singleton(statement(iri(base), RDF.VALUE, value))) // wrap value inside root object
					.get("value"); // then unwrap
		}


		@Test void testBNode() {
			assertThat(encode(bnode())).isEqualTo(EMPTY_JSON_OBJECT);
		}


		@Test void testIRIInternal() {
			assertThat(encode(iri(base, "/x"))).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.build());
		}

		@Test void testIRIExternal() {
			assertThat(encode(iri("http://example.net/"))).isEqualTo(createObjectBuilder()
					.add("@id", "http://example.net/")
			);
		}


		@Test void testBoolean() {
			assertThat(encode(True)).isEqualTo(JsonValue.TRUE);
			assertThat(encode(False)).isEqualTo(JsonValue.FALSE);
		}

		@Test void testString() {
			assertThat(encode(literal("string"))).isEqualTo(createValue("string"));
		}

		@Test void testInteger() {
			assertThat(encode(literal(integer(1)))).isEqualTo(createValue(integer(1)));
		}

		@Test void testDecimal() {
			assertThat(encode(literal(decimal(1)))).isEqualTo(createValue(decimal(1)));
		}


		@Test void testInt() {
			assertThat(encode(literal(1))).isEqualTo(createObjectBuilder()
					.add("@value", createValue("1"))
					.add("@type", createValue(XSD.INT.stringValue()))
			);
		}

		@Test void testDouble() {
			assertThat(encode(literal(1.0))).isEqualTo(createObjectBuilder()
					.add("@value", createValue("1.0"))
					.add("@type", createValue(XSD.DOUBLE.stringValue()))
			);
		}


		@Test void testCustom() {
			assertThat(encode(literal("value", iri(base, "type")))).isEqualTo(createObjectBuilder()
					.add("@value", createValue("value"))
					.add("@type", createValue(iri(base, "type").stringValue()))
			);
		}

		@Test void testTagged() {
			assertThat(encode(literal("value", "en"))).isEqualTo(createObjectBuilder()
					.add("@value", createValue("value"))
					.add("@language", createValue("en"))
			);
		}

		@Test void testTyped() {
			assertThat(encode(literal("2019-04-03", XSD.DATE))).isEqualTo(createObjectBuilder()
					.add("@value", createValue("2019-04-03"))
					.add("@type", createValue(XSD.DATE.stringValue()))
			);
		}

		@Test void testMalformed() {
			assertThat(encode(literal("none", XSD.BOOLEAN))).isEqualTo(createObjectBuilder()
					.add("@value", createValue("none"))
					.add("@type", createValue(XSD.BOOLEAN.toString()))
			);
		}

	}

	@Nested final class Focus {

		@Test void testIgnoreNoNFocusNodes() {
			assertThat(encode(x,

					field(RDF.VALUE, and()),

					statement(x, RDF.VALUE, literal("x")),
					statement(y, RDF.VALUE, literal("y"))

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", createArrayBuilder()
							.add("x")
					)
			);
		}

		@Test void testHandleUnknownFocusNode() {
			assertThat(encode(z,

					field(RDF.VALUE, and()),

					statement(x, RDF.VALUE, literal("x")),
					statement(y, RDF.VALUE, literal("y"))

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/z")
			);
		}

	}

	@Nested final class SharedReferences {

		@Test void testExpandSharedTrees() {
			assertThat(encode(x,

					field(RDF.VALUE, and(repeatable(),
							field(RDF.VALUE, required())
					)),

					statement(x, RDF.VALUE, w),
					statement(x, RDF.VALUE, y),

					statement(w, RDF.VALUE, z),
					statement(y, RDF.VALUE, z)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", createArrayBuilder()

							.add(createObjectBuilder()
									.add("@id", "/w")
									.add("value", createObjectBuilder()
											.add("@id", "/z")
									)
							)

							.add(createObjectBuilder()
									.add("@id", "/y")
									.add("value", createObjectBuilder()
											.add("@id", "/z")
									)
							)

					)
			);
		}

		@Test void testHandleNamedLoops() {
			assertThat(encode(x,

					field(RDF.VALUE, and(required(),
							field(RDF.VALUE, required())
					)),

					statement(x, RDF.VALUE, y),
					statement(y, RDF.VALUE, x)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", createObjectBuilder()
							.add("@id", "/y")
							.add("value", createObjectBuilder()
									.add("@id", "/x")
							)
					)
			);
		}

		@Test void testHandleBlankLoops() {

			final BNode a=bnode("a");
			final BNode b=bnode("b");

			assertThat(encode(x,

					field(RDF.VALUE, and(required(),
							field(RDF.VALUE, and(required(),
									field(RDF.VALUE, required())
							))
					)),

					statement(x, RDF.VALUE, a),
					statement(a, RDF.VALUE, b),
					statement(b, RDF.VALUE, a)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", createObjectBuilder()
							.add("@id", "_:a")
							.add("value", createObjectBuilder()
									.add("value", createObjectBuilder()
											.add("@id", "_:a")
									)
							)
					)
			);
		}


		@Test void testBNodeWithBackLinkToProvedResource() {
			assertThat(encode(x,

					field(RDF.VALUE, and(required(),
							field(RDF.VALUE, and(required(), datatype(ResourceType)))
					)),

					statement(x, RDF.VALUE, y),
					statement(y, RDF.VALUE, x)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", createObjectBuilder()
							.add("@id", "/y")
							.add("value", "/x")
					)
			);
		}

	}

	@Nested final class Aliases {

		@Test void testAliasDirectField() {
			assertThat(encode(x,

					field(RDF.VALUE, required()),

					statement(x, RDF.VALUE, y)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", createObjectBuilder()
							.add("@id", "/y")
					)
			);
		}

		@Test void testAliasInverseField() {
			assertThat(encode(x,

					field(inverse(RDF.VALUE), required()),

					statement(y, RDF.VALUE, x)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("valueOf", "/y") // !!! valueOf?
			);
		}

		@Test void testAliasUserLabelledField() {
			assertThat(encode(x,

					field(RDF.VALUE, and(required(), alias("alias"))),

					statement(x, RDF.VALUE, y)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("alias", createObjectBuilder()
							.add("@id", "/y")
					)
			);
		}

		@Test void testAliasNestedField() {
			assertThat(encode(x,

					field(RDF.VALUE, and(required(),
							field(RDF.VALUE, and(required(), alias("alias")))
					)),

					statement(x, RDF.VALUE, y),
					statement(y, RDF.VALUE, z)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", createObjectBuilder()
							.add("@id", "/y")
							.add("alias", createObjectBuilder()
									.add("@id", "/z")
							)
					)
			);
		}

		@Test void testHandleAliasClashes() {

			final IRI value=iri(base, "value");

			assertThat(encode(x,

					and(
							field(RDF.VALUE, required()),
							field(value, required())
					),

					statement(x, RDF.VALUE, y),
					statement(x, value, y)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add(RDF.VALUE.stringValue(), createObjectBuilder()
							.add("@id", "/y")
					)
					.add(value.stringValue(), createObjectBuilder()
							.add("@id", "/y")
					)
			);
		}

		@Test void testIgnoreReservedAliases() {
			assertThat(encode(x,

					field(RDF.VALUE, and(required(), alias("@id"))),

					statement(x, RDF.VALUE, y)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", createObjectBuilder()
							.add("@id", "/y")
					)
			);
		}

	}

	@Nested final class IRIs {

		private final IRI container=iri(base, "/container/");


		@Test void testRootRelativizeProvedIRIs() {
			assertThat(encode(container,

					field(RDF.VALUE, datatype(IRIType)),

					statement(container, RDF.VALUE, iri(base, "/container/x")),
					statement(container, RDF.VALUE, iri(base, "/container/y"))

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/container/")
					.add("value", createArrayBuilder()
							.add("/container/x")
							.add("/container/y")
					)
			);
		}

		@Test void testRelativizeProvedIRIBackReferences() {
			assertThat(encode(container,

					field(RDF.VALUE, and(required(), datatype(IRIType))),

					statement(container, RDF.VALUE, container)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/container/")
					.add("value", "/container/")
			);
		}

	}

	@Nested final class Shapes {

		@Test void testOmitMissingValues() {
			assertThat(encode(x,

					field(RDF.VALUE, optional())

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
			);
		}

		@Test void testOmitEmptyArrays() {
			assertThat(encode(x,

					field(RDF.VALUE)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
			);
		}


		@Test void testCompactProvedScalarValue() {
			assertThat(encode(x,

					field(RDF.VALUE, maxCount(1)),

					statement(x, RDF.VALUE, y)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", createObjectBuilder()
							.add("@id", "/y")
					)
			);
		}

		@Test void testCompactProvedLeafIRI() {
			assertThat(encode(x,

					field(RDF.VALUE, and(required(), datatype(IRIType))),

					statement(x, RDF.VALUE, y)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", "/y")
			);
		}

		@Test void testCompactProvedTypedLiteral() {
			assertThat(encode(x,

					field(RDF.VALUE, and(required(), datatype(XSD.DATE))),

					statement(x, RDF.VALUE, literal("2019-04-03", XSD.DATE))

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("value", "2019-04-03")
			);
		}


		@Test void testConsiderDisjunctiveDefinitions() {
			assertThat(encode(x,

					or(
							field(RDF.FIRST, required()),
							field(RDF.REST, required())
					),

					statement(x, RDF.FIRST, y),
					statement(x, RDF.REST, z)

			)).isEqualTo(createObjectBuilder()
					.add("@id", "/x")
					.add("first", createObjectBuilder()
							.add("@id", "/y")
					)
					.add("rest", createObjectBuilder()
							.add("@id", "/z")
					)
			);

		}


	}

}