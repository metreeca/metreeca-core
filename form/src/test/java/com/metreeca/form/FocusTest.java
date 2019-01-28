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

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.Frame.frame;
import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Sets.set;

import static org.assertj.core.api.Assertions.assertThat;


final class FocusTest {


	@Test void testAssess() {

		final Issue info=issue(Issue.Level.Info, "info", and());
		final Issue warning=issue(Issue.Level.Warning, "warning", and());
		final Issue error=issue(Issue.Level.Error, "error", and());


		assertThat(focus(set()).assess(Issue.Level.Info))
				.as("no issues")
				.isFalse();

		assertThat(focus(set(warning)).assess(Issue.Level.Warning))
				.as("matching issue")
				.isTrue();

		assertThat(focus(set(warning)).assess(Issue.Level.Error))
				.as("no matching issue")
				.isFalse();

		assertThat(focus(set(), set(frame(RDF.NIL, set(error)))).assess(Issue.Level.Error))
				.as("matching frame")
				.isTrue();

	}

}
