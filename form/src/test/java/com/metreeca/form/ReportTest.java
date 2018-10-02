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

package com.metreeca.form;

import com.metreeca.form.shapes.And;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.*;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Frame.slot;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.ModelAssert.assertThat;
import static com.metreeca.form.things.Sets.set;


final class ReportTest {

	private final IRI x=ValuesTest.item("x");
	private final IRI y=ValuesTest.item("y");
	private final IRI z=ValuesTest.item("z");

	private final Issue info=Issue.issue(Issue.Level.Info, "info", And.and());
	private final Issue warning=Issue.issue(Issue.Level.Warning, "warning", And.and());
	private final Issue error=Issue.issue(Issue.Level.Error, "error", And.and());


	@Test void testAssess() {

		Assertions.assertThat(Report.report(Sets.set()).assess(Issue.Level.Info)).as("no issues").isFalse();

		Assertions.assertThat(Report.report(list(warning)).assess(Issue.Level.Warning)).as("matching issue").isFalse();
		Assertions.assertThat(Report.report(list(warning)).assess(Issue.Level.Error)).as("no matching issue").isFalse();

		Assertions.assertThat(Report.report(Lists.list(), set(
				frame(RDF.NIL, slot(Step.step(RDF.VALUE), new Report(set(error), Sets.set())))
		)).assess(Issue.Level.Error)).as("matching frame").isFalse();

	}

	@Test void testPrune() {

		final Frame<Report> first=frame(x, slot(Step.step(RDF.FIRST), Report.report(info)));
		final Frame<Report> rest=frame(x, slot(Step.step(RDF.REST), Report.report(warning)));

		final Report report=Report.report(Sets.set(info, warning, error), Sets.set(first, rest))
				.prune(Issue.Level.Warning)
				.orElse(null);

		Assertions.assertThat(Sets.set(warning, error)).as("pruned issues").isEqualTo(report.getIssues());
		Assertions.assertThat(set(rest)).as("pruned frames").isEqualTo(report.getFrames());

	}

	@Test void testOutline() {

		assertThat((Collection<Statement>)ValuesTest.decode("<x> rdf:value <y>.")).as("direct edge").isIsomorphicTo(trace(

				frame(x, slot(Step.step(RDF.VALUE), trace(frame(y))))

		).outline());

		assertThat((Collection<Statement>)ValuesTest.decode("<y> rdf:value <x>.")).as("inverse edge").isIsomorphicTo(trace(

				frame(x, slot(Step.step(RDF.VALUE, true), trace(frame(y))))

		).outline());

		assertThat((Collection<Statement>)ValuesTest.decode("<x> rdf:value <y>, <z>.")).as("multiple traces").isIsomorphicTo(trace(

				frame(x, slot(Step.step(RDF.VALUE), trace(frame(y), frame(z))))

		).outline());

		assertThat((Collection<Statement>)ValuesTest.decode("<x> rdf:first <y>; rdf:rest <z>.")).as("multiple edges").isIsomorphicTo(trace(

				frame(x,

						slot(Step.step(RDF.FIRST), trace(frame(y))),
						slot(Step.step(RDF.REST), trace(frame(z)))
				)

		).outline());

		assertThat(Sets.set()).as("illegal direct edge").isIsomorphicTo(trace(

				frame(Values.literal("x"), slot(Step.step(RDF.VALUE), trace(frame(y))))

		).outline());

		assertThat(Sets.set()).as("illegal inverse edge").isIsomorphicTo(trace(

				frame(x, slot(Step.step(RDF.VALUE, true), trace(frame(Values.literal("y")))))

		).outline());

		assertThat((Collection<Statement>)ValuesTest.decode("<x> rdf:value <y>. <y> rdf:value <z>.")).as("nested edges").isIsomorphicTo(trace(

				frame(x, slot(Step.step(RDF.VALUE),
						trace(frame(y, slot(Step.step(RDF.VALUE),
								trace(frame(z)))))))

		).outline());

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs private final Report trace(final Frame<Report>... traces) {
		return Report.report(Sets.set(), Sets.set(traces));
	}

}
