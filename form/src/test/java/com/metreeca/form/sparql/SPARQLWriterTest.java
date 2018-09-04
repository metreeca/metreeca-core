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

package com.metreeca.form.sparql;

import com.metreeca.form.shapes.*;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Sets;
import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.form.Issue;
import com.metreeca.form.Report;
import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Models;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collection;
import java.util.function.Supplier;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Any.any;
import static com.metreeca.form.shapes.Clazz.clazz;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.In.in;
import static com.metreeca.form.shapes.Like.like;
import static com.metreeca.form.shapes.MaxCount.maxCount;
import static com.metreeca.form.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.form.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.form.shapes.Test.test;
import static com.metreeca.form.shapes.Trait.trait;
import static com.metreeca.form.shifts.Step.step;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.bnode;
import static com.metreeca.form.things.Values.iri;
import static com.metreeca.form.things.Values.literal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static java.util.Collections.emptySet;
import static java.util.stream.Collectors.toSet;


public class SPARQLWriterTest {

	private static final IRI x=ValuesTest.item("x");
	private static final IRI y=ValuesTest.item("y");
	private static final IRI z=ValuesTest.item("z");


	private final Supplier<RepositoryConnection> sandbox=ValuesTest.sandbox();


	//// Validation ////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testGenerateTraceNodes() {

		final Shape shape=maxInclusive(Values.literal(10));

		final Report report=process(shape, Values.literal(1), Values.literal(100));
		final Collection<Issue> issues=report.getIssues();

		assertTrue("report severity level", report.assess(Issue.Level.Error));

		assertTrue("reference failed shape",
				issues.stream().anyMatch(issue -> issue.getShape().equals(shape)));

		Assert.assertEquals("reference offending values", Sets.set(Values.literal(100)),
				issues.stream().flatMap(issue -> issue.getValues().stream()).collect(toSet()));

	}


	@Test public void testValidateDirectEdgeTraits() {

		final Shape shape=trait(Step.step(RDF.VALUE), any(RDF.NIL));

		final Report report=process(shape, x, y);

		assertTrue("identify invalid trait", report.assess(Issue.Level.Error));

	}

	@Test public void testValidateInverseEdgeTraits() {

		final Shape shape=trait(Step.step(RDF.VALUE, true), any(RDF.NIL));

		final Report report=process(shape, x, y);

		assertTrue("identify invalid trait", report.assess(Issue.Level.Error));

	}


	//// Outlining /////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test public void testOutlineClasses() {

		final Shape shape=clazz(RDFS.RESOURCE);
		final Model model=ValuesTest.decode("rdf:first a rdfs:Resource.");

		final Report report=process(shape, model, RDF.FIRST);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineDirectEdgeTraits() {

		final Shape shape=trait(Step.step(RDF.VALUE), any(RDF.NIL));
		final Model model=ValuesTest.decode("<x> rdf:value rdf:nil. <y> rdf:value rdf:nil.");

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineInverseEdgeTraits() {

		final Shape shape=trait(Step.step(RDF.VALUE, true), any(RDF.NIL));
		final Model model=ValuesTest.decode("rdf:nil rdf:value <x>. rdf:nil rdf:value <y>.");

		final Report report=process(shape, model, x, y);

		System.out.println(report);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleObjects() {

		final Shape shape=trait(Step.step(RDF.VALUE), and());
		final Model model=ValuesTest.decode("<x> rdf:value rdf:first, rdf:rest. <y> rdf:value rdf:first, rdf:rest.");

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleSources() {

		final Shape shape=trait(Step.step(RDF.VALUE, true), and());
		final Model model=ValuesTest.decode(
				"rdf:first rdf:value <x>. rdf:rest rdf:value <x>. rdf:first rdf:value <y>. rdf:rest rdf:value <y>."
		);

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleDirectEdges() {

		final Shape shape=and(
				trait(Step.step(RDF.FIRST), and()),
				trait(Step.step(RDF.REST), and())
		);

		final Model model=ValuesTest.decode(
				"<x> rdf:first rdf:value; rdf:rest rdf:value . <y> rdf:first rdf:value; rdf:rest rdf:value .");

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleInverseEdges() {

		final Shape shape=and(
				trait(Step.step(RDF.FIRST, true), and()),
				trait(Step.step(RDF.REST, true), and())
		);

		final Model model=ValuesTest.decode(
				"rdf:value rdf:first <x>. rdf:value rdf:rest <x>. rdf:value rdf:first <y>. rdf:value rdf:rest <y>.");

		final Report report=process(shape, model, x, y);

		assertFalse("validated", report.assess(Issue.Level.Error));
		assertTrue("outline computed", Models.isomorphic(model, report.outline()));

	}

	@Test public void testOutlineMultipleDirectEdgeValuePairs() {

		final Shape shape=and(
				trait(Step.step(RDF.FIRST, true), and()),
				trait(Step.step(RDF.REST, true), and())
		);

		final Model model=ValuesTest.decode(""
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

		final Shape shape=In.in(x, y);

		assertTrue("pass", validate(shape, x, y));
		assertFalse("fail", validate(shape, x, z));
		assertTrue("pass / empty focus", validate(shape));

	}

	@Test public void testValidateAll() {

		final Shape shape=All.all(x, y);

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
		assertTrue("pass / blank", validate(datatype(Values.BNodeType), Values.bnode()));
		assertTrue("pass / iri", validate(datatype(Values.IRIType), ValuesTest.term("iri")));
		assertTrue("pass / plain literal", validate(datatype(XMLSchema.STRING), Values.literal("text")));
		assertTrue("pass / tagged literal", validate(datatype(RDF.LANGSTRING), Values.literal("text", "en")));
		assertTrue("pass / typed literal", validate(datatype(XMLSchema.BOOLEAN), Values.literal(true)));

		assertFalse("fail", validate(datatype(Values.IRIType), Values.bnode()));

		assertTrue("pass / generic resource", validate(datatype(Values.ResoureType), Values.bnode()));
		assertFalse("fail / generic resource", validate(datatype(Values.ResoureType), Values.literal(true)));

		assertTrue("pass / generic literal", validate(datatype(RDFS.LITERAL), Values.literal(true)));
		assertFalse("fail / generic literal", validate(datatype(RDFS.LITERAL), Values.bnode()));

	}

	@Test public void testValidateClazz() {

		final Shape shape=clazz(z);
		final Model model=ValuesTest.decode("<x> a <y>. <y> rdfs:subClassOf <z>.");

		assertTrue("pass", validate(shape, model, x));
		assertFalse("fail", validate(shape, model, y));

	}


	@Test public void testValidateMinExclusive() {

		final Shape shape=MinExclusive.minExclusive(Values.literal(1));

		assertTrue("integer / pass", validate(shape, Values.literal(2)));
		assertFalse("integer / fail / equal", validate(shape, Values.literal(1)));
		assertFalse("integer / fail", validate(shape, Values.literal(0)));

	}

	@Test public void testValidateMaxExclusive() {

		final Shape shape=maxExclusive(Values.literal(10));

		assertTrue("integer / pass", validate(shape, Values.literal(2)));
		assertFalse("integer / fail / equal", validate(shape, Values.literal(10)));
		assertFalse("integer / fail", validate(shape, Values.literal(100)));

	}

	@Test public void testValidateMinInclusive() {

		final Shape shape=MinInclusive.minInclusive(Values.literal(1));

		assertTrue("integer / pass", validate(shape, Values.literal(2)));
		assertTrue("integer / pass / equal", validate(shape, Values.literal(1)));
		assertFalse("integer / fail", validate(shape, Values.literal(0)));

	}

	@Test public void testValidateMaxInclusive() {

		final Shape shape=maxInclusive(Values.literal(10));

		assertTrue("integer / pass", validate(shape, Values.literal(2)));
		assertTrue("integer / pass / equal", validate(shape, Values.literal(10)));
		assertFalse("integer / fail", validate(shape, Values.literal(100)));

	}


	@Test public void testValidatePattern() {

		final Shape shape=Pattern.pattern(".*\\.org");

		assertTrue("iri / pass", validate(shape, Values.iri("http://exampe.org")));
		assertFalse("iri / fail", validate(shape, Values.iri("http://exampe.com")));

		assertTrue("string / pass", validate(shape, Values.literal("example.org")));
		assertFalse("string / fail", validate(shape, Values.literal("example.com")));

	}

	@Test public void testValidateLike() {

		final Shape shape=like("ex.org");

		assertTrue("iri / pass", validate(shape, Values.iri("http://exampe.org/")));
		assertFalse("iri / fail", validate(shape, Values.iri("http://exampe.com/")));

		assertTrue("string / pass", validate(shape, Values.literal("example.org")));
		assertFalse("string / fail", validate(shape, Values.literal("example.com")));

	}

	@Test public void testValidateMinLength() {

		final Shape shape=MinLength.minLength(3);

		assertTrue("number / pass", validate(shape, Values.literal(100)));
		assertFalse("number / fail", validate(shape, Values.literal(99)));

		assertTrue("string / pass", validate(shape, Values.literal("100")));
		assertFalse("string / fail", validate(shape, Values.literal("99")));

	}

	@Test public void testValidateMaxLength() {

		final Shape shape=MaxLength.maxLength(2);

		assertTrue("number / pass", validate(shape, Values.literal(99)));
		assertFalse("number / fail", validate(shape, Values.literal(100)));

		assertTrue("string / pass", validate(shape, Values.literal("99")));
		assertFalse("string / fail", validate(shape, Values.literal("100")));

	}


	@Test public void testValidateCustom() {

		final Shape shape=Custom.custom(Issue.Level.Error, "test custom shape",
				"select * { filter not exists { ?this rdf:value rdf:first } }");

		final Model model=ValuesTest.decode(""
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

		assertEquals("reported focus node", Sets.set(focus),
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

		final Shape shape=and(any(Values.literal(1)), any(Values.literal(2)));

		assertTrue("pass", validate(shape, Values.literal(1), Values.literal(2), Values.literal(3)));
		assertFalse("fail", validate(shape, Values.literal(1), Values.literal(3)));

	}

	@Test public void testValidateDisjunction() {

		final Shape shape=Or.or(All.all(Values.literal(1), Values.literal(2)), All.all(Values.literal(1), Values.literal(3)));

		assertTrue("pass", validate(shape, Values.literal(3), Values.literal(2), Values.literal(1)));
		assertFalse("fail", validate(shape, Values.literal(3), Values.literal(2)));

	}

	@Test public void testValidateOption() {

		final Shape shape=com.metreeca.form.shapes.Test.test(any(Values.literal(1)), any(Values.literal(2)), any(Values.literal(3)));

		assertTrue("true / pass", validate(shape, Values.literal(1), Values.literal(2)));
		assertFalse("true / fail", validate(shape, Values.literal(1), Values.literal(3)));

		assertTrue("false / pass", validate(shape, Values.literal(3)));
		assertFalse("false / fail", validate(shape, Values.literal(2)));

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
		try ( final RepositoryConnection connection=sandbox.get()) {
			return new SPARQLWriter(connection).process(shape, statements, focus);
		}
	}

}
