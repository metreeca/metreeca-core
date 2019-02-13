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

package com.metreeca.rest.engines;

import com.metreeca.form.*;
import com.metreeca.form.shapes.Field;
import com.metreeca.form.truths.ModelAssert;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.function.Supplier;

import static com.metreeca.form.FocusAssert.assertThat;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.In.in;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.MaxLength.maxLength;
import static com.metreeca.form.shapes.MinCount.minCount;
import static com.metreeca.form.shapes.MinExclusive.minExclusive;
import static com.metreeca.form.shapes.MinInclusive.minInclusive;
import static com.metreeca.form.shapes.MinLength.minLength;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.things.Values.integer;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.form.things.ValuesTest.sandbox;
import static com.metreeca.tray.rdf.GraphTest.graph;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Arrays.stream;
import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toList;


final class GraphValidatorTest {

	private static final IRI x=item("x");
	private static final IRI y=item("y");
	private static final IRI z=item("z");


	private final Supplier<RepositoryConnection> sandbox=sandbox();


	private Collection<Statement> model(final String... model) {
		return model.length == 0 ? emptySet() : decode("rdf:nil rdf:value "
				+stream(model).collect(joining(" . ", "", " . "))
		);
	}


	private Focus validate(final Shape shape, final String... model) {
		return validate(shape, model(model));
	}

	private Focus validate(final Shape shape, final Collection<Statement> model) {
		return new GraphValidator().validate(RDF.NIL, field(RDF.VALUE, shape), model);
	}


	//// Validation ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGenerateTraceNodes() {
		new Tray().exec(() -> {

			final Shape shape=maxInclusive(literal(10));

			final Focus report=validate(shape, "1, 100");

			assertThat(report.assess(Issue.Level.Error))
					.as("report severity level")
					.isTrue();

			final Collection<Frame> frames=report.getFrames().stream()
					.flatMap(frame -> frame.getFields().get(RDF.VALUE).getFrames().stream())
					.collect(toList());

			assertThat(frames.stream()
					.flatMap(frame -> frame.getIssues().stream())
					.anyMatch(issue -> issue.getShape().equals(shape)))
					.as("reference failed shape")
					.isTrue();

			assertThat(frames.stream()
					.map(Frame::getValue)
					.anyMatch(value -> value.equals(literal(integer(100)))))
					.as("reference offending values")
					.isTrue();

		});
	}


	@Test void testValidateDirectEdgeFields() {
		new Tray().exec(() -> {

			final Shape shape=field(RDF.VALUE, all(y));

			assertThat(validate(shape, "<x>", "<x> rdf:value <y>")).isValid();
			assertThat(validate(shape, "<x>")).isNotValid();

		});
	}

	@Test void testValidateInverseEdgeFields() {
		new Tray().exec(() -> {

			final Shape shape=field(inverse(RDF.VALUE), all(y));

			assertThat(validate(shape, "<x>", "<y> rdf:value <x>")).isValid();
			assertThat(validate(shape, "<x>")).isNotValid();

		});
	}


	//// Outlining /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testOutlineClasses() {
		new Tray().exec(() -> {

			final Shape shape=clazz(RDFS.RESOURCE);

			final Collection<Statement> model=model("<x>", "<x> a rdfs:Resource");

			final Focus focus=validate(shape, model);

			assertThat(focus).isValid();
			ModelAssert.assertThat(focus.outline().collect(toList())).isIsomorphicTo(model);

		});
	}

	@Test void testOutlineDirectEdgeFields() {
		new Tray().exec(() -> {

			final Field shape=field(RDF.VALUE, any(RDF.NIL));

			final Collection<Statement> model=model("<x>", " <x> rdf:value rdf:nil");

			final Focus focus=validate(shape, model);

			assertThat(focus).isValid();
			ModelAssert.assertThat(focus.outline().collect(toList())).isIsomorphicTo(model);

		});
	}

	@Test void testOutlineInverseEdgeFields() {
		new Tray().exec(() -> {

			final Field shape=field(inverse(RDF.VALUE), any(RDF.NIL));

			final Collection<Statement> model=model("<x>", " rdf:nil rdf:value <x>");

			final Focus focus=validate(shape, model);

			assertThat(focus).isValid();
			ModelAssert.assertThat(focus.outline().collect(toList())).isIsomorphicTo(model);

		});
	}

	@Test void testOutlineMultipleObjects() {
		new Tray().exec(() -> {

			final Field shape=field(RDF.VALUE);

			final Collection<Statement> model=model("<x>, <y>",
					"<x> rdf:value rdf:first, rdf:rest",
					"<y> rdf:value rdf:first, rdf:rest"
			);

			final Focus focus=validate(shape, model);

			assertThat(focus).isValid();
			ModelAssert.assertThat(focus.outline().collect(toList())).isIsomorphicTo(model);

		});
	}

	@Test void testOutlineMultipleSources() {
		new Tray().exec(() -> {

			final Field shape=field(inverse(RDF.VALUE));

			final Collection<Statement> model=model("<x>, <y>",
					"rdf:first rdf:value <x>",
					"rdf:rest rdf:value <x>",
					"rdf:first rdf:value <y>",
					"rdf:rest rdf:value <y>"
			);

			final Focus focus=validate(shape, model);

			assertThat(focus).isValid();
			ModelAssert.assertThat(focus.outline().collect(toList())).isIsomorphicTo(model);

		});
	}

	@Test void testOutlineMultipleDirectEdges() {
		new Tray().exec(() -> {

			final Shape shape=and(
					field(RDF.FIRST, and()),
					field(RDF.REST, and())
			);

			final Collection<Statement> model=model("<x>, <y>",
					"<x> rdf:first rdf:value; rdf:rest rdf:value",
					"<y> rdf:first rdf:value; rdf:rest rdf:value"
			);

			final Focus focus=validate(shape, model);

			assertThat(focus).isValid();
			ModelAssert.assertThat(focus.outline().collect(toList())).isIsomorphicTo(model);

		});
	}

	@Test void testOutlineMultipleInverseEdges() {
		new Tray().exec(() -> {

			final Shape shape=and(
					field(inverse(RDF.FIRST)),
					field(inverse(RDF.REST))
			);

			final Collection<Statement> model=model("<x>, <y>",
					"rdf:value rdf:first <x>",
					"rdf:value rdf:rest <x>",
					"rdf:value rdf:first <y>",
					"rdf:value rdf:rest <y>"
			);

			final Focus focus=validate(shape, model);

			assertThat(focus).isValid();
			ModelAssert.assertThat(focus.outline().collect(toList())).isIsomorphicTo(model);

		});
	}

	@Test void testOutlineMultipleDirectEdgeValuePairs() {
		new Tray().exec(() -> {

			final Shape shape=and(
					field(inverse(RDF.FIRST)),
					field(inverse(RDF.REST))
			);

			final Collection<Statement> model=model("rdf:first, rdf:rest",
					"rdf:first rdf:first rdf:first, rdf:rest; rdf:rest rdf:first, rdf:rest",
					"rdf:rest rdf:first rdf:first, rdf:rest; rdf:rest rdf:first, rdf:rest"
			);

			final Focus focus=validate(shape, model);

			assertThat(focus).isValid();
			ModelAssert.assertThat(focus.outline().collect(toList())).isIsomorphicTo(model);

		});
	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testValidateMinCount() {
		new Tray().exec(() -> {

			final Shape shape=minCount(2);

			assertThat(validate(shape, "1, 2, 3")).isValid();
			assertThat(validate(shape, "1")).isNotValid();

		});
	}

	@Test void testValidateMaxCount() {
		new Tray().exec(() -> {

			final Shape shape=maxCount(2);

			assertThat(validate(shape, "1, 2")).isValid();
			assertThat(validate(shape, "1, 2, 3")).isNotValid();

		});
	}

	@Test void testValidateIn() {
		new Tray().exec(() -> {

			final Shape shape=in(x, y);

			assertThat(validate(shape, "<x>, <y>")).isValid();
			assertThat(validate(shape, "<x>, <y>, <z>")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isValid();

		});
	}

	@Test void testValidateAll() {
		new Tray().exec(() -> {

			final Shape shape=all(x, y);

			assertThat(validate(shape, "<x>, <y>, <z>")).isValid();
			assertThat(validate(shape, "<x>")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isNotValid();

		});
	}

	@Test void testValidateAny() {
		new Tray().exec(() -> {

			final Shape shape=any(x, y);

			assertThat(validate(shape, "<x>")).isValid();
			assertThat(validate(shape, "<z>")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isNotValid();

		});
	}


	@Test void testValidateDatatype() {
		new Tray().exec(() -> {

			assertThat(validate(datatype(Form.ValueType), "<x>")).isValid();
			assertThat(validate(datatype(Form.ValueType), "_:x")).isValid();
			assertThat(validate(datatype(Form.ValueType), "1")).isValid();

			assertThat(validate(datatype(Form.ResourceType), "<x>")).isValid();
			assertThat(validate(datatype(Form.ResourceType), "_:x")).isValid();
			assertThat(validate(datatype(Form.ResourceType), "1")).isNotValid();

			assertThat(validate(datatype(Form.BNodeType), "_:x")).isValid();
			assertThat(validate(datatype(Form.BNodeType), "1")).isNotValid();

			assertThat(validate(datatype(Form.IRIType), "<x>")).isValid();
			assertThat(validate(datatype(Form.IRIType), "_:x")).isNotValid();

			assertThat(validate(datatype(Form.LiteralType), "'x'")).isValid();
			assertThat(validate(datatype(Form.LiteralType), "1")).isValid();
			assertThat(validate(datatype(Form.LiteralType), "_:x")).isNotValid();

			assertThat(validate(datatype(RDFS.LITERAL), "'x'")).isValid();
			assertThat(validate(datatype(RDFS.LITERAL), "1")).isValid();
			assertThat(validate(datatype(RDFS.LITERAL), "_:x")).isNotValid();

			assertThat(validate(datatype(XMLSchema.STRING), "'text'")).isValid();
			assertThat(validate(datatype(XMLSchema.STRING), "_:x")).isNotValid();

			assertThat(validate(datatype(RDF.LANGSTRING), "'text'@en")).isValid();
			assertThat(validate(datatype(RDF.LANGSTRING), "_:x")).isNotValid();

			assertThat(validate(datatype(XMLSchema.BOOLEAN), "true")).isValid();
			assertThat(validate(datatype(XMLSchema.BOOLEAN), "_:x")).isNotValid();

			assertThat(validate(datatype(Form.IRIType))).as("empty focus").isValid();

		});
	}

	@Test void testValidateClazz() {

		final Shape shape=clazz(RDFS.RESOURCE);

		new Tray()

				.exec(() -> { // validate using type info retrieved from model

					assertThat(validate(shape, "rdf:first", "rdf:first a rdfs:Resource")).isValid();

					assertThat(validate(shape, "rdf:rest")).isNotValid();

				})

				.exec(graph(decode("rdf:first a rdfs:Resource."))) // inject type info into graph

				.exec(() -> { // validate using type info retrieved from graph

					assertThat(validate(shape, "rdf:first")).isValid();

					assertThat(validate(shape, "rdf:rest")).isNotValid();

				});
	}


	@Test void testValidateMinExclusive() {
		new Tray().exec(() -> {

			final Shape shape=minExclusive(literal(1));

			assertThat(validate(shape, "2")).isValid();
			assertThat(validate(shape, "1")).isNotValid();
			assertThat(validate(shape, "0")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isValid();

		});
	}

	@Test void testValidateMaxExclusive() {
		new Tray().exec(() -> {

			final Shape shape=maxExclusive(literal(10));

			assertThat(validate(shape, "2")).isValid();
			assertThat(validate(shape, "10")).isNotValid();
			assertThat(validate(shape, "100")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isValid();

		});
	}

	@Test void testValidateMinInclusive() {
		new Tray().exec(() -> {

			final Shape shape=minInclusive(literal(1));

			assertThat(validate(shape, "2")).isValid();
			assertThat(validate(shape, "1")).isValid();
			assertThat(validate(shape, "0")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isValid();

		});
	}

	@Test void testValidateMaxInclusive() {
		new Tray().exec(() -> {

			final Shape shape=maxInclusive(literal(10));

			assertThat(validate(shape, "2")).isValid();
			assertThat(validate(shape, "10")).isValid();
			assertThat(validate(shape, "100")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isValid();

		});
	}


	@Test void testValidatePattern() {
		new Tray().exec(() -> {

			final Shape shape=pattern(".*\\.org");

			assertThat(validate(shape, "<http://exampe.org>")).isValid();
			assertThat(validate(shape, "<http://exampe.com>")).isNotValid();

			assertThat(validate(shape, "'example.org'")).isValid();
			assertThat(validate(shape, "'example.com'")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isValid();

		});
	}

	@Test void testValidateLike() {
		new Tray().exec(() -> {

			final Shape shape=like("ex.org");

			assertThat(validate(shape, "<http://exampe.org/>")).isValid();
			assertThat(validate(shape, "<http://exampe.com/>")).isNotValid();

			assertThat(validate(shape, "'example.org'")).isValid();
			assertThat(validate(shape, "'example.com'")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isValid();

		});
	}

	@Test void testValidateMinLength() {
		new Tray().exec(() -> {

			final Shape shape=minLength(3);

			assertThat(validate(shape, "100")).isValid();
			assertThat(validate(shape, "99")).isNotValid();

			assertThat(validate(shape, "'100'")).isValid();
			assertThat(validate(shape, "'99'")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isValid();

		});
	}

	@Test void testValidateMaxLength() {
		new Tray().exec(() -> {

			final Shape shape=maxLength(2);

			assertThat(validate(shape, "99")).isValid();
			assertThat(validate(shape, "100")).isNotValid();

			assertThat(validate(shape, "'99'")).isValid();
			assertThat(validate(shape, "'100'")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isValid();

		});
	}


	@Test void testValidateConjunction() {
		new Tray().exec(() -> {

			final Shape shape=and(any(x), any(y));

			assertThat(validate(shape, "<x>, <y>, <z>")).isValid();
			assertThat(validate(shape, "<x>, <z>")).isNotValid();

			assertThat(validate(shape)).as("empty focus").isNotValid();

		});
	}

	@Test void testValidateDisjunction() {
		new Tray().exec(() -> {

			final Shape shape=or(all(x, y), all(x, z));

			assertThat(validate(shape, "<x>, <y>, <z>")).isValid();
			assertThat(validate(shape, "<y>, <z>")).isNotValid();

		});
	}

}
