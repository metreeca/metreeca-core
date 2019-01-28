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

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;

import static org.assertj.core.api.Assertions.assertThat;


final class FocusTest {

	private static final Issue info=issue(Issue.Level.Info, "info", and());
	private static final Issue warning=issue(Issue.Level.Warning, "warning", and());
	private static final Issue error=issue(Issue.Level.Error, "error", and());


	@Test void testMerge() {

		final Issue[] lower={info, warning};
		final Issue[] upper={warning, error};
		final Issue[] merged={info, warning, error};


		assertThat(focus(lower).merge(focus(upper)).getIssues())
				.as("merge focus issues")
				.containsOnly(info, warning, error);


		assertThat(focus(frame(RDF.NIL, lower)).merge(focus(frame(RDF.NIL,upper))).getFrames())
				.as("merge compatible frame issues")
				.containsOnly(frame(RDF.NIL, merged));

		assertThat(focus(frame(RDF.FIRST, lower)).merge(focus(frame(RDF.REST, upper))).getFrames())
				.as("preserve distinct frames")
				.containsOnly(
						frame(RDF.FIRST, lower),
						frame(RDF.REST, upper)
				);


		assertThat(focus(frame(RDF.NIL, RDF.VALUE, focus(lower))).merge(focus(frame(RDF.NIL, RDF.VALUE, focus(upper)))).getFrames())
				.as("merge compatible fields")
				.containsOnly(frame(RDF.NIL, RDF.VALUE, focus(merged)));

		assertThat(focus(frame(RDF.NIL, RDF.FIRST, focus(lower))).merge(focus(frame(RDF.NIL, RDF.REST, focus(upper)))).getFrames())
				.as("preserve distinct fields")
				.containsOnly(
						Frame.frame(RDF.NIL, set(), map(
								entry(RDF.FIRST, focus(lower)),
								entry(RDF.REST, focus(upper))
						))
				);

	}

	@Test void testAssess() {

		assertThat(focus(frame(RDF.NIL)).assess(Issue.Level.Info))
				.as("no issues")
				.isFalse();

		assertThat(focus(warning).assess(Issue.Level.Warning))
				.as("matching issue")
				.isTrue();

		assertThat(focus(warning).assess(Issue.Level.Error))
				.as("no matching issue")
				.isFalse();

		assertThat(focus(frame(RDF.NIL, error)).assess(Issue.Level.Error))
				.as("matching frame")
				.isTrue();

	}


	private Focus focus(final Issue... issues) {
		return Focus.focus(set(issues));
	}

	private Focus focus(final Frame... frames) {
		return Focus.focus(set(), set(frames));
	}

	private Frame frame(final Value value, final Issue... issues) {
		return Frame.frame(value, set(issues));
	}

	private Frame frame(final Value value, final IRI iri, final Focus focus) {
		return Frame.frame(value, set(), map(entry(iri, focus)));
	}

}
