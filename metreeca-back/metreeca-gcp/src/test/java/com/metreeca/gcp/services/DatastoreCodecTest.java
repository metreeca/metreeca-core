/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.gcp.services;

import com.metreeca.json.Frame;
import com.metreeca.json.Shape;
import com.metreeca.json.shapes.Lang;

import com.google.cloud.Timestamp;
import com.google.cloud.datastore.Value;
import com.google.cloud.datastore.*;
import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.time.*;

import static com.metreeca.gcp.services.Datastore.datastore;
import static com.metreeca.gcp.services.DatastoreTest.exec;
import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Shape.*;
import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Localized.localized;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.rest.Toolbox.service;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

final class DatastoreCodecTest {

	private static final String base="http://example.com/";

	private final IRI w=iri(base, "w");
	private final IRI x=iri(base, "x");
	private final IRI y=iri(base, "y");
	private final IRI z=iri(base, "z");


	private Key key(final String id) {
		return service(datastore()).query(datastore -> datastore
				.newKeyFactory()
				.setKind("Resource")
				.addAncestor(PathElement.of("Context", base))
				.newKey(id)
		);
	}


	private FullEntity<?> decode(final Frame frame, final Shape shape) {
		return service(datastore()).query(datastore -> new DatastoreCodec(iri(base), datastore).decode(frame, shape));
	}

	private Frame encode(final Entity entity, final Shape shape) {
		return service(datastore()).query(datastore -> new DatastoreCodec(iri(base), datastore).encode(entity, shape));
	}


	@Nested final class Values {

		private Value<?> decode(final org.eclipse.rdf4j.model.Value value) {
			return service(datastore()).query(datastore -> new DatastoreCodec(iri(base), datastore)
					.decode(frame(iri(base)).value(RDF.VALUE, value), field(RDF.VALUE, optional())) // wrap inside frame
					.getValue("value") // then unwrap
			);
		}

		private org.eclipse.rdf4j.model.Value encode(final Value<?> value) {
			return service(datastore()).query(datastore -> new DatastoreCodec(iri(base), datastore)
					.encode(FullEntity.newBuilder().set("value", value).build(), field(RDF.VALUE)) // wrap inside entity
					.value(RDF.VALUE) // then unwrap
					.orElseThrow(() -> new AssertionError("missing value"))
			);
		}


		@Test void testBNode() {
			exec(() -> {

				final BNode external=bnode("id");
				final Value<?> internal=KeyValue.of(key("id"));

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}


		@Test void testIRIInternal() {
			exec(() -> {

				final IRI external=iri(base, "/x");
				final Value<?> internal=KeyValue.of(key("/x"));

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}

		@Test void testIRIExternal() {
			exec(() -> {

				final IRI external=iri("http://example.net/");
				final Value<?> internal=KeyValue.of(key("http://example.net/"));

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}


		@Test void testBoolean() {
			exec(() -> {

				final Literal external=literal(true);
				final Value<?> internal=BooleanValue.of(true);

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}


		@Test void testLong() {
			exec(() -> {

				final Literal external=literal(1L, true);
				final Value<?> internal=EntityValue.of(FullEntity.newBuilder()
						.set("@value", "1")
						.set("@type", XSD.LONG.stringValue())
						.build()
				);

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}

		@Test void testDouble() {
			exec(() -> {

				final Literal external=literal(1.0, true);
				final Value<?> internal=EntityValue.of(FullEntity.newBuilder()
						.set("@value", "1.0")
						.set("@type", XSD.DOUBLE.stringValue())
						.build()
				);
				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}

		@Test void testInteger() {
			exec(() -> {

				final Literal external=literal(1L, false);
				final Value<?> internal=LongValue.of(1L);

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}

		@Test void testIntegerExtended() {
			exec(() -> {

				final BigInteger value=BigInteger.valueOf(Long.MAX_VALUE).multiply(BigInteger.TEN);

				final Literal external=literal(value);

				final Value<?> internal=EntityValue.of(FullEntity.newBuilder()
						.set("@type", XSD.INTEGER.stringValue())
						.set("@value", value.toString())
						.build()
				);

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}

		@Test void testDecimal() {
			exec(() -> {

				final Literal external=literal(1.0, false);
				final Value<?> internal=DoubleValue.of(1.0);

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}

		@Test void testDecimalExtended() {
			exec(() -> {

				final BigDecimal value=BigDecimal.valueOf(Double.MAX_VALUE).multiply(BigDecimal.TEN);

				final Literal external=literal(value);

				final Value<?> internal=EntityValue.of(FullEntity.newBuilder()
						.set("@type", XSD.DECIMAL.stringValue())
						.set("@value", value.toPlainString())
						.build()
				);

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}


		@Test void testString() {
			exec(() -> {

				final Literal external=literal("string");
				final StringValue internal=StringValue.of("string");

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}


		@Test void testDateTime() {
			exec(() -> {

				final Instant value=Instant.now();

				final Literal external=literal(OffsetDateTime.ofInstant(value, ZoneId.of("UTC")));

				final Value<?> internal=TimestampValue.of(Timestamp.ofTimeMicroseconds(value.toEpochMilli()*1000));

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}


		@Test void testTyped() {
			exec(() -> {

				final Literal external=literal("2019-04-03", XSD.DATE);
				final Value<?> internal=EntityValue.of(FullEntity.newBuilder()
						.set("@value", "2019-04-03")
						.set("@type", XSD.DATE.stringValue())
						.build()
				);

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});

		}

		@Test void testTagged() {
			exec(() -> {

				final Literal external=literal("value", "en");

				final Value<?> internal=EntityValue.of(FullEntity.newBuilder()
						.set("@value", "value")
						.set("@language", "en")
						.build()
				);

				assertThat(decode(external)).isEqualTo(internal);
				assertThat(encode(internal)).isEqualTo(external);

			});
		}

	}

	@Nested final class References {

		@Test void testExpandSharedTrees() {
			exec(() -> {

				final Frame external=frame(x)
						.frame(RDF.VALUE, frame(w).value(RDF.VALUE, z))
						.frame(RDF.VALUE, frame(y).value(RDF.VALUE, z));

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", ListValue.of(

								Entity.newBuilder(key("/w"))
										.set("value", KeyValue.of(key("/z")))
										.build(),

								Entity.newBuilder(key("/y"))
										.set("value", KeyValue.of(key("/z")))
										.build()

						))
						.build();

				final Shape shape=field(RDF.VALUE, and(repeatable(),
						field(RDF.VALUE, required())
				));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}


		@Test void testHandleNamedLoops() {
			exec(() -> {

				final Frame external=frame(x)
						.frame(RDF.VALUE, frame(y)
								.value(RDF.VALUE, x)
						);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", Entity.newBuilder(key("/y"))
								.set("value", KeyValue.of(key("/x")))
								.build()
						)
						.build();

				final Shape shape=field(RDF.VALUE, and(required(),
						field(RDF.VALUE, required())
				));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testHandleBlankLoops() {

			final BNode a=bnode("a");
			final BNode b=bnode("b");

			exec(() -> {

				final Frame external=frame(x)
						.frame(RDF.VALUE, frame(a)
								.frame(RDF.VALUE, frame(b)
										.value(RDF.VALUE, a)
								)
						);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", Entity.newBuilder(key("a"))
								.set("value", Entity.newBuilder(key("b"))
										.set("value", KeyValue.of(key("a")))
										.build()
								)
								.build()
						)
						.build();

				final Shape shape=field(RDF.VALUE, and(required(),
						field(RDF.VALUE, and(required(),
								field(RDF.VALUE, required())
						))
				));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}


		@Test void testBNodeWithBackLinkToProvedResource() {
			exec(() -> {

				final Shape shape=field(RDF.VALUE, and(required(),
						field(RDF.VALUE, and(required(), datatype(ResourceType)))
				));

				final Frame external=frame(x)
						.frame(RDF.VALUE, frame(y)
								.value(RDF.VALUE, x)
						);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", Entity.newBuilder(key("/y"))
								.set("value", "/x")
								.build()
						)
						.build();

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

	}

	@Nested final class Aliases {

		@Test void testAliasDirectField() {
			exec(() -> {

				final Frame external=frame(x).value(RDF.VALUE, y);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", KeyValue.of(key("/y")))
						.build();

				final Shape shape=field(RDF.VALUE, required());

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testAliasInverseField() {
			exec(() -> {

				final Frame external=frame(x).value(inverse(RDF.VALUE), y);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("valueOf", KeyValue.of(key("/y")))
						.build();

				final Shape shape=field(inverse(RDF.VALUE), required());

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testAliasUserLabelledField() {
			exec(() -> {

				final Frame external=frame(x).value(RDF.VALUE, y);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("label", KeyValue.of(key("/y")))
						.build();

				final Shape shape=field("label", RDF.VALUE, required());

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testAliasNestedField() {
			exec(() -> {

				final Frame external=frame(x).frame(RDF.VALUE, frame(y).value(RDF.VALUE, z));

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", Entity.newBuilder(key("/y"))
								.set("alias", KeyValue.of(key("/z")))
								.build()
						)
						.build();

				final Shape shape=field(RDF.VALUE, and(required(),
						field("alias", RDF.VALUE, required())
				));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

	}

	@Nested final class IRIs {

		private final IRI container=iri(base, "/container/");


		@Test void testRootRelativizeProvedIRIs() {
			exec(() -> {

				final Frame external=frame(container).values(RDF.VALUE,
						iri(base, "/container/x"),
						iri(base, "/container/y")
				);

				final Entity internal=Entity.newBuilder(key("/container/"))
						.set("value", ListValue.of(
								StringValue.of("/container/x"),
								StringValue.of("/container/y")
						))
						.build();

				final Shape shape=field(RDF.VALUE, datatype(IRIType));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testRelativizeProvedIRIBackReferences() {
			exec(() -> {

				final Frame external=frame(container).value(RDF.VALUE, container);

				final Entity internal=Entity.newBuilder(key("/container/"))
						.set("value", StringValue.of("/container/"))
						.build();

				final Shape shape=field(RDF.VALUE, and(required(), datatype(IRIType)));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

	}

	@Nested final class Shapes {

		@Test void testHandleScalars() {
			exec(() -> {

				final Frame external=frame(x).value(RDF.VALUE, y);
				final Entity internal=Entity.newBuilder(key("/x")).set("value", key("/y")).build();

				final Shape shape=field(RDF.VALUE, optional());

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testHandleArrays() {
			exec(() -> {

				final Frame external=frame(x).values(RDF.VALUE, y, z);
				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", asList(KeyValue.of(key("/y")), KeyValue.of(key("/z"))))
						.build();

				final Shape shape=field(RDF.VALUE, multiple());

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testOmitMissingValues() {
			exec(() -> {

				final Frame external=frame(x);
				final Entity internal=Entity.newBuilder(key("/x")).build();

				final Shape shape=field(RDF.VALUE, optional());

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testOmitEmptyArrays() {
			exec(() -> {

				final Frame external=frame(x);
				final Entity internal=Entity.newBuilder(key("/x")).build();

				final Shape shape=field(RDF.VALUE, repeatable());

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}


		@Test void testCompactProvedScalarValue() {
			exec(() -> {

				final Frame external=frame(x).value(RDF.VALUE, y);
				final Entity internal=Entity.newBuilder(key("/x")).set("value", key("/y")).build();

				final Shape shape=field(RDF.VALUE, maxCount(1));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testCompactProvedLeafIRI() {
			exec(() -> {

				final Frame external=frame(x).value(RDF.VALUE, y);
				final Entity internal=Entity.newBuilder(key("/x")).set("value", "/y").build();

				final Shape shape=field(RDF.VALUE, and(required(), datatype(IRIType)));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testCompactProvedTypedLiteral() {
			exec(() -> {

				final Frame external=frame(x).value(RDF.VALUE, literal("2019-04-03", XSD.DATE));
				final Entity internal=Entity.newBuilder(key("/x")).set("value", "2019-04-03").build();

				final Shape shape=field(RDF.VALUE, and(required(), datatype(XSD.DATE)));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}


		@Test void testCompactProvedTaggedValues() {
			exec(() -> {

				final Frame external=frame(x).values(RDF.VALUE,
						literal("one", "en"),
						literal("two", "en"),
						literal("uno", "it")
				);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", FullEntity.newBuilder()
								.set("en", asList(StringValue.of("one"), StringValue.of("two")))
								.set("it", singletonList(StringValue.of("uno")))
								.build()
						)
						.build();

				final Shape shape=field(RDF.VALUE, datatype(RDF.LANGSTRING));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testCompactProvedLocalizedValues() {
			exec(() -> {

				final Frame external=frame(x).values(RDF.VALUE,
						literal("one", "en"),
						literal("uno", "it")
				);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", FullEntity.newBuilder()
								.set("en", "one")
								.set("it", "uno")
								.build()
						)
						.build();

				final Shape shape=field(RDF.VALUE, localized());

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testCompactProvedTaggedValuesWithKnownLanguage() {
			exec(() -> {

				final Frame external=frame(x).values(RDF.VALUE,
						literal("one", "en"),
						literal("two", "en")
				);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", asList(StringValue.of("one"), StringValue.of("two")))
						.build();

				final Shape shape=field(RDF.VALUE, Lang.lang("en"));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

		@Test void testCompactProvedLocalizedValuesWithKnownLanguage() {
			exec(() -> {

				final Frame external=frame(x).values(RDF.VALUE,
						literal("one", "en")
				);

				final Entity internal=Entity.newBuilder(key("/x"))
						.set("value", "one")
						.build();

				final Shape shape=field(RDF.VALUE, localized("en"));

				assertThat(decode(external, shape)).isEqualTo(internal);
				assertThat(encode(internal, shape)).isEqualTo(external);

			});
		}

	}

}