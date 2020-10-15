/*
 * Copyright © 2013-2020 Metreeca srl
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
import com.metreeca.json.shapes.Range;
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
import static com.metreeca.json.shapes.Like.like;
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
import static com.metreeca.json.shapes.Stem.stem;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rest.EitherAssert.assertThat;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyMap;
import static javax.json.Json.*;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

final class JSONLDValidatorTest {

	private static final Literal x=literal("x");
	private static final Literal y=literal("y");
	private static final Literal z=literal("z");


	private Either<Trace, JsonObject> validate(final Shape shape, final JsonValue... values) {

		final JsonArrayBuilder array=createArrayBuilder();

		for (final JsonValue value : values) {
			array.add(value);
		}

		return validate(field(RDF.VALUE, shape), createObjectBuilder().add("value", array));
	}

	private Either<Trace, JsonObject> validate(final Shape shape, final JsonObjectBuilder builder) {
		return new JSONLDValidator(iri("app:/"), shape, emptyMap()).validate(builder.build());
	}


	@Nested final class Validation {

		@Test void testValidateShapeEnvelope() {

			final Shape shape=field(RDF.VALUE, all(x));

			assertThat(validate(shape, createObjectBuilder()
					.add("value", "x")
			)).hasRight();

			assertThat(validate(shape, createObjectBuilder()
					.add("value", "x")
					.add("unknown", "x")
			)).hasLeft();

		}


		@Test void testValidateField() {

			final Shape shape=field(RDF.VALUE, minCount(1));

			assertThat(validate(shape, createObjectBuilder().add("value", createArrayBuilder(asList("x", "y"))))).hasRight();

			assertThat(validate(shape, createObjectBuilder())).as("empty focus").hasLeft();

		}

		@Test void testValidateDirectFields() {

			final Shape shape=field(RDF.VALUE, all(y));

			assertThat(validate(shape, createObjectBuilder().add("value", createArrayBuilder(asList("x", "y"))))).hasRight();

			assertThat(validate(shape, createObjectBuilder().add("value", "x"))).hasLeft();

		}

		@Test void testValidateInverseFields() {

			final Shape shape=field(inverse(RDF.VALUE), all(iri("http://example.com/")));

			assertThat(validate(shape, createObjectBuilder().add("valueOf", "http://example.com/"))).hasRight();
			assertThat(validate(shape, createValue("x"))).hasLeft();

		}


		@Test void testValidateAnd() {

			final Shape shape=and(any(x), any(y));

			assertThat(validate(shape, createValue("x"), createValue("y"), createValue("z"))).hasRight();
			assertThat(validate(shape, createValue("x"), createValue("z"))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		}

		@Test void testValidateOr() {

			final Shape shape=or(all(x, y), all(x, z));

			assertThat(validate(shape, createValue("x"), createValue("y"), createValue("z"))).hasRight();
			assertThat(validate(shape, createValue("y"), createValue("z"))).hasLeft();

		}

		@Test void ValidateWhen() {

			final Shape shape=when(
					datatype(XSD.INTEGER),
					maxInclusive(literal(100)),
					maxInclusive(literal("10"))
			);

			assertThat(validate(shape, createValue(100))).hasRight();
			assertThat(validate(shape, createValue("100.0"))).hasLeft();
		}


		@Test void testReportUnredactedGuard() {
			assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
					validate(when(guard("axis", "value"), maxInclusive(literal(100))))
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


			assertThat(validate(datatype(ValueType), iri)).hasRight();
			assertThat(validate(datatype(ValueType), bnode)).hasRight();
			assertThat(validate(datatype(ValueType), number)).hasRight();

			assertThat(validate(datatype(ResourceType), iri)).hasRight();
			assertThat(validate(datatype(ResourceType), bnode)).hasRight();
			assertThat(validate(datatype(ResourceType), number)).hasLeft();

			assertThat(validate(datatype(BNodeType), bnode)).hasRight();
			assertThat(validate(datatype(BNodeType), number)).hasLeft();

			assertThat(validate(datatype(IRIType), iri)).hasRight();
			assertThat(validate(datatype(IRIType), bnode)).hasLeft();

			assertThat(validate(datatype(LiteralType), string)).hasRight();
			assertThat(validate(datatype(LiteralType), number)).hasRight();
			assertThat(validate(datatype(LiteralType), bnode)).hasLeft();

			assertThat(validate(datatype(XSD.STRING), string)).hasRight();
			assertThat(validate(datatype(XSD.STRING), bnode)).hasLeft();

			assertThat(validate(datatype(XSD.DATE), typed)).hasRight();
			assertThat(validate(datatype(XSD.DATE), bnode)).hasLeft();

			assertThat(validate(datatype(RDF.LANGSTRING), tagged)).hasRight();
			assertThat(validate(datatype(RDF.LANGSTRING), bnode)).hasLeft();

			assertThat(validate(datatype(XSD.BOOLEAN), JsonValue.TRUE)).hasRight();
			assertThat(validate(datatype(XSD.BOOLEAN), bnode)).hasLeft();

			assertThat(validate(datatype(IRIType))).as("empty focus").hasRight();

		}

		@Test void testValidateRange() {

			final Shape shape=Range.range(x, y);

			assertThat(validate(shape, createValue("x"), createValue("y"))).hasRight();
			assertThat(validate(shape, createValue("x"), createValue("y"), createValue("z"))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}


		@Test void testValidateMinExclusive() {

			final Shape shape=minExclusive(literal(1));

			assertThat(validate(shape, createValue(2))).hasRight();
			assertThat(validate(shape, createValue(1))).hasLeft();
			assertThat(validate(shape, createValue(0))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMaxExclusive() {

			final Shape shape=maxExclusive(literal(10));

			assertThat(validate(shape, createValue(2))).hasRight();
			assertThat(validate(shape, createValue(10))).hasLeft();
			assertThat(validate(shape, createValue(100))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMinInclusive() {

			final Shape shape=minInclusive(literal(1));

			assertThat(validate(shape, createValue(2))).hasRight();
			assertThat(validate(shape, createValue(1))).hasRight();
			assertThat(validate(shape, createValue(0))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMaxInclusive() {

			final Shape shape=maxInclusive(literal(10));

			assertThat(validate(shape, createValue(2))).hasRight();
			assertThat(validate(shape, createValue(10))).hasRight();
			assertThat(validate(shape, createValue(100))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}


		@Test void testValidateMinLength() {

			final Shape shape=minLength(3);

			assertThat(validate(shape, createValue(100))).hasRight();
			assertThat(validate(shape, createValue(99))).hasLeft();

			assertThat(validate(shape, createValue("100"))).hasRight();
			assertThat(validate(shape, createValue("99"))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMaxLength() {

			final Shape shape=maxLength(2);

			assertThat(validate(shape, createValue(99))).hasRight();
			assertThat(validate(shape, createValue(100))).hasLeft();

			assertThat(validate(shape, createValue("99"))).hasRight();
			assertThat(validate(shape, createValue("100"))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidatePattern() {

			final Shape shape=pattern(".*\\.org");

			assertThat(validate(shape, createObjectBuilder().add("@id", "http://example.org"))).hasRight();
			assertThat(validate(shape, createObjectBuilder().add("@id", "http://example.com"))).hasLeft();

			assertThat(validate(shape, createValue("example.org"))).hasRight();
			assertThat(validate(shape, createValue("example.com"))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateLike() {

			final Shape shape=like("ex.org", true);

			assertThat(validate(shape, createObjectBuilder().add("@id", "http://exampe.org/"))).hasRight();
			assertThat(validate(shape, createObjectBuilder().add("@id", "http://exampe.com/"))).hasLeft();

			assertThat(validate(shape, createValue("example.org"))).hasRight();
			assertThat(validate(shape, createValue("example.com"))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateStem() {

			final Shape shape=stem("http://example.com/");

			assertThat(validate(shape, createObjectBuilder().add("@id", "http://example.com/"))).hasRight();
			assertThat(validate(shape, createObjectBuilder().add("@id", "http://example.net/"))).hasLeft();

			assertThat(validate(shape, createObjectBuilder().add("@id", "http://example.com/resource"))).hasRight();
			assertThat(validate(shape, createObjectBuilder().add("@id", "http://example.net/resource"))).hasLeft();

			assertThat(validate(shape, createValue("http://example.com/resource"))).hasRight();
			assertThat(validate(shape, createValue("http://example.net/resource"))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}


		@Test void testValidateMinCount() {

			final Shape shape=minCount(2);

			assertThat(validate(shape, createValue(1), createValue(2), createValue(3))).hasRight();
			assertThat(validate(shape, createValue(1))).hasLeft();

		}

		@Test void testValidateMaxCount() {

			final Shape shape=maxCount(2);

			assertThat(validate(shape, createValue(1), createValue(2))).hasRight();
			assertThat(validate(shape, createValue(1), createValue(2), createValue(3))).hasLeft();

		}

		@Test void testValidateAll() {

			final Shape shape=all(x, y);

			assertThat(validate(shape, createValue("x"), createValue("y"), createValue("z"))).hasRight();
			assertThat(validate(shape, createValue("x"))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		}

		@Test void testValidateAny() {

			final Shape shape=any(x, y);

			assertThat(validate(shape, createValue("x"))).hasRight();
			assertThat(validate(shape, createValue("z"))).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		}

	}

}
