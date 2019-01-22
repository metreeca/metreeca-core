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

package com.metreeca.form.engines;

import com.metreeca.form.Issue;
import com.metreeca.form.Report;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Sets;
import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.function.Supplier;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.Trait.trait;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;


final class SPARQLWriterTest {

	private static final IRI x=ValuesTest.item("x");
	private static final IRI y=ValuesTest.item("y");
	private static final IRI z=ValuesTest.item("z");


	private final Supplier<RepositoryConnection> sandbox=ValuesTest.sandbox();


	//// Validation ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testGenerateTraceNodes() {

		final Shape shape=maxInclusive(Values.literal(10));

		final Report report=process(shape, Values.literal(1), Values.literal(100));
		final Collection<Issue> issues=report.getIssues();

		assertThat(report.assess(Issue.Level.Error)).as("report severity level").isTrue();

		assertThat(issues.stream().anyMatch(issue1 -> issue1.getShape().equals(shape))).as("reference failed shape").isTrue();

		assertThat(Sets.set(Values.literal(100))).as("reference offending values").isEqualTo(issues.stream().flatMap(issue -> issue.getValues().stream()).collect(toSet()));

	}


	@Test void testValidateDirectEdgeTraits() {

		final Shape shape=trait(Step.step(RDF.VALUE), any(RDF.NIL));

		final Report report=process(shape, x, y);

		assertThat(report.assess(Issue.Level.Error)).as("identify invalid trait").isTrue();

	}

	@Test void testValidateInverseEdgeTraits() {

		final Shape shape=trait(Step.step(RDF.VALUE, true), any(RDF.NIL));

		final Report report=process(shape, x, y);

		assertThat(report.assess(Issue.Level.Error)).as("identify invalid trait").isTrue();

	}


	//// Outlining /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testOutlineClasses() {

		final Shape shape=clazz(RDFS.RESOURCE);
		final Model model=ValuesTest.decode("rdf:first a rdfs:Resource.");

		final Report report=process(shape, model, RDF.FIRST);

		assertThat(report.assess(Issue.Level.Error)).as("validated").isFalse();
		assertThat(Models.isomorphic(model, report.outline())).as("outline computed").isTrue();

	}

	@Test void testOutlineDirectEdgeTraits() {

		final Shape shape=trait(Step.step(RDF.VALUE), any(RDF.NIL));
		final Model model=ValuesTest.decode("<x> rdf:value rdf:nil. <y> rdf:value rdf:nil.");

		final Report report=process(shape, model, x, y);

		assertThat(report.assess(Issue.Level.Error)).as("validated").isFalse();
		assertThat(Models.isomorphic(model, report.outline())).as("outline computed").isTrue();

	}

	@Test void testOutlineInverseEdgeTraits() {

		final Shape shape=trait(Step.step(RDF.VALUE, true), any(RDF.NIL));
		final Model model=ValuesTest.decode("rdf:nil rdf:value <x>. rdf:nil rdf:value <y>.");

		final Report report=process(shape, model, x, y);

		System.out.println(report);

		assertThat(report.assess(Issue.Level.Error)).as("validated").isFalse();
		assertThat(Models.isomorphic(model, report.outline())).as("outline computed").isTrue();

	}

	@Test void testOutlineMultipleObjects() {

		final Shape shape=trait(Step.step(RDF.VALUE), and());
		final Model model=ValuesTest.decode("<x> rdf:value rdf:first, rdf:rest. <y> rdf:value rdf:first, rdf:rest.");

		final Report report=process(shape, model, x, y);

		assertThat(report.assess(Issue.Level.Error)).as("validated").isFalse();
		assertThat(Models.isomorphic(model, report.outline())).as("outline computed").isTrue();

	}

	@Test void testOutlineMultipleSources() {

		final Shape shape=trait(Step.step(RDF.VALUE, true), and());
		final Model model=ValuesTest.decode(
				"rdf:first rdf:value <x>. rdf:rest rdf:value <x>. rdf:first rdf:value <y>. rdf:rest rdf:value <y>."
		);

		final Report report=process(shape, model, x, y);

		assertThat(report.assess(Issue.Level.Error)).as("validated").isFalse();
		assertThat(Models.isomorphic(model, report.outline())).as("outline computed").isTrue();

	}

	@Test void testOutlineMultipleDirectEdges() {

		final Shape shape=and(
				trait(Step.step(RDF.FIRST), and()),
				trait(Step.step(RDF.REST), and())
		);

		final Model model=ValuesTest.decode(
				"<x> rdf:first rdf:value; rdf:rest rdf:value . <y> rdf:first rdf:value; rdf:rest rdf:value .");

		final Report report=process(shape, model, x, y);

		assertThat(report.assess(Issue.Level.Error)).as("validated").isFalse();
		assertThat(Models.isomorphic(model, report.outline())).as("outline computed").isTrue();

	}

	@Test void testOutlineMultipleInverseEdges() {

		final Shape shape=and(
				trait(Step.step(RDF.FIRST, true), and()),
				trait(Step.step(RDF.REST, true), and())
		);

		final Model model=ValuesTest.decode(
				"rdf:value rdf:first <x>. rdf:value rdf:rest <x>. rdf:value rdf:first <y>. rdf:value rdf:rest <y>.");

		final Report report=process(shape, model, x, y);

		assertThat(report.assess(Issue.Level.Error)).as("validated").isFalse();
		assertThat(Models.isomorphic(model, report.outline())).as("outline computed").isTrue();

	}

	@Test void testOutlineMultipleDirectEdgeValuePairs() {

		final Shape shape=and(
				trait(Step.step(RDF.FIRST, true), and()),
				trait(Step.step(RDF.REST, true), and())
		);

		final Model model=ValuesTest.decode(""
				+"rdf:first rdf:first rdf:first, rdf:rest; rdf:rest rdf:first, rdf:rest ."
				+"rdf:rest rdf:first rdf:first, rdf:rest; rdf:rest rdf:first, rdf:rest ."
		);

		final Report report=process(shape, model, RDF.FIRST, RDF.REST);

		assertThat(report.assess(Issue.Level.Error)).as("validated").isFalse();
		assertThat(Models.isomorphic(model, report.outline())).as("outline computed").isTrue();

	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testValidateMinCount() {

		final Shape shape=MinCount.minCount(2);

		assertThat(validate(shape, x, y, z)).as("pass").isTrue();
		assertThat(validate(shape, x)).as("fail").isFalse();

	}

	@Test void testValidateMaxCount() {

		final Shape shape=maxCount(2);

		assertThat(validate(shape, x, y)).as("pass").isTrue();
		assertThat(validate(shape, x, y, z)).as("fail").isFalse();

	}

	@Test void testValidateIn() {

		final Shape shape=In.in(x, y);

		assertThat(validate(shape, x, y)).as("pass").isTrue();
		assertThat(validate(shape, x, z)).as("fail").isFalse();
		assertThat(validate(shape)).as("pass / empty focus").isTrue();

	}

	@Test void testValidateAll() {

		final Shape shape=All.all(x, y);

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


	@Test void testValidateDataype() {

		assertThat(validate(datatype(Values.BNodeType))).as("pass / empty").isTrue();
		assertThat(validate(datatype(Values.BNodeType), Values.bnode())).as("pass / blank").isTrue();
		assertThat(validate(datatype(Values.IRIType), ValuesTest.term("iri"))).as("pass / iri").isTrue();
		assertThat(validate(datatype(XMLSchema.STRING), Values.literal("text"))).as("pass / plain literal").isTrue();
		assertThat(validate(datatype(RDF.LANGSTRING), Values.literal("text", "en"))).as("pass / tagged literal").isTrue();
		assertThat(validate(datatype(XMLSchema.BOOLEAN), Values.literal(true))).as("pass / typed literal").isTrue();

		assertThat(validate(datatype(Values.IRIType), Values.bnode())).as("fail").isFalse();

		assertThat(validate(datatype(Values.ResoureType), Values.bnode())).as("pass / generic resource").isTrue();
		assertThat(validate(datatype(Values.ResoureType), Values.literal(true))).as("fail / generic resource").isFalse();

		assertThat(validate(datatype(RDFS.LITERAL), Values.literal(true))).as("pass / generic literal").isTrue();
		assertThat(validate(datatype(RDFS.LITERAL), Values.bnode())).as("fail / generic literal").isFalse();

	}

	@Test void testValidateClazz() {

		final Shape shape=clazz(z);
		final Model model=ValuesTest.decode("<x> a <y>. <y> rdfs:subClassOf <z>.");

		assertThat(validate(shape, model, x)).as("pass").isTrue();
		assertThat(validate(shape, model, y)).as("fail").isFalse();

	}


	@Test void testValidateMinExclusive() {

		final Shape shape=MinExclusive.minExclusive(Values.literal(1));

		assertThat(validate(shape, Values.literal(2))).as("integer / pass").isTrue();
		assertThat(validate(shape, Values.literal(1))).as("integer / fail / equal").isFalse();
		assertThat(validate(shape, Values.literal(0))).as("integer / fail").isFalse();

	}

	@Test void testValidateMaxExclusive() {

		final Shape shape=maxExclusive(Values.literal(10));

		assertThat(validate(shape, Values.literal(2))).as("integer / pass").isTrue();
		assertThat(validate(shape, Values.literal(10))).as("integer / fail / equal").isFalse();
		assertThat(validate(shape, Values.literal(100))).as("integer / fail").isFalse();

	}

	@Test void testValidateMinInclusive() {

		final Shape shape=MinInclusive.minInclusive(Values.literal(1));

		assertThat(validate(shape, Values.literal(2))).as("integer / pass").isTrue();
		assertThat(validate(shape, Values.literal(1))).as("integer / pass / equal").isTrue();
		assertThat(validate(shape, Values.literal(0))).as("integer / fail").isFalse();

	}

	@Test void testValidateMaxInclusive() {

		final Shape shape=maxInclusive(Values.literal(10));

		assertThat(validate(shape, Values.literal(2))).as("integer / pass").isTrue();
		assertThat(validate(shape, Values.literal(10))).as("integer / pass / equal").isTrue();
		assertThat(validate(shape, Values.literal(100))).as("integer / fail").isFalse();

	}


	@Test void testValidatePattern() {

		final Shape shape=Pattern.pattern(".*\\.org");

		assertThat(validate(shape, Values.iri("http://exampe.org"))).as("iri / pass").isTrue();
		assertThat(validate(shape, Values.iri("http://exampe.com"))).as("iri / fail").isFalse();

		assertThat(validate(shape, Values.literal("example.org"))).as("string / pass").isTrue();
		assertThat(validate(shape, Values.literal("example.com"))).as("string / fail").isFalse();

	}

	@Test void testValidateLike() {

		final Shape shape=like("ex.org");

		assertThat(validate(shape, Values.iri("http://exampe.org/"))).as("iri / pass").isTrue();
		assertThat(validate(shape, Values.iri("http://exampe.com/"))).as("iri / fail").isFalse();

		assertThat(validate(shape, Values.literal("example.org"))).as("string / pass").isTrue();
		assertThat(validate(shape, Values.literal("example.com"))).as("string / fail").isFalse();

	}

	@Test void testValidateMinLength() {

		final Shape shape=MinLength.minLength(3);

		assertThat(validate(shape, Values.literal(100))).as("number / pass").isTrue();
		assertThat(validate(shape, Values.literal(99))).as("number / fail").isFalse();

		assertThat(validate(shape, Values.literal("100"))).as("string / pass").isTrue();
		assertThat(validate(shape, Values.literal("99"))).as("string / fail").isFalse();

	}

	@Test void testValidateMaxLength() {

		final Shape shape=MaxLength.maxLength(2);

		assertThat(validate(shape, Values.literal(99))).as("number / pass").isTrue();
		assertThat(validate(shape, Values.literal(100))).as("number / fail").isFalse();

		assertThat(validate(shape, Values.literal("99"))).as("string / pass").isTrue();
		assertThat(validate(shape, Values.literal("100"))).as("string / fail").isFalse();

	}


	@Test void testValidateCustom() {

		final Shape shape=Custom.custom(Issue.Level.Error, "test custom shape",
				"select * { filter not exists { ?this rdf:value rdf:first } }");

		final Model model=ValuesTest.decode(""
				+"<x> rdf:value rdf:first.\n"
				+"<y> rdf:value rdf:first, rdf:rest.\n"
				+"<z> rdf:value rdf:rest. ");

		assertThat(validate(shape, model)).as("pass / empty").isTrue();
		assertThat(validate(shape, model, x)).as("pass / single").isTrue();
		assertThat(validate(shape, model, x, y)).as("pass / multiple").isTrue();

		assertThat(validate(shape, model, z)).as("fail / single").isFalse();
		assertThat(validate(shape, model, x, y, z)).as("fail / multiple").isFalse();
	}

	@Test void testValidateCustomWithReportingDetails() {

		final Shape shape=Custom.custom(Issue.Level.Warning, "custom {?type}",
				"select ('shape' as ?type) {}");

		final IRI focus=x;

		final Report report=process(shape, focus);

		assertThat(report.assess(Issue.Level.Warning)).as("reported issue level").isTrue();

		assertThat(Sets.set(focus)).as("reported focus node").isEqualTo(report.getIssues().stream().flatMap(issue -> issue.getValues().stream()).collect(toSet()));

		assertThat((Object)"custom shape").as("populated message template").isEqualTo(report.getIssues().stream().map(Issue::getMessage).findFirst().orElse(null));

	}

	@Test void testValidateCustomOnAggregates() {

		final Shape shape=Custom.custom(Issue.Level.Error, "count={?count}",
				"select ?count { { select (count(?this) as ?count) {} values ?this {} } }");

		final Report report=process(shape, x, y, z);

		assertThat((Object)"count=3").as("custom aggregate validation").isEqualTo(report.getIssues().stream().map(Issue::getMessage).findFirst().orElse(""));

	}


	@Test void testValidateConjunction() {

		final Shape shape=and(any(Values.literal(1)), any(Values.literal(2)));

		assertThat(validate(shape, Values.literal(1), Values.literal(2), Values.literal(3))).as("pass").isTrue();
		assertThat(validate(shape, Values.literal(1), Values.literal(3))).as("fail").isFalse();

	}

	@Test void testValidateDisjunction() {

		final Shape shape=Or.or(All.all(Values.literal(1), Values.literal(2)), All.all(Values.literal(1), Values.literal(3)));

		assertThat(validate(shape, Values.literal(3), Values.literal(2), Values.literal(1))).as("pass").isTrue();
		assertThat(validate(shape, Values.literal(3), Values.literal(2))).as("fail").isFalse();

	}

	@Test void testValidateOption() {

		final Shape shape=Option.option(any(Values.literal(1)), any(Values.literal(2)), any(Values.literal(3)));

		assertThat(validate(shape, Values.literal(1), Values.literal(2))).as("true / pass").isTrue();
		assertThat(validate(shape, Values.literal(1), Values.literal(3))).as("true / fail").isFalse();

		assertThat(validate(shape, Values.literal(3))).as("false / pass").isTrue();
		assertThat(validate(shape, Values.literal(2))).as("false / fail").isFalse();

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private boolean validate(final Shape shape, final Value... focus) {
		return validate(shape, emptySet(), focus);
	}

	private boolean validate(final Shape shape, final Iterable<Statement> statements, final Value... focus) {
		return !process(shape, statements, focus).assess(Issue.Level.Error);
	}


	private Report process(final Shape shape, final Value... focus) {
		return process(shape, emptySet(), focus);
	}

	private Report process(final Shape shape, final Iterable<Statement> statements, final Value... focus) {
		try (final RepositoryConnection connection=sandbox.get()) {
			return new SPARQLWriter(connection).process(shape, statements, focus);
		}
	}

}
