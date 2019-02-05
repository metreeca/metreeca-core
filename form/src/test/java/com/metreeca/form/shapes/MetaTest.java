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

package com.metreeca.form.shapes;

import com.metreeca.form.Form;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.pass;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Meta.*;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.form.shapes.Or.or;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Values.literal;

import static org.assertj.core.api.Assertions.assertThat;


final class MetaTest {

	@Nested final class MetasTest {

		@Test void testCollectMetadataFromAnnotationShapes() {
			assertThat(metas(label("label")))
					.as("collected from metadata")
					.containsOnly(
							entry(Form.Label, literal("label"))
					);
		}

		@Test void testCollectMetadataFromLogicalShapes() {

			assertThat(metas(and(
					label("label"),
					notes("notes")
			)))
					.as("collected from conjunctions")
					.containsOnly(
							entry(Form.Label, literal("label")),
							entry(Form.Notes, literal("notes"))
					);

			assertThat(metas(or(
					label("label"),
					notes("notes")
			)))
					.as("collected from disjunctions")
					.containsOnly(
							entry(Form.Label, literal("label")),
							entry(Form.Notes, literal("notes"))
					);

			assertThat(metas(when(
					pass(),
					label("label"),
					notes("notes")
			)))
					.as("collected from option")
					.containsOnly(
							entry(Form.Label, literal("label")),
							entry(Form.Notes, literal("notes"))
					);
		}

		@Test void testCollectMetadataFromNestedLogicalShapes() {

			assertThat(metas(and(
					and(
							label("label"),
							notes("notes")
					),
					or(
							alias("alias"),
							placeholder("placeholder")
					)
			)))
					.as("collected from nested logical shapes")
					.containsOnly(
							entry(Form.Label, literal("label")),
							entry(Form.Notes, literal("notes")),
							entry(Form.Alias, literal("alias")),
							entry(Form.Placeholder, literal("placeholder"))
					);

		}


		@Test void testCollectDuplicateMetadata() {
			assertThat(metas(and(
					label("label"),
					label("label")
			))).containsOnly(
					entry(Form.Label, literal("label"))
			);
		}

		@Test void testIgnoreConflictingMetadata() {
			assertThat(metas(and(
					label("label"),
					label("other")
			))).isEmpty();
		}


		@Test void testIgnoreMetadataFromStructuralShapes() {

			assertThat(metas(field(RDF.VALUE, field(RDF.VALUE, label("label")))))
					.as("ignored in fields")
					.isEmpty();

		}

	}

}
