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

package com.metreeca.form;

import com.metreeca.form.things.Values;
import com.metreeca.form.things.ValuesTest;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.ValuesTest.decode;

import static org.assertj.core.api.Assertions.assertThat;


final class FocusTest {

	private final IRI x=ValuesTest.item("x");
	private final IRI y=ValuesTest.item("y");
	private final IRI z=ValuesTest.item("z");

	private final Issue info=issue(Issue.Level.Info, "info", and());
	private final Issue warning=issue(Issue.Level.Warning, "warning", and());
	private final Issue error=issue(Issue.Level.Error, "error", and());


	@Test void testAssess() {

		assertThat(focus(set()).assess(Issue.Level.Info)).as("no issues").isFalse();

		assertThat(focus(list(warning)).assess(Issue.Level.Warning)).as("matching issue").isTrue();
		assertThat(focus(list(warning)).assess(Issue.Level.Error)).as("no matching issue").isFalse();

		assertThat(focus(list(), set(
				frame(RDF.NIL, entry(RDF.VALUE, focus(set(error), set())))
		)).assess(Issue.Level.Error)).as("matching frame").isTrue();

	}

	@Test void testPrune() {

		final Frame first=frame(x, entry(RDF.FIRST, focus(info)));
		final Frame rest=frame(x, entry(RDF.REST, focus(warning)));

		final Focus focus=focus(set(info, warning, error), set(first, rest))
				.prune(Issue.Level.Warning)
				.orElse(null);

		assertThat(set(warning, error)).as("pruned issues").isEqualTo(focus.getIssues());
		assertThat(set(rest)).as("pruned frames").isEqualTo(focus.getFrames());

	}

	@Test void testOutline() {

		assertThat(decode("<x> rdf:value <y>.")).as("direct edge").isIsomorphicTo(trace(

				frame(x, entry(RDF.VALUE, trace(frame(y))))

		).outline());

		assertThat(decode("<y> rdf:value <x>.")).as("inverse edge").isIsomorphicTo(trace(

				frame(x, entry(inverse(RDF.VALUE), trace(frame(y))))

		).outline());

		assertThat(decode("<x> rdf:value <y>, <z>.")).as("multiple traces").isIsomorphicTo(trace(

				frame(x, entry(RDF.VALUE, trace(frame(y), frame(z))))

		).outline());

		assertThat(decode("<x> rdf:first <y>; rdf:rest <z>.")).as("multiple edges").isIsomorphicTo(trace(

				frame(x,

						entry(RDF.FIRST, trace(frame(y))),
						entry(RDF.REST, trace(frame(z)))
				)

		).outline());

		assertThat(set()).as("illegal direct edge").isIsomorphicTo(trace(

				frame(Values.literal("x"), entry(RDF.VALUE, trace(frame(y))))

		).outline());

		assertThat(set()).as("illegal inverse edge").isIsomorphicTo(trace(

				frame(x, entry(inverse(RDF.VALUE), trace(frame(Values.literal("y")))))

		).outline());

		assertThat(decode("<x> rdf:value <y>. <y> rdf:value <z>.")).as("nested edges").isIsomorphicTo(trace(

				frame(x, entry(RDF.VALUE, trace(frame(y, entry(RDF.VALUE, trace(frame(z)))))))

		).outline());

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@SafeVarargs private final Focus trace(final Frame... traces) {
		return focus(set(), set(traces));
	}

}
