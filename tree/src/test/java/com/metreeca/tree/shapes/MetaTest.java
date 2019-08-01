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

import com.metreeca.tree.Shape;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.Map;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Meta.*;
import static com.metreeca.tree.shapes.Or.or;
import static com.metreeca.tree.shapes.When.when;

import static org.assertj.core.api.Assertions.assertThat;



final class MetaTest {

	@Nested final class MetasTest {

		@Test void testCollectMetadataFromAnnotationShapes() {
			assertThat(metas(label("label")))
					.as("collected from metadata")
					.containsOnly(
							entry(Shape.Label, "label")
					);
		}

		@Test void testCollectMetadataFromLogicalShapes() {

			assertThat(metas(and(
					label("label"),
					notes("notes")
			)))
					.as("collected from conjunctions")
					.containsOnly(
							entry(Shape.Label, "label"),
							entry(Shape.Notes, "notes")
					);

			assertThat(metas(or(
					label("label"),
					notes("notes")
			)))
					.as("collected from disjunctions")
					.containsOnly(
							entry(Shape.Label, "label"),
							entry(Shape.Notes, "notes")
					);

			assertThat(metas(when(
					and(),
					label("label"),
					notes("notes")
			)))
					.as("collected from option")
					.containsOnly(
							entry(Shape.Label, "label"),
							entry(Shape.Notes, "notes")
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
							entry(Shape.Label, "label"),
							entry(Shape.Notes, "notes"),
							entry(Shape.Alias, "alias"),
							entry(Shape.Placeholder, "placeholder")
					);

		}


		@Test void testCollectDuplicateMetadata() {
			assertThat(metas(and(
					label("label"),
					label("label")
			))).containsOnly(
					entry(Shape.Label, "label")
			);
		}

		@Test void testIgnoreConflictingMetadata() {
			assertThat(metas(and(
					label("label"),
					label("other")
			))).isEmpty();
		}


		@Test void testIgnoreMetadataFromStructuralShapes() {

			assertThat(metas(field("value", field("value", label("label")))))
					.as("ignored in fields")
					.isEmpty();

		}

	}


	private <K, V> Map.Entry<K, V> entry(final K key, final V value) {
		return new SimpleImmutableEntry<>(key, value);
	}

}
