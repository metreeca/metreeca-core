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

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;
import com.metreeca.json.Trace;
import com.metreeca.rest.Either;

import org.eclipse.rdf4j.model.Literal;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import javax.json.*;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.guard;
import static com.metreeca.json.shapes.Lang.lang;
import static com.metreeca.json.shapes.Like.like;
import static com.metreeca.json.shapes.Localized.localized;
import static com.metreeca.json.shapes.MaxCount.maxCount;
import static com.metreeca.json.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MaxLength.maxLength;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.MinExclusive.minExclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.MinLength.minLength;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Pattern.pattern;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.Stem.stem;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rest.EitherAssert.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static javax.json.Json.*;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

final class JSONLDScannerTest {

	private static final Literal x=literal("x");
	private static final Literal y=literal("y");
	private static final Literal z=literal("z");


	private Either<Trace, JsonObject> scan(final Shape shape, final JsonValue... values) {

		final JsonArrayBuilder array=createArrayBuilder();

		for (final JsonValue value : values) {
			array.add(value);
		}

		return scan(field(RDF.VALUE, shape), createObjectBuilder().add("value", array));
	}

	private Either<Trace, JsonObject> scan(final Shape shape, final JsonObjectBuilder builder) {
		return new JSONLDScanner(iri("app:/"), shape.expand(), emptyMap()).scan(builder.build());
	}


	@Nested final class Validation {

		@Test void testValidateShapeEnvelope() {

			final Shape shape=field(RDF.VALUE, all(x));

			assertThat(scan(shape, createObjectBuilder()
					.add("value", "x")
			)).hasRight();

			assertThat(scan(shape, createObjectBuilder()
					.add("value", "x")
					.add("unknown", "x")
			)).hasLeft();

		}


		@Test void testValidateField() {

			final Shape shape=field(RDF.VALUE, minCount(1));

			assertThat(scan(shape, createObjectBuilder().add("value", createArrayBuilder(asList("x", "y"))))).hasRight();

			assertThat(scan(shape, createObjectBuilder())).as("empty focus").hasLeft();

		}

		@Test void testValidateDirectFields() {

			final Shape shape=field(RDF.VALUE, all(y));

			assertThat(scan(shape, createObjectBuilder().add("value", createArrayBuilder(asList("x", "y"))))).hasRight();

			assertThat(scan(shape, createObjectBuilder().add("value", "x"))).hasLeft();

		}

		@Test void testValidateInverseFields() {

			final Shape shape=field(inverse(RDF.VALUE), all(iri("http://example.com/")));

			assertThat(scan(shape, createObjectBuilder().add("valueOf", "http://example.com/"))).hasRight();
			assertThat(scan(shape, createValue("x"))).hasLeft();

		}


		@Test void testValidateAnd() {

			final Shape shape=and(any(x), any(y));

			assertThat(scan(shape, createValue("x"), createValue("y"), createValue("z"))).hasRight();
			assertThat(scan(shape, createValue("x"), createValue("z"))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasLeft();

		}

		@Test void testValidateOr() {

			final Shape shape=or(all(x, y), all(x, z));

			assertThat(scan(shape, createValue("x"), createValue("y"), createValue("z"))).hasRight();
			assertThat(scan(shape, createValue("y"), createValue("z"))).hasLeft();

		}

		@Test void ValidateWhen() {

			final Shape shape=when(
					datatype(XSD.INTEGER),
					maxInclusive(literal(100)),
					maxInclusive(literal("10"))
			);

			assertThat(scan(shape, createValue(100))).hasRight();
			assertThat(scan(shape, createValue("100.0"))).hasLeft();
		}


		@Test void testReportUnredactedGuard() {
			assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
					scan(when(guard("axis", "value"), maxInclusive(literal(100))))
			);
		}

	}

	@Nested final class Constraints {

		@Test void testValidateDatatype() {

			final JsonValue iri=createObjectBuilder().add("@id", "http://example.com/").build();
			final JsonValue bnode=createObjectBuilder().build();

			final JsonValue number=createValue(1);
			final JsonString string=createValue("text");

			final JsonObject typed=createObjectBuilder()
					.add("@value", "2020-09-25")
					.add("@type", XSD.DATE.stringValue())
					.build();

			final JsonObject tagged=createObjectBuilder()
					.add("@value", "text")
					.add("@language", "en")
					.build();


			assertThat(scan(datatype(ValueType), iri)).hasRight();
			assertThat(scan(datatype(ValueType), bnode)).hasRight();
			assertThat(scan(datatype(ValueType), number)).hasRight();

			assertThat(scan(datatype(ResourceType), iri)).hasRight();
			assertThat(scan(datatype(ResourceType), bnode)).hasRight();
			assertThat(scan(datatype(ResourceType), number)).hasLeft();

			assertThat(scan(datatype(BNodeType), bnode)).hasRight();
			assertThat(scan(datatype(BNodeType), number)).hasLeft();

			assertThat(scan(datatype(IRIType), iri)).hasRight();
			assertThat(scan(datatype(IRIType), bnode)).hasLeft();

			assertThat(scan(datatype(LiteralType), string)).hasRight();
			assertThat(scan(datatype(LiteralType), number)).hasRight();
			assertThat(scan(datatype(LiteralType), bnode)).hasLeft();

			assertThat(scan(datatype(XSD.STRING), string)).hasRight();
			assertThat(scan(datatype(XSD.STRING), bnode)).hasLeft();

			assertThat(scan(datatype(XSD.DATE), typed)).hasRight();
			assertThat(scan(datatype(XSD.DATE), bnode)).hasLeft();

			assertThat(scan(datatype(RDF.LANGSTRING), tagged)).hasRight();
			assertThat(scan(datatype(RDF.LANGSTRING), iri)).hasLeft();

			assertThat(scan(datatype(XSD.BOOLEAN), JsonValue.TRUE)).hasRight();
			assertThat(scan(datatype(XSD.BOOLEAN), bnode)).hasLeft();

			assertThat(scan(datatype(IRIType))).as("empty focus").hasRight();

		}

		@Test void testValidateRange() {

			final Shape shape=range(x, y);

			assertThat(scan(shape, createValue("x"), createValue("y"))).hasRight();
			assertThat(scan(shape, createValue("x"), createValue("y"), createValue("z"))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateGenericLang() {

			final Shape shape=lang();

			assertThat(scan(shape, createObjectBuilder()
					.add("@value", "one")
					.add("@language", "en")
			)).hasRight();


			assertThat(scan(shape, createObjectBuilder()
					.add("en", "one")
					.add("it", "one")
			)).hasRight();

			assertThat(scan(shape, createObjectBuilder()
					.add("@id", "http://example.com/")
			)).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateRestrictedLang() {

			final Shape shape=lang("en", "fr");

			assertThat(scan(shape, createObjectBuilder()
					.add("@value", "one")
					.add("@language", "en")
			)).hasRight();

			assertThat(scan(shape, createObjectBuilder()
					.add("@value", "uno")
					.add("@language", "it")
			)).hasLeft();

			assertThat(scan(shape, createObjectBuilder()
					.add("en", "one")
			)).hasRight();

			assertThat(scan(shape, createObjectBuilder()
					.add("en", "one")
					.add("it", "one")
			)).hasLeft();

			assertThat(scan(shape, createObjectBuilder()
					.add("it", "one")
			)).hasLeft();

			assertThat(scan(shape, createObjectBuilder()
					.add("@id", "http://example.com/")
			)).hasLeft();

			assertThat(scan(lang("en"), createValue("one"))).as("known language").hasRight();
			assertThat(scan(shape)).as("empty focus").hasRight();

		}


		@Test void testValidateMinExclusive() {

			final Shape shape=minExclusive(literal(1));

			assertThat(scan(shape, createValue(2))).hasRight();
			assertThat(scan(shape, createValue(1))).hasLeft();
			assertThat(scan(shape, createValue(0))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMaxExclusive() {

			final Shape shape=maxExclusive(literal(10));

			assertThat(scan(shape, createValue(2))).hasRight();
			assertThat(scan(shape, createValue(10))).hasLeft();
			assertThat(scan(shape, createValue(100))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMinInclusive() {

			final Shape shape=minInclusive(literal(1));

			assertThat(scan(shape, createValue(2))).hasRight();
			assertThat(scan(shape, createValue(1))).hasRight();
			assertThat(scan(shape, createValue(0))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMaxInclusive() {

			final Shape shape=maxInclusive(literal(10));

			assertThat(scan(shape, createValue(2))).hasRight();
			assertThat(scan(shape, createValue(10))).hasRight();
			assertThat(scan(shape, createValue(100))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}


		@Test void testValidateMinLength() {

			final Shape shape=minLength(3);

			assertThat(scan(shape, createValue(100))).hasRight();
			assertThat(scan(shape, createValue(99))).hasLeft();

			assertThat(scan(shape, createValue("100"))).hasRight();
			assertThat(scan(shape, createValue("99"))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMaxLength() {

			final Shape shape=maxLength(2);

			assertThat(scan(shape, createValue(99))).hasRight();
			assertThat(scan(shape, createValue(100))).hasLeft();

			assertThat(scan(shape, createValue("99"))).hasRight();
			assertThat(scan(shape, createValue("100"))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

		@Test void testValidatePattern() {

			final Shape shape=pattern(".*\\.org");

			assertThat(scan(shape, createObjectBuilder().add("@id", "http://example.org"))).hasRight();
			assertThat(scan(shape, createObjectBuilder().add("@id", "http://example.com"))).hasLeft();

			assertThat(scan(shape, createValue("example.org"))).hasRight();
			assertThat(scan(shape, createValue("example.com"))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateLike() {

			final Shape shape=like("ex.org", true);

			assertThat(scan(shape, createObjectBuilder().add("@id", "http://exampe.org/"))).hasRight();
			assertThat(scan(shape, createObjectBuilder().add("@id", "http://exampe.com/"))).hasLeft();

			assertThat(scan(shape, createValue("example.org"))).hasRight();
			assertThat(scan(shape, createValue("example.com"))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateStem() {

			final Shape shape=stem("http://example.com/");

			assertThat(scan(shape, createObjectBuilder().add("@id", "http://example.com/"))).hasRight();
			assertThat(scan(shape, createObjectBuilder().add("@id", "http://example.net/"))).hasLeft();

			assertThat(scan(shape, createObjectBuilder().add("@id", "http://example.com/resource"))).hasRight();
			assertThat(scan(shape, createObjectBuilder().add("@id", "http://example.net/resource"))).hasLeft();

			assertThat(scan(shape, createValue("http://example.com/resource"))).hasRight();
			assertThat(scan(shape, createValue("http://example.net/resource"))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}


		@Test void testValidateMinCount() {

			final Shape shape=minCount(2);

			assertThat(scan(shape, createValue(1), createValue(2), createValue(3))).hasRight();
			assertThat(scan(shape, createValue(1))).hasLeft();

		}

		@Test void testValidateMaxCount() {

			final Shape shape=maxCount(2);

			assertThat(scan(shape, createValue(1), createValue(2))).hasRight();
			assertThat(scan(shape, createValue(1), createValue(2), createValue(3))).hasLeft();

		}

		@Test void testValidateAll() {

			final Shape shape=all(x, y);

			assertThat(scan(shape, createValue("x"), createValue("y"), createValue("z"))).hasRight();
			assertThat(scan(shape, createValue("x"))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasLeft();

		}

		@Test void testValidateAny() {

			final Shape shape=any(x, y);

			assertThat(scan(shape, createValue("x"))).hasRight();
			assertThat(scan(shape, createValue("z"))).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasLeft();

		}

		@Test void testValidateLocalized() {

			final Shape shape=localized();

			assertThat(scan(shape,
					createObjectBuilder().add("@value", "one").add("@language", "en").build(),
					createObjectBuilder().add("@value", "uno").add("@language", "it").build()
			)).hasRight();

			assertThat(scan(shape,
					createObjectBuilder().add("@value", "one").add("@language", "en").build(),
					createObjectBuilder().add("@value", "two").add("@language", "en").build()
			)).hasLeft();

			assertThat(scan(shape, createObjectBuilder()
					.add("en", "one")
					.add("it", "uno")
			)).hasRight();

			assertThat(scan(shape, createObjectBuilder()
					.add("en", createArrayBuilder().add("one").add("two"))
					.add("it", "uno")
			)).hasLeft();

			assertThat(scan(shape)).as("empty focus").hasRight();

		}

	}

}
