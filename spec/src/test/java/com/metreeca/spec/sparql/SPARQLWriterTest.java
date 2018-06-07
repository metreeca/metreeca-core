/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.spec.sparql;

import com.metreeca.spec.Issue;
import com.metreeca.spec.Report;
import com.metreeca.spec.Shape;
import com.metreeca.spec.shapes.*;
import com.metreeca.spec.things.Values;
import com.metreeca.spec.things.ValuesTest;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.Test;

import java.util.Collection;

import static com.metreeca.spec.shapes.All.all;
import static com.metreeca.spec.shapes.And.and;
import static com.metreeca.spec.shapes.Any.any;
import static com.metreeca.spec.shapes.Clazz.clazz;
import static com.metreeca.spec.shapes.Datatype.datatype;
import static com.metreeca.spec.shapes.In.in;
import static com.metreeca.spec.shapes.Like.like;
import static com.metreeca.spec.shapes.MaxCount.maxCount;
import static com.metreeca.spec.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.spec.shapes.MinExclusive.minExclusive;
import static com.metreeca.spec.shapes.Test.test;
import static com.metreeca.spec.shapes.Trait.trait;
import static com.metreeca.spec.shifts.Step.step;
import static com.metreeca.spec.things.Sets.set;
import static com.metreeca.spec.things.Values.bnode;
import static com.metreeca.spec.things.Values.iri;
import static com.metreeca.spec.things.Values.literal;
import static com.metreeca.spec.things.ValuesTest.parse;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;


public class SPARQLWriterTest {

	private static final IRI x=ValuesTest.item("x");
	private static final IRI y=ValuesTest.item("y");
	private static final IRI z=ValuesTest.item("z");


	//// Validation ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testGenerateTraceNodes() {

		final Shape shape=MaxInclusive.maxInclusive(literal(10));

		final Report report=process(shape, literal(1), literal(100));
		final Collection<Issue> issues=report.getIssues();

		assertTrue("report severity level", report.assess(Issue.Level.Error));

		assertTrue("reference failed shape",
				issues.stream().anyMatch(issue -> issue.getShape().equals(shape)));

		assertEquals("reference offending values", set(literal(100)),
				issues.stream().flatMap(issue -> issue.getValues().stream()).collect(toSet()));

	}


	@Test public void testValidateDirectEdgeTraits() {

		final Shape shape=trait(step(RDF.VALUE), any(RDF.NIL));

		final Report report=process(shape, x, y);

		assertTrue("identify invalid trait", report.assess(Issue.Level.Error));

	}

	@Test public void testValidateInverseEdgeTraits() {

		final Shape shape=trait(step(RDF.VALUE, true), any(RDF.NIL));

		final Report report=process(shape, x, y);

		assertTrue("identify invalid trait", report.assess(Issue.Level.Error));

	}


	//// Outlining /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testOutlineClasses() {

		final Shape shape=clazz(RDFS.RESOURCE);
		final Model model=parse("rdf:first a rdfs:Resource.");

		final Report report=process(shape, model, RDF.FIRST);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineDirectEdgeTraits() {

		final Shape shape=trait(step(RDF.VALUE), any(RDF.NIL));
		final Model model=parse("<x> rdf:value rdf:nil. <y> rdf:value rdf:nil.");

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineInverseEdgeTraits() {

		final Shape shape=trait(step(RDF.VALUE, true), any(RDF.NIL));
		final Model model=parse("rdf:nil rdf:value <x>. rdf:nil rdf:value <y>.");

		final Report report=process(shape, model, x, y);

		System.out.println(report);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleObjects() {

		final Shape shape=trait(step(RDF.VALUE), and());
		final Model model=parse("<x> rdf:value rdf:first, rdf:rest. <y> rdf:value rdf:first, rdf:rest.");

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleSources() {

		final Shape shape=trait(step(RDF.VALUE, true), and());
		final Model model=parse(
				"rdf:first rdf:value <x>. rdf:rest rdf:value <x>. rdf:first rdf:value <y>. rdf:rest rdf:value <y>."
		);

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleDirectEdges() {

		final Shape shape=and(
				trait(step(RDF.FIRST), and()),
				trait(step(RDF.REST), and())
		);

		final Model model=parse(
				"<x> rdf:first rdf:value; rdf:rest rdf:value . <y> rdf:first rdf:value; rdf:rest rdf:value .");

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleInverseEdges() {

		final Shape shape=and(
				trait(step(RDF.FIRST, true), and()),
				trait(step(RDF.REST, true), and())
		);

		final Model model=parse(
				"rdf:value rdf:first <x>. rdf:value rdf:rest <x>. rdf:value rdf:first <y>. rdf:value rdf:rest <y>.");

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleDirectEdgeValuePairs() {

		final Shape shape=and(
				trait(step(RDF.FIRST, true), and()),
				trait(step(RDF.REST, true), and())
		);

		final Model model=parse(""
				+"rdf:first rdf:first rdf:first, rdf:rest; rdf:rest rdf:first, rdf:rest ."
				+"rdf:rest rdf:first rdf:first, rdf:rest; rdf:rest rdf:first, rdf:rest ."
		);

		final Report report=process(shape, model, RDF.FIRST, RDF.REST);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}


	//// Shapes ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testValidateMinCount() {

		final Shape shape=MinCount.minCount(2);

		assertTrue("pass", validate(shape, x, y, z));
		assertFalse("fail", validate(shape, x));

	}

	@Test public void testValidateMaxCount() {

		final Shape shape=maxCount(2);

		assertTrue("pass", validate(shape, x, y));
		assertFalse("fail", validate(shape, x, y, z));

	}

	@Test public void testValidateIn() {

		final Shape shape=in(x, y);

		assertTrue("pass", validate(shape, x, y));
		assertFalse("fail", validate(shape, x, z));
		assertTrue("pass / empty focus", validate(shape));

	}

	@Test public void testValidateAll() {

		final Shape shape=all(x, y);

		assertTrue("pass", validate(shape, x, y, z));
		assertFalse("fail", validate(shape, x));
		assertFalse("fail / empty focus", validate(shape));

	}

	@Test public void testValidateAny() {

		final Shape shape=any(x, y);

		assertTrue("pass", validate(shape, x));
		assertFalse("fail", validate(shape, z));
		assertFalse("fail / empty focus", validate(shape));

	}


	@Test public void testValidateDataype() {

		assertTrue("pass / empty", validate(datatype(Values.BNodeType)));
		assertTrue("pass / blank", validate(datatype(Values.BNodeType), bnode()));
		assertTrue("pass / iri", validate(datatype(Values.IRIType), ValuesTest.term("iri")));
		assertTrue("pass / plain literal", validate(datatype(XMLSchema.STRING), literal("text")));
		assertTrue("pass / tagged literal", validate(datatype(RDF.LANGSTRING), literal("text", "en")));
		assertTrue("pass / typed literal", validate(datatype(XMLSchema.BOOLEAN), literal(true)));

		assertFalse("fail", validate(datatype(Values.IRIType), bnode()));

		assertTrue("pass / generic resource", validate(datatype(Values.ResoureType), bnode()));
		assertFalse("fail / generic resource", validate(datatype(Values.ResoureType), literal(true)));

		assertTrue("pass / generic literal", validate(datatype(RDFS.LITERAL), literal(true)));
		assertFalse("fail / generic literal", validate(datatype(RDFS.LITERAL), bnode()));

	}

	@Test public void testValidateClazz() {

		final Shape shape=clazz(z);
		final Model model=parse("<x> a <y>. <y> rdfs:subClassOf <z>.");

		assertTrue("pass", validate(shape, model, x));
		assertFalse("fail", validate(shape, model, y));

	}


	@Test public void testValidateMinExclusive() {

		final Shape shape=minExclusive(literal(1));

		assertTrue("integer / pass", validate(shape, literal(2)));
		assertFalse("integer / fail / equal", validate(shape, literal(1)));
		assertFalse("integer / fail", validate(shape, literal(0)));

	}

	@Test public void testValidateMaxExclusive() {

		final Shape shape=maxExclusive(literal(10));

		assertTrue("integer / pass", validate(shape, literal(2)));
		assertFalse("integer / fail / equal", validate(shape, literal(10)));
		assertFalse("integer / fail", validate(shape, literal(100)));

	}

	@Test public void testValidateMinInclusive() {

		final Shape shape=MinInclusive.minInclusive(literal(1));

		assertTrue("integer / pass", validate(shape, literal(2)));
		assertTrue("integer / pass / equal", validate(shape, literal(1)));
		assertFalse("integer / fail", validate(shape, literal(0)));

	}

	@Test public void testValidateMaxInclusive() {

		final Shape shape=MaxInclusive.maxInclusive(literal(10));

		assertTrue("integer / pass", validate(shape, literal(2)));
		assertTrue("integer / pass / equal", validate(shape, literal(10)));
		assertFalse("integer / fail", validate(shape, literal(100)));

	}


	@Test public void testValidatePattern() {

		final Shape shape=Pattern.pattern(".*\\.org");

		assertTrue("iri / pass", validate(shape, iri("http://exampe.org")));
		assertFalse("iri / fail", validate(shape, iri("http://exampe.com")));

		assertTrue("string / pass", validate(shape, literal("example.org")));
		assertFalse("string / fail", validate(shape, literal("example.com")));

	}

	@Test public void testValidateLike() {

		final Shape shape=like("ex.org");

		assertTrue("iri / pass", validate(shape, iri("http://exampe.org/")));
		assertFalse("iri / fail", validate(shape, iri("http://exampe.com/")));

		assertTrue("string / pass", validate(shape, literal("example.org")));
		assertFalse("string / fail", validate(shape, literal("example.com")));

	}

	@Test public void testValidateMinLength() {

		final Shape shape=MinLength.minLength(3);

		assertTrue("number / pass", validate(shape, literal(100)));
		assertFalse("number / fail", validate(shape, literal(99)));

		assertTrue("string / pass", validate(shape, literal("100")));
		assertFalse("string / fail", validate(shape, literal("99")));

	}

	@Test public void testValidateMaxLength() {

		final Shape shape=MaxLength.maxLength(2);

		assertTrue("number / pass", validate(shape, literal(99)));
		assertFalse("number / fail", validate(shape, literal(100)));

		assertTrue("string / pass", validate(shape, literal("99")));
		assertFalse("string / fail", validate(shape, literal("100")));

	}


	@Test public void testValidateCustom() {

		final Shape shape=Custom.custom(Issue.Level.Error, "test custom shape",
				"select * { filter not exists { ?this rdf:value rdf:first } }");

		final Model model=parse(""
				+"<x> rdf:value rdf:first.\n"
				+"<y> rdf:value rdf:first, rdf:rest.\n"
				+"<z> rdf:value rdf:rest. ");

		assertTrue("pass / empty", validate(shape, model));
		assertTrue("pass / single", validate(shape, model, x));
		assertTrue("pass / multiple", validate(shape, model, x, y));

		assertFalse("fail / single", validate(shape, model, z));
		assertFalse("fail / multiple", validate(shape, model, x, y, z));
	}

	@Test public void testValidateCustomWithReportingDetails() {

		final Shape shape=Custom.custom(Issue.Level.Warning, "custom {?type}",
				"select ('shape' as ?type) {}");

		final IRI focus=x;

		final Report report=process(shape, focus);

		assertTrue("reported issue level", report.assess(Issue.Level.Warning));

		assertEquals("reported focus node", set(focus),
				report.getIssues().stream().flatMap(issue -> issue.getValues().stream()).collect(toSet()));

		assertEquals("populated message template", "custom shape",
				report.getIssues().stream().map(Issue::getMessage).findFirst().orElse(null));

	}

	@Test public void testValidateCustomOnAggregates() {

		final Shape shape=Custom.custom(Issue.Level.Error, "count={?count}",
				"select ?count { { select (count(?this) as ?count) {} values ?this {} } }");

		final Report report=process(shape, x, y, z);

		assertEquals("custom aggregate validation", "count=3",
				report.getIssues().stream().map(Issue::getMessage).findFirst().orElse(""));

	}


	@Test public void testValidateConjunction() {

		final Shape shape=and(any(literal(1)), any(literal(2)));

		assertTrue("pass", validate(shape, literal(1), literal(2), literal(3)));
		assertFalse("fail", validate(shape, literal(1), literal(3)));

	}

	@Test public void testValidateDisjunction() {

		final Shape shape=Or.or(all(literal(1), literal(2)), all(literal(1), literal(3)));

		assertTrue("pass", validate(shape, literal(3), literal(2), literal(1)));
		assertFalse("fail", validate(shape, literal(3), literal(2)));

	}

	@Test public void testValidateOption() {

		final Shape shape=test(any(literal(1)), any(literal(2)), any(literal(3)));

		assertTrue("true / pass", validate(shape, literal(1), literal(2)));
		assertFalse("true / fail", validate(shape, literal(1), literal(3)));

		assertTrue("false / pass", validate(shape, literal(3)));
		assertFalse("false / fail", validate(shape, literal(2)));

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
		return ValuesTest.connection(connection -> new SPARQLWriter(connection).process(shape, statements, focus));
	}

}
