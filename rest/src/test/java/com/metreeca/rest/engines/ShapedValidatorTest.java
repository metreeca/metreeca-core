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
import com.metreeca.form.things.ValuesTest;
import com.metreeca.form.truths.ModelAssert;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.util.function.Supplier;

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
import static com.metreeca.form.things.Values.*;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.things.ValuesTest.term;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toList;


final class ShapedValidatorTest {

	private static final IRI x=ValuesTest.item("x");
	private static final IRI y=ValuesTest.item("y");
	private static final IRI z=ValuesTest.item("z");


	private final Supplier<RepositoryConnection> sandbox=ValuesTest.sandbox();


	//// Validation ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGenerateTraceNodes() {

		final Shape shape=maxInclusive(literal(10));

		final Focus report=process(shape, literal(1), literal(100));

		assertThat(report.assess(Issue.Level.Error))
				.as("report severity level")
				.isTrue();

		assertThat(report.getFrames().stream()
				.flatMap(frame -> frame.getIssues().stream())
				.anyMatch(issue -> issue.getShape().equals(shape)))
				.as("reference failed shape")
				.isTrue();

		assertThat(report.getFrames().stream()
				.map(Frame::getValue)
				.anyMatch(value -> value.equals(literal(100))))
				.as("reference offending values")
				.isTrue();

	}


	@Test void testValidateDirectEdgeFields() {

		final Shape shape=field(RDF.VALUE, any(RDF.NIL));

		final Focus focus=process(shape, x, y);

		assertThat(focus.assess(Issue.Level.Error)).as("identify invalid field").isTrue();

	}

	@Test void testValidateInverseEdgeFields() {

		final Shape shape=field(inverse(RDF.VALUE), any(RDF.NIL));

		final Focus focus=process(shape, x, y);

		assertThat(focus.assess(Issue.Level.Error)).as("identify invalid field").isTrue();

	}


	//// Outlining /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testOutlineClasses() {

		final Shape shape=clazz(RDFS.RESOURCE);
		final Model model=decode("rdf:first a rdfs:Resource.");

		final Focus focus=process(shape, model, RDF.FIRST);

		assertThat(focus.assess(Issue.Level.Error))
				.as("validated").
				isFalse();

		ModelAssert.assertThat(focus.outline().collect(toList()))
				.as("outline computed")
				.isIsomorphicTo(model);

	}

	@Test void testOutlineDirectEdgeFields() {

		final Shape shape=field(RDF.VALUE, any(RDF.NIL));
		final Model model=decode("<x> rdf:value rdf:nil. <y> rdf:value rdf:nil.");

		final Focus focus=process(shape, model, x, y);

		assertThat(focus.assess(Issue.Level.Error)).as("validated").isFalse();
		ModelAssert.assertThat(focus.outline().collect(toList()))
				.as("outline computed")
				.isIsomorphicTo(model);

	}

	@Test void testOutlineInverseEdgeFields() {

		final Shape shape=field(inverse(RDF.VALUE), any(RDF.NIL));
		final Model model=decode("rdf:nil rdf:value <x>. rdf:nil rdf:value <y>.");

		final Focus focus=process(shape, model, x, y);

		System.out.println(focus);

		assertThat(focus.assess(Issue.Level.Error)).as("validated").isFalse();
		ModelAssert.assertThat(focus.outline().collect(toList()))
				.as("outline computed")
				.isIsomorphicTo(model);

	}

	@Test void testOutlineMultipleObjects() {

		final Shape shape=field(RDF.VALUE, and());
		final Model model=decode("<x> rdf:value rdf:first, rdf:rest. <y> rdf:value rdf:first, rdf:rest.");

		final Focus focus=process(shape, model, x, y);

		assertThat(focus.assess(Issue.Level.Error)).as("validated").isFalse();
		ModelAssert.assertThat(focus.outline().collect(toList()))
				.as("outline computed")
				.isIsomorphicTo(model);

	}

	@Test void testOutlineMultipleSources() {

		final Shape shape=field(inverse(RDF.VALUE), and());
		final Model model=decode(
				"rdf:first rdf:value <x>. rdf:rest rdf:value <x>. rdf:first rdf:value <y>. rdf:rest rdf:value <y>."
		);

		final Focus focus=process(shape, model, x, y);

		assertThat(focus.assess(Issue.Level.Error)).as("validated").isFalse();
		ModelAssert.assertThat(focus.outline().collect(toList())).as("outline computed").isIsomorphicTo(model);

	}

	@Test void testOutlineMultipleDirectEdges() {

		final Shape shape=and(
				field(RDF.FIRST, and()),
				field(RDF.REST, and())
		);

		final Model model=decode(
				"<x> rdf:first rdf:value; rdf:rest rdf:value . <y> rdf:first rdf:value; rdf:rest rdf:value .");

		final Focus focus=process(shape, model, x, y);

		assertThat(focus.assess(Issue.Level.Error)).as("validated").isFalse();
		ModelAssert.assertThat(focus.outline().collect(toList()))
				.as("outline computed")
				.isIsomorphicTo(model);

	}

	@Test void testOutlineMultipleInverseEdges() {

		final Shape shape=and(
				field(inverse(RDF.FIRST), and()),
				field(inverse(RDF.REST), and())
		);

		final Model model=decode(
				"rdf:value rdf:first <x>. rdf:value rdf:rest <x>. rdf:value rdf:first <y>. rdf:value rdf:rest <y>.");

		final Focus focus=process(shape, model, x, y);

		assertThat(focus.assess(Issue.Level.Error)).as("validated").isFalse();
		ModelAssert.assertThat(focus.outline().collect(toList()))
				.as("outline computed")
				.isIsomorphicTo(model);

	}

	@Test void testOutlineMultipleDirectEdgeValuePairs() {

		final Shape shape=and(
				field(inverse(RDF.FIRST), and()),
				field(inverse(RDF.REST), and())
		);

		final Model model=decode(""
				+"rdf:first rdf:first rdf:first, rdf:rest; rdf:rest rdf:first, rdf:rest ."
				+"rdf:rest rdf:first rdf:first, rdf:rest; rdf:rest rdf:first, rdf:rest ."
		);

		final Focus focus=process(shape, model, RDF.FIRST, RDF.REST);

		assertThat(focus.assess(Issue.Level.Error)).as("validated").isFalse();
		ModelAssert.assertThat(focus.outline().collect(toList()))
				.as("outline computed")
				.isIsomorphicTo(model);

	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testValidateMinCount() {

		final Shape shape=minCount(2);

		assertThat(validate(shape, x, y, z)).as("pass").isTrue();
		assertThat(validate(shape, x)).as("fail").isFalse();

	}

	@Test void testValidateMaxCount() {

		final Shape shape=maxCount(2);

		assertThat(validate(shape, x, y)).as("pass").isTrue();
		assertThat(validate(shape, x, y, z)).as("fail").isFalse();

	}

	@Test void testValidateIn() {

		final Shape shape=in(x, y);

		assertThat(validate(shape, x, y)).as("pass").isTrue();
		assertThat(validate(shape, x, z)).as("fail").isFalse();
		assertThat(validate(shape)).as("pass / empty focus").isTrue();

	}

	@Test void testValidateAll() {

		final Shape shape=all(x, y);

		assertThat(validate(shape, x, y, z)).as("pass").isTrue();
		assertThat(validate(shape, x)).as("fail").isFalse();
		assertThat(validate(shape)).as("fail / empty focus").isFalse();

	}

	@Test void testValidateAny() {

		final Shape shape=any(x, y);

		assertThat(validate(shape, x)).as("pass").isTrue();
		assertThat(validate(shape, z)).as("fail").isFalse();
		assertThat(validate(shape)).as("fail / empty focus").isFalse();

	}


	@Test void testValidateDatatype() {

		assertThat(validate(datatype(Form.BNodeType))).as("pass / empty").isTrue();
		assertThat(validate(datatype(Form.BNodeType), bnode())).as("pass / blank").isTrue();
		assertThat(validate(datatype(Form.IRIType), term("iri"))).as("pass / iri").isTrue();
		assertThat(validate(datatype(XMLSchema.STRING), literal("text"))).as("pass / plain literal").isTrue();
		assertThat(validate(datatype(RDF.LANGSTRING), literal("text", "en"))).as("pass / tagged literal").isTrue();
		assertThat(validate(datatype(XMLSchema.BOOLEAN), literal(true))).as("pass / typed literal").isTrue();

		assertThat(validate(datatype(Form.IRIType), bnode())).as("fail").isFalse();

		assertThat(validate(datatype(Form.ResourceType), bnode())).as("pass / generic resource").isTrue();
		assertThat(validate(datatype(Form.ResourceType), literal(true))).as("fail / generic resource").isFalse();

		assertThat(validate(datatype(RDFS.LITERAL), literal(true))).as("pass / generic literal").isTrue();
		assertThat(validate(datatype(RDFS.LITERAL), bnode())).as("fail / generic literal").isFalse();

	}

	@Test void testValidateClazz() {

		final Shape shape=clazz(z);
		final Model model=decode("<x> a <y>. <y> rdfs:subClassOf <z>.");

		assertThat(validate(shape, model, x)).as("pass").isTrue();
		assertThat(validate(shape, model, y)).as("fail").isFalse();

	}


	@Test void testValidateMinExclusive() {

		final Shape shape=minExclusive(literal(1));

		assertThat(validate(shape, literal(2))).as("integer / pass").isTrue();
		assertThat(validate(shape, literal(1))).as("integer / fail / equal").isFalse();
		assertThat(validate(shape, literal(0))).as("integer / fail").isFalse();

	}

	@Test void testValidateMaxExclusive() {

		final Shape shape=maxExclusive(literal(10));

		assertThat(validate(shape, literal(2))).as("integer / pass").isTrue();
		assertThat(validate(shape, literal(10))).as("integer / fail / equal").isFalse();
		assertThat(validate(shape, literal(100))).as("integer / fail").isFalse();

	}

	@Test void testValidateMinInclusive() {

		final Shape shape=minInclusive(literal(1));

		assertThat(validate(shape, literal(2))).as("integer / pass").isTrue();
		assertThat(validate(shape, literal(1))).as("integer / pass / equal").isTrue();
		assertThat(validate(shape, literal(0))).as("integer / fail").isFalse();

	}

	@Test void testValidateMaxInclusive() {

		final Shape shape=maxInclusive(literal(10));

		assertThat(validate(shape, literal(2))).as("integer / pass").isTrue();
		assertThat(validate(shape, literal(10))).as("integer / pass / equal").isTrue();
		assertThat(validate(shape, literal(100))).as("integer / fail").isFalse();

	}


	@Test void testValidatePattern() {

		final Shape shape=pattern(".*\\.org");

		assertThat(validate(shape, iri("http://exampe.org"))).as("iri / pass").isTrue();
		assertThat(validate(shape, iri("http://exampe.com"))).as("iri / fail").isFalse();

		assertThat(validate(shape, literal("example.org"))).as("string / pass").isTrue();
		assertThat(validate(shape, literal("example.com"))).as("string / fail").isFalse();

	}

	@Test void testValidateLike() {

		final Shape shape=like("ex.org");

		assertThat(validate(shape, iri("http://exampe.org/"))).as("iri / pass").isTrue();
		assertThat(validate(shape, iri("http://exampe.com/"))).as("iri / fail").isFalse();

		assertThat(validate(shape, literal("example.org"))).as("string / pass").isTrue();
		assertThat(validate(shape, literal("example.com"))).as("string / fail").isFalse();

	}

	@Test void testValidateMinLength() {

		final Shape shape=minLength(3);

		assertThat(validate(shape, literal(100))).as("number / pass").isTrue();
		assertThat(validate(shape, literal(99))).as("number / fail").isFalse();

		assertThat(validate(shape, literal("100"))).as("string / pass").isTrue();
		assertThat(validate(shape, literal("99"))).as("string / fail").isFalse();

	}

	@Test void testValidateMaxLength() {

		final Shape shape=maxLength(2);

		assertThat(validate(shape, literal(99))).as("number / pass").isTrue();
		assertThat(validate(shape, literal(100))).as("number / fail").isFalse();

		assertThat(validate(shape, literal("99"))).as("string / pass").isTrue();
		assertThat(validate(shape, literal("100"))).as("string / fail").isFalse();

	}


	@Test void testValidateConjunction() {

		final Shape shape=and(any(literal(1)), any(literal(2)));

		assertThat(validate(shape, literal(1), literal(2), literal(3))).as("pass").isTrue();
		assertThat(validate(shape, literal(1), literal(3))).as("fail").isFalse();

	}

	@Test void testValidateDisjunction() {

		final Shape shape=or(all(literal(1), literal(2)), all(literal(1), literal(3)));

		assertThat(validate(shape, literal(3), literal(2), literal(1))).as("pass").isTrue();
		assertThat(validate(shape, literal(3), literal(2))).as("fail").isFalse();

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean validate(final Shape shape, final Value... focus) {
		return validate(shape, emptySet(), focus);
	}

	private boolean validate(final Shape shape, final Iterable<Statement> statements, final Value... focus) {
		return !process(shape, statements, focus).assess(Issue.Level.Error);
	}


	private Focus process(final Shape shape, final Value... focus) {
		return process(shape, emptySet(), focus);
	}

	private Focus process(final Shape shape, final Iterable<Statement> statements, final Value... focus) {
		try (final RepositoryConnection connection=sandbox.get()) {
			connection.add(statements);
			final ShapedValidator validator=new ShapedValidator();
			return validator.validate(connection, shape, focus);
		}
	}

}
