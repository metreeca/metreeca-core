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

import com.metreeca.json.*;
import com.metreeca.json.shapes.Range;
import com.metreeca.rest.Either;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.ValuesTest.decode;
import static com.metreeca.json.ValuesTest.item;
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
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rest.EitherAssert.assertThat;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyMap;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

final class JSONLDValidatorTest {

	private static final IRI x=item("x");
	private static final IRI y=item("y");
	private static final IRI z=item("z");


	private Collection<Statement> model(final String... model) {
		return model.length == 0 ? emptySet() : decode("<app:/> rdf:value "
				+stream(model).collect(joining(" . ", "", " . "))
		);
	}


	private Either<Trace, Collection<Statement>> validate(final Shape shape, final String... model) {
		return validate(shape, model(model));
	}

	private Either<Trace, Collection<Statement>> validate(final Shape shape, final Collection<Statement> model) {
		return new JSONLDValidator(iri("app:/"), field(RDF.VALUE, shape), emptyMap()).validate(model);
	}


	@Nested final class Validation {

		@Test void testValidateShapeEnvelope() {

			final Shape shape=all(x);

			assertThat(validate(shape, "<x>")).hasRight();
			assertThat(validate(shape, "<x>; rdf:rest rdf:nil")).hasLeft();

		}


		@Test void testValidateField() {

			final Shape shape=minCount(1);

			assertThat(validate(shape, "<x>, <z>")).hasRight();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		}

		@Test void testValidateDirectFields() {

			final Shape shape=field(RDF.VALUE, all(y));

			assertThat(validate(shape, "<x>", "<x> rdf:value <y>")).hasRight();
			assertThat(validate(shape, "<x>")).hasLeft();

		}

		@Test void testValidateInverseFields() {

			final Shape shape=field(inverse(RDF.VALUE), all(y));

			assertThat(validate(shape, "<x>", "<y> rdf:value <x>")).hasRight();
			assertThat(validate(shape, "<x>")).hasLeft();

		}


		@Test void testValidateAnd() {

			final Shape shape=and(any(x), any(y));

			assertThat(validate(shape, "<x>, <y>, <z>")).hasRight();
			assertThat(validate(shape, "<x>, <z>")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		}

		@Test void testValidateOr() {

			final Shape shape=or(all(x, y), all(x, z));

			assertThat(validate(shape, "<x>, <y>, <z>")).hasRight();
			assertThat(validate(shape, "<y>, <z>")).hasLeft();

		}

		@Test void ValidateWhen() {

			final Shape shape=when(
					datatype(XSD.INTEGER),
					maxInclusive(literal(100)),
					maxInclusive(literal(10))
			);

			assertThat(validate(shape, "100")).hasRight();
			assertThat(validate(shape, "100.0")).hasLeft();
		}


		@Test void testReportUnredactedGuard() {
			assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() ->
					validate(when(guard("axis", "value"), maxInclusive(literal(100))))
			);
		}

	}

	@Nested final class Constraints {

		@Test void testValidateDatatype() {

			assertThat(validate(datatype(Values.ValueType), "<x>")).hasRight();
			assertThat(validate(datatype(Values.ValueType), "_:x")).hasRight();
			assertThat(validate(datatype(Values.ValueType), "1")).hasRight();

			assertThat(validate(datatype(Values.ResourceType), "<x>")).hasRight();
			assertThat(validate(datatype(Values.ResourceType), "_:x")).hasRight();
			assertThat(validate(datatype(Values.ResourceType), "1")).hasLeft();

			assertThat(validate(datatype(Values.BNodeType), "_:x")).hasRight();
			assertThat(validate(datatype(Values.BNodeType), "1")).hasLeft();

			assertThat(validate(datatype(Values.IRIType), "<x>")).hasRight();
			assertThat(validate(datatype(Values.IRIType), "_:x")).hasLeft();

			assertThat(validate(datatype(Values.LiteralType), "'x'")).hasRight();
			assertThat(validate(datatype(Values.LiteralType), "1")).hasRight();
			assertThat(validate(datatype(Values.LiteralType), "_:x")).hasLeft();

			assertThat(validate(datatype(XSD.STRING), "'text'")).hasRight();
			assertThat(validate(datatype(XSD.STRING), "_:x")).hasLeft();

			assertThat(validate(datatype(RDF.LANGSTRING), "'text'@en")).hasRight();
			assertThat(validate(datatype(RDF.LANGSTRING), "_:x")).hasLeft();

			assertThat(validate(datatype(XSD.BOOLEAN), "true")).hasRight();
			assertThat(validate(datatype(XSD.BOOLEAN), "_:x")).hasLeft();

			assertThat(validate(datatype(Values.IRIType))).as("empty focus").hasRight();

		}

		@Test void testValidateRange() {

			final Shape shape=Range.range(x, y);

			assertThat(validate(shape, "<x>, <y>")).hasRight();
			assertThat(validate(shape, "<x>, <y>, <z>")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}


		@Test void testValidateMinExclusive() {

			final Shape shape=minExclusive(literal(1));

			assertThat(validate(shape, "2")).hasRight();
			assertThat(validate(shape, "1")).hasLeft();
			assertThat(validate(shape, "0")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMaxExclusive() {

			final Shape shape=maxExclusive(literal(10));

			assertThat(validate(shape, "2")).hasRight();
			assertThat(validate(shape, "10")).hasLeft();
			assertThat(validate(shape, "100")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMinInclusive() {

			final Shape shape=minInclusive(literal(1));

			assertThat(validate(shape, "2")).hasRight();
			assertThat(validate(shape, "1")).hasRight();
			assertThat(validate(shape, "0")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMaxInclusive() {

			final Shape shape=maxInclusive(literal(10));

			assertThat(validate(shape, "2")).hasRight();
			assertThat(validate(shape, "10")).hasRight();
			assertThat(validate(shape, "100")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}


		@Test void testValidatePattern() {

			final Shape shape=pattern(".*\\.org");

			assertThat(validate(shape, "<http://exampe.org>")).hasRight();
			assertThat(validate(shape, "<http://exampe.com>")).hasLeft();

			assertThat(validate(shape, "'example.org'")).hasRight();
			assertThat(validate(shape, "'example.com'")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateLike() {

			final Shape shape=like("ex.org", true);

			assertThat(validate(shape, "<http://exampe.org/>")).hasRight();
			assertThat(validate(shape, "<http://exampe.com/>")).hasLeft();

			assertThat(validate(shape, "'example.org'")).hasRight();
			assertThat(validate(shape, "'example.com'")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMinLength() {

			final Shape shape=minLength(3);

			assertThat(validate(shape, "100")).hasRight();
			assertThat(validate(shape, "99")).hasLeft();

			assertThat(validate(shape, "'100'")).hasRight();
			assertThat(validate(shape, "'99'")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}

		@Test void testValidateMaxLength() {

			final Shape shape=maxLength(2);

			assertThat(validate(shape, "99")).hasRight();
			assertThat(validate(shape, "100")).hasLeft();

			assertThat(validate(shape, "'99'")).hasRight();
			assertThat(validate(shape, "'100'")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		}


		@Test void testValidateMinCount() {

			final Shape shape=minCount(2);

			assertThat(validate(shape, "1, 2, 3")).hasRight();
			assertThat(validate(shape, "1")).hasLeft();

		}

		@Test void testValidateMaxCount() {

			final Shape shape=maxCount(2);

			assertThat(validate(shape, "1, 2")).hasRight();
			assertThat(validate(shape, "1, 2, 3")).hasLeft();

		}

		@Test void testValidateAll() {

			final Shape shape=all(x, y);

			assertThat(validate(shape, "<x>, <y>, <z>")).hasRight();
			assertThat(validate(shape, "<x>")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		}

		@Test void testValidateAny() {

			final Shape shape=any(x, y);

			assertThat(validate(shape, "<x>")).hasRight();
			assertThat(validate(shape, "<z>")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		}

	}

}
