/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.handlers;

import org.junit.jupiter.api.Test;

import static com.metreeca.rest.handlers.Publisher.variants;
import static org.assertj.core.api.Assertions.assertThat;

final class PublisherTest {

	@Test void testVariants() {

		assertThat(variants("link")).containsExactly("link", "link.html");
		assertThat(variants("link.html")).containsExactly("link.html");

		assertThat(variants("path/link")).containsExactly("path/link", "path/link.html");
		assertThat(variants("path/link.html")).containsExactly("path/link.html");

		assertThat(variants("path/link#hash")).containsExactly("path/link#hash", "path/link.html#hash");
		assertThat(variants("path/link.html#hash")).containsExactly("path/link.html#hash");

		assertThat(variants(".")).containsExactly("index.html");
		assertThat(variants("path/")).containsExactly("path/index.html");
		assertThat(variants("path/#hash")).containsExactly("path/index.html#hash");

		assertThat(variants("reindex")).containsExactly("reindex", "reindex.html");

	}
}