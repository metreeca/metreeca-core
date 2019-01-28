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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.metreeca.form.Focus.focus;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.inverse;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.truths.ModelAssert.assertThat;

import static java.util.stream.Collectors.toList;


final class FrameTest {

	@Nested final class OutlineTest {

		private final Literal value=literal("value");


		@Test void testOutlineFields() {

			assertThat(outline(frame(RDF.NIL)))
					.as("no fields")
					.isEmpty();

			assertThat(outline(frame(RDF.NIL, RDF.VALUE, frame(value))))
					.as("direct")
					.isIsomorphicTo(statement(RDF.NIL, RDF.VALUE, value));

			assertThat(outline(frame(value, RDF.VALUE, frame(value))))
					.as("direct with literal source")
					.isEmpty();

			assertThat(outline(frame(value, inverse(RDF.VALUE), frame(RDF.NIL))))
					.as("inverse")
					.isIsomorphicTo(statement(RDF.NIL, RDF.VALUE, value));

			assertThat(outline(frame(value, inverse(RDF.VALUE), frame(value))))
					.as("inverse with literal source")
					.isEmpty();

		}


		@Test void testOutlineNestedFrames() {

			assertThat(outline(frame(RDF.NIL, RDF.VALUE, frame(RDF.FIRST, RDF.VALUE, frame(RDF.REST)))))
					.as("nested")
					.isIsomorphicTo(
							statement(RDF.NIL, RDF.VALUE, RDF.FIRST),
							statement(RDF.FIRST, RDF.VALUE, RDF.REST)
					);

		}


		private Frame frame(final Value value) {
			return Frame.frame(value);
		}

		private Frame frame(final Value value, final IRI iri, final Frame frame) {
			return Frame.frame(value, set(), map(entry(iri, focus(set(), set(frame)))));
		}

		private List<Statement> outline(final Frame frame) {
			return frame.outline().collect(toList());
		}

	}

}
