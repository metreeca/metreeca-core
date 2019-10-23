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

package com.metreeca.tree;

import org.junit.jupiter.api.Test;

import static com.metreeca.tree.Shape.relative;

import static org.assertj.core.api.Assertions.assertThat;


final class ShapeTest {

	@Test void testResolveRelativeValues() {

		final String target="http://example.org/collection/member/nested";

		assertThat(relative().apply(target))
				.as("target")
				.isEqualTo(target);

		assertThat(relative(".").apply(target))
				.as("member w/o trailing slash")
				.isEqualTo("http://example.org/collection/member");

		assertThat(relative("./").apply(target))
				.as("member w/ trailing slash")
				.isEqualTo("http://example.org/collection/member/");

		assertThat(relative("..").apply(target))
				.as("collection w/o trailing slash")
				.isEqualTo("http://example.org/collection");

		assertThat(relative("../").apply(target))
				.as("collection w/ trailing slash")
				.isEqualTo("http://example.org/collection/");

		assertThat(relative("../sibling").apply(target))
				.as("sibling")
				.isEqualTo("http://example.org/collection/sibling");

	}

}
