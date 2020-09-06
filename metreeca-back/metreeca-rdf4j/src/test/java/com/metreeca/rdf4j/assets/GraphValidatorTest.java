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

package com.metreeca.rdf4j.assets;

import com.metreeca.core.*;
import com.metreeca.json.Shape;
import com.metreeca.rdf.Values;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XSD;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.core.EitherAssert.assertThat;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Clazz.clazz;
import static com.metreeca.json.shapes.Datatype.datatype;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.In.in;
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
import static com.metreeca.rdf.Values.inverse;
import static com.metreeca.rdf.Values.literal;
import static com.metreeca.rdf.ValuesTest.*;
import static com.metreeca.rdf.formats.RDFFormat.rdf;
import static com.metreeca.rdf4j.assets.GraphTest.exec;
import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;


final class GraphValidatorTest {

	private static final IRI x=item("x");
	private static final IRI y=item("y");
	private static final IRI z=item("z");


	private Collection<Statement> model(final String... model) {
		return model.length == 0 ? emptySet() : decode("<app:/> rdf:value "
				+stream(model).collect(joining(" . ", "", " . "))
		);
	}


	private Either<MessageException, Request> validate(final Shape shape, final String... model) {
		return validate(shape, model(model));
	}

	private Either<MessageException, Request> validate(final Shape shape, final Collection<Statement> model) {
		return new GraphValidator().validate(new Request()
				.body(rdf(), model)
				.shape(field(RDF.VALUE, shape))
		);
	}


	//// Validation
	// //////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testValidateShapeEnvelope() {
		exec(() -> {

			final Shape shape=all(x);

			assertThat(validate(shape, "<x>")).hasRight();
			assertThat(validate(shape, "<x>; rdf:rest rdf:nil")).hasLeft();

		});
	}

	@Test void testValidateDirectEdgeFields() {
		exec(() -> {

			final Shape shape=field(RDF.VALUE, all(y));

			assertThat(validate(shape, "<x>", "<x> rdf:value <y>")).hasRight();
			assertThat(validate(shape, "<x>")).hasLeft();

		});
	}

	@Test void testValidateInverseEdgeFields() {
		exec(() -> {

			final Shape shape=field(inverse(RDF.VALUE), all(y));

			assertThat(validate(shape, "<x>", "<y> rdf:value <x>")).hasRight();
			assertThat(validate(shape, "<x>")).hasLeft();

		});
	}


	//// Constraints ///////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testValidateMinCount() {
		exec(() -> {

			final Shape shape=minCount(2);

			assertThat(validate(shape, "1, 2, 3")).hasRight();
			assertThat(validate(shape, "1")).hasLeft();

		});
	}

	@Test void testValidateMaxCount() {
		exec(() -> {

			final Shape shape=maxCount(2);

			assertThat(validate(shape, "1, 2")).hasRight();
			assertThat(validate(shape, "1, 2, 3")).hasLeft();

		});
	}

	@Test void testValidateIn() {
		exec(() -> {

			final Shape shape=in(x, y);

			assertThat(validate(shape, "<x>, <y>")).hasRight();
			assertThat(validate(shape, "<x>, <y>, <z>")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		});
	}

	@Test void testValidateAll() {
		exec(() -> {

			final Shape shape=all(x, y);

			assertThat(validate(shape, "<x>, <y>, <z>")).hasRight();
			assertThat(validate(shape, "<x>")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		});
	}

	@Test void testValidateAny() {
		exec(() -> {

			final Shape shape=any(x, y);

			assertThat(validate(shape, "<x>")).hasRight();
			assertThat(validate(shape, "<z>")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		});
	}


	@Test void testValidateDatatype() {
		exec(() -> {

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

		});
	}

	@Test void testValidateClazz() {
		exec(GraphTest.model(small()), () -> {

			final Shape shape=and(clazz(term("Employee")), field(RDF.TYPE));

			// validate using type info retrieved from model

			assertThat(validate(shape, "<employees/9999>", "<employees/9999> a :Employee")).hasRight();
			assertThat(validate(shape, "<offices/9999>")).hasLeft();

			// validate using type info retrieved from graph

			assertThat(validate(shape, "<employees/1370>")).hasRight();
			assertThat(validate(shape, "<offices/1>")).hasLeft();

		});
	}


	@Test void testValidateMinExclusive() {
		exec(() -> {

			final Shape shape=minExclusive(literal(1));

			assertThat(validate(shape, "2")).hasRight();
			assertThat(validate(shape, "1")).hasLeft();
			assertThat(validate(shape, "0")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		});
	}

	@Test void testValidateMaxExclusive() {
		exec(() -> {

			final Shape shape=maxExclusive(literal(10));

			assertThat(validate(shape, "2")).hasRight();
			assertThat(validate(shape, "10")).hasLeft();
			assertThat(validate(shape, "100")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		});
	}

	@Test void testValidateMinInclusive() {
		exec(() -> {

			final Shape shape=minInclusive(literal(1));

			assertThat(validate(shape, "2")).hasRight();
			assertThat(validate(shape, "1")).hasRight();
			assertThat(validate(shape, "0")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		});
	}

	@Test void testValidateMaxInclusive() {
		exec(() -> {

			final Shape shape=maxInclusive(literal(10));

			assertThat(validate(shape, "2")).hasRight();
			assertThat(validate(shape, "10")).hasRight();
			assertThat(validate(shape, "100")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		});
	}


	@Test void testValidatePattern() {
		exec(() -> {

			final Shape shape=pattern(".*\\.org");

			assertThat(validate(shape, "<http://exampe.org>")).hasRight();
			assertThat(validate(shape, "<http://exampe.com>")).hasLeft();

			assertThat(validate(shape, "'example.org'")).hasRight();
			assertThat(validate(shape, "'example.com'")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		});
	}

	@Test void testValidateLike() {
		exec(() -> {

			final Shape shape=like("ex.org", true);

			assertThat(validate(shape, "<http://exampe.org/>")).hasRight();
			assertThat(validate(shape, "<http://exampe.com/>")).hasLeft();

			assertThat(validate(shape, "'example.org'")).hasRight();
			assertThat(validate(shape, "'example.com'")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		});
	}

	@Test void testValidateMinLength() {
		exec(() -> {

			final Shape shape=minLength(3);

			assertThat(validate(shape, "100")).hasRight();
			assertThat(validate(shape, "99")).hasLeft();

			assertThat(validate(shape, "'100'")).hasRight();
			assertThat(validate(shape, "'99'")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		});
	}

	@Test void testValidateMaxLength() {
		exec(() -> {

			final Shape shape=maxLength(2);

			assertThat(validate(shape, "99")).hasRight();
			assertThat(validate(shape, "100")).hasLeft();

			assertThat(validate(shape, "'99'")).hasRight();
			assertThat(validate(shape, "'100'")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasRight();

		});
	}


	@Test void testValidateField() {
		exec(() -> {

			final Shape shape=minCount(1);

			assertThat(validate(shape, "<x>, <z>")).hasRight();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		});

	}


	@Test void testValidateConjunction() {
		exec(() -> {

			final Shape shape=and(any(x), any(y));

			assertThat(validate(shape, "<x>, <y>, <z>")).hasRight();
			assertThat(validate(shape, "<x>, <z>")).hasLeft();

			assertThat(validate(shape)).as("empty focus").hasLeft();

		});
	}

	@Test void testValidateDisjunction() {
		exec(() -> {

			final Shape shape=or(all(x, y), all(x, z));

			assertThat(validate(shape, "<x>, <y>, <z>")).hasRight();
			assertThat(validate(shape, "<y>, <z>")).hasLeft();

		});
	}

}
