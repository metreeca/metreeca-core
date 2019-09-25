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

package com.metreeca.gcp.services;

import com.metreeca.gcp.GCP;
import com.metreeca.tree.Shape;

import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Clazz.clazz;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Guard.guard;
import static com.metreeca.tree.shapes.MaxCount.maxCount;
import static com.metreeca.tree.shapes.Meta.meta;
import static com.metreeca.tree.shapes.MinCount.minCount;
import static com.metreeca.tree.shapes.Or.or;

import static org.assertj.core.api.Assertions.assertThat;


final class DatastoreSplitterTest extends DatastoreTestBase {

	@Nested final class Container {

		private Shape container(final Shape shape) {
			return new DatastoreSplitter().container(shape);
		}


		@Test void testExtractContainerSection() {
			exec(() -> assertThat(container(and(

					meta(Shape.Label, "label"),
					clazz("Clazz"),
					field("field", and()),
					or(clazz("Clazz"), field("field", and())),
					field(GCP.contains, and())

			))).isEqualTo(and(

					meta(Shape.Label, "label"), // preserve annotations
					clazz("Clazz"), // preserve constraints
					field("field", and()), // preserve fields
					or(clazz("Clazz"), field("field", and()))// preserve structure

			)));
		}

		@Test void testIdempotentIfContainerSectionIsMissing() {
			exec(() -> assertThat(container(and(

					meta(Shape.Label, "label"),
					clazz("Clazz"),
					field("field", and()),
					or(clazz("Clazz"), field("field", and()))

			))).isEqualTo(and(

					meta(Shape.Label, "label"), // preserve annotations
					clazz("Clazz"), // preserve constraints
					field("field", and()), // preserve fields
					or(clazz("Clazz"), field("field", and()))// preserve structure

			)));
		}

	}

	@Nested final class Resource {

		private Shape resource(final Shape shape) {
			return new DatastoreSplitter().resource(shape);
		}


		@Test void testExtractResourceSection() {
			exec(() -> assertThat(resource(and(

					meta(Shape.Label, "label"),
					clazz("Clazz"),
					field("field", and()),
					or(
							guard("axis"),
							field("field", and())
					),
					or(
							guard("axis"),
							field(GCP.contains, minCount(1)),
							field(GCP.contains, maxCount(1))
					)

			))).isEqualTo(

					or( // preserve structure
							guard("axis"), // preserve guards
							minCount(1), // unwrap resource fields
							maxCount(1) // unwrap resource fields
					)

			));
		}

		@Test void testHandleEmptyResourceSection() {
			exec(() -> assertThat(resource(and(

					meta(Shape.Label, "label"),
					clazz("Clazz"),
					field("field", and()),
					and(field("field", and())),
					or(
							field(GCP.contains, and()),
							field(GCP.contains, and())
					)

			))).isEqualTo(

					and()

			));
		}

		@Test void testIdempotentIfResourceSectionIsMissing() {
			exec(() -> assertThat(resource(and(

					guard("axis"), // guards are preserved and thwart constant-based resource presence tests
					meta(Shape.Label, "label"),
					clazz("Clazz"),
					field("field", and()),
					and(
							guard("axis"),
							field("field", and())
					)

			))).isEqualTo(and(

					guard("axis"),
					meta(Shape.Label, "label"),
					clazz("Clazz"),
					field("field", and()),
					and(
							guard("axis"),
							field("field", and())
					)


			)));
		}

	}

}
