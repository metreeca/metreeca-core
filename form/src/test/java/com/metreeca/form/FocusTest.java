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

import com.metreeca.form.things.ValuesTest;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.truths.ModelAssert.assertThat;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toList;


final class FocusTest {

	private final IRI x=ValuesTest.item("x");
	private final IRI y=ValuesTest.item("y");
	private final IRI z=ValuesTest.item("z");

	private final Issue info=issue(Issue.Level.Info, "info", and());
	private final Issue warning=issue(Issue.Level.Warning, "warning", and());
	private final Issue error=issue(Issue.Level.Error, "error", and());


	@Test void testAssess() {

		assertThat(Focus.focus(set()).assess(Issue.Level.Info)).as("no issues").isFalse();

		assertThat(Focus.focus(list(warning)).assess(Issue.Level.Warning)).as("matching issue").isTrue();
		assertThat(Focus.focus(list(warning)).assess(Issue.Level.Error)).as("no matching issue").isFalse();

		assertThat(Focus.focus(list(), set(
				frame(RDF.NIL, set(), map(entry(RDF.VALUE, Focus.focus(set(error), set()))))
		)).assess(Issue.Level.Error)).as("matching frame").isTrue();

	}

	@Test void testPrune() {

		final Frame first=frame(x, set(), map(entry(RDF.FIRST, Focus.focus(set(new Issue[] {info})))));
		final Frame rest=frame(x, set(), map(entry(RDF.REST, Focus.focus(set(new Issue[] {warning})))));

		final Focus focus=Focus.focus(set(info, warning, error), set(first, rest))
				.prune(Issue.Level.Warning)
				.orElse(null);

		assertThat(set(warning, error)).as("pruned issues").isEqualTo(focus.getIssues());
		assertThat(set(rest)).as("pruned frames").isEqualTo(focus.getFrames());

	}

	@Test void testOutline() {

		assertThat(decode("<x> rdf:value <y>.")).as("direct edge").isIsomorphicTo(focus(

				frame(x, set(), map(entry(RDF.VALUE, focus(frame(y)))))

		).outline().collect(toList()));

		assertThat(decode("<y> rdf:value <x>.")).as("inverse edge").isIsomorphicTo(focus(

				frame(x, set(), map(entry(inverse(RDF.VALUE), focus(frame(y)))))

		).outline().collect(toList()));

		assertThat(decode("<x> rdf:value <y>, <z>.")).as("multiple traces").isIsomorphicTo(focus(

				frame(x, set(), map(entry(RDF.VALUE, focus(frame(y), frame(z)))))

		).outline().collect(toList()));

		assertThat(decode("<x> rdf:first <y>; rdf:rest <z>.")).as("multiple edges").isIsomorphicTo(focus(

				frame(x, set(), map(
						entry(RDF.FIRST, focus(frame(y))),
						entry(RDF.REST, focus(frame(z)))
				))

		).outline().collect(toList()));

		assertThat(set()).as("illegal direct edge").isIsomorphicTo(focus(

				frame(literal("x"), set(), map(entry(RDF.VALUE, focus(frame(y)))))

		).outline().collect(toList()));

		assertThat(set()).as("illegal inverse edge").isIsomorphicTo(focus(

				frame(x, set(), map(entry(inverse(RDF.VALUE), focus(frame(literal("y"))))))

		).outline().collect(toList()));

		assertThat(decode("<x> rdf:value <y>. <y> rdf:value <z>.")).as("nested edges").isIsomorphicTo(focus(

				frame(x, set(), map(entry(RDF.VALUE, focus(frame(y, set(), map(entry(RDF.VALUE, focus(frame(z)))))))))

		).outline().collect(toList()));

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Focus focus(final Frame... frames) {
		return Focus.focus(set(), set(frames));
	}

}
