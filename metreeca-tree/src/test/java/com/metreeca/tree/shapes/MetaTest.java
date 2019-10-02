/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca/Link.
 *
 * Metreeca/Link is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca/Link is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca/Link.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.tree.shapes;

import org.junit.jupiter.api.Test;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Meta.*;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;


final class MetaTest {

	@Test void testCollectMetadataFromAnnotationShapes() {
		assertThat(label(label("label")))
				.as("collected from metadata")
				.contains("label");
	}

	@Test void testCollectMetadataFromLogicalShapes() {

		assertThat(label(and(
				label("label"),
				notes("notes")
		)))
				.as("collected from conjunctions")
				.contains("label");

		assertThat(label(or(
				label("label"),
				notes("notes")
		)))
				.as("collected from disjunctions")
				.contains("label");

		assertThat(label(when(
				and(),
				label("label"),
				notes("notes")
		)))
				.as("collected from option")
				.contains("label");

	}

	@Test void testCollectMetadataFromNestedLogicalShapes() {

		assertThat(label(and(
				and(
						placeholder("placeholder"),
						notes("notes")
				),
				or(
						alias("alias"),
						label("label")
				)
		)))
				.as("collected from nested logical shapes")
				.contains("label");

	}


	@Test void testCollectDuplicateMetadata() {
		assertThat(label(and(
				label("label"),
				label("label")
		)))
				.contains("label");
	}

	@Test void testIgnoreConflictingMetadata() {
		assertThat(label(and(
				label("label"),
				label("other")
		))).isEmpty();
	}


	@Test void testIgnoreMetadataFromStructuralShapes() {

		assertThat(label(field("value", field("value", label("label")))))
				.as("ignored in fields")
				.isEmpty();

	}

}
