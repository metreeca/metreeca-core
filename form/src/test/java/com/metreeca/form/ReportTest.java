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

import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.*;
import com.metreeca.form.shapes.And;
import com.metreeca.form.things.Sets;
import com.metreeca.form.things.ValuesTest;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.Test;

import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Frame.slot;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.assertIsomorphic;
import static com.metreeca.form.things.ValuesTest.decode;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;


public class ReportTest {

	private final IRI x=ValuesTest.item("x");
	private final IRI y=ValuesTest.item("y");
	private final IRI z=ValuesTest.item("z");

	private final Issue info=Issue.issue(Issue.Level.Info, "info", And.and());
	private final Issue warning=Issue.issue(Issue.Level.Warning, "warning", And.and());
	private final Issue error=Issue.issue(Issue.Level.Error, "error", And.and());


	@Test public void testAssess() {

		assertFalse("no issues", Report.trace(Sets.set()).assess(Issue.Level.Info));

		assertTrue("matching issue", Report.trace(list(warning)).assess(Issue.Level.Warning));
		assertFalse("no matching issue", Report.trace(list(warning)).assess(Issue.Level.Error));

		assertTrue("matching frame", Report.trace(Lists.list(), set(
				frame(RDF.NIL, slot(Step.step(RDF.VALUE), new Report(set(error), Sets.set())))
		)).assess(Issue.Level.Error));

	}

	@Test public void testPrune() {

		final Frame<Report> first=frame(x, slot(Step.step(RDF.FIRST), Report.trace(info)));
		final Frame<Report> rest=frame(x, slot(Step.step(RDF.REST), Report.trace(warning)));

		final Report report=Report.trace(Sets.set(info, warning, error), Sets.set(first, rest))
				.prune(Issue.Level.Warning)
				.orElse(null);

		assertEquals("pruned issues", Sets.set(warning, error), report.getIssues());
		assertEquals("pruned frames", set(rest), report.getFrames());

	}

	@Test public void testOutline() {

		ValuesTest.assertIsomorphic("direct edge", ValuesTest.decode("<x> rdf:value <y>."), trace(

				frame(x, slot(Step.step(RDF.VALUE), trace(frame(y))))

		).outline());

		ValuesTest.assertIsomorphic("inverse edge", ValuesTest.decode("<y> rdf:value <x>."), trace(

				frame(x, slot(Step.step(RDF.VALUE, true), trace(frame(y))))

		).outline());

		ValuesTest.assertIsomorphic("multiple traces", ValuesTest.decode("<x> rdf:value <y>, <z>."), trace(

				frame(x, slot(Step.step(RDF.VALUE), trace(frame(y), frame(z))))

		).outline());

		ValuesTest.assertIsomorphic("multiple edges", ValuesTest.decode("<x> rdf:first <y>; rdf:rest <z>."), trace(

				frame(x,

						slot(Step.step(RDF.FIRST), trace(frame(y))),
						slot(Step.step(RDF.REST), trace(frame(z)))
				)

		).outline());

		ValuesTest.assertIsomorphic("illegal direct edge", Sets.set(), trace(

				frame(Values.literal("x"), slot(Step.step(RDF.VALUE), trace(frame(y))))

		).outline());

		ValuesTest.assertIsomorphic("illegal inverse edge", Sets.set(), trace(

				frame(x, slot(Step.step(RDF.VALUE, true), trace(frame(Values.literal("y")))))

		).outline());

		ValuesTest.assertIsomorphic("nested edges", ValuesTest.decode("<x> rdf:value <y>. <y> rdf:value <z>."), trace(

				frame(x, slot(Step.step(RDF.VALUE),
						trace(frame(y, slot(Step.step(RDF.VALUE),
								trace(frame(z)))))))

		).outline());

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs private final Report trace(final Frame<Report>... traces) {
		return Report.trace(Sets.set(), Sets.set(traces));
	}

}
