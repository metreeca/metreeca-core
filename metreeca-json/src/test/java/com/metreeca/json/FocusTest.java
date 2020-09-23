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

package com.metreeca.json;

import org.eclipse.rdf4j.model.IRI;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Focus.focus;
import static com.metreeca.json.Values.iri;
import static org.assertj.core.api.Assertions.assertThat;


final class FocusTest {

	private final IRI target=iri("http://example.org/collection/member/nested");

	@Test void testResolveRelativeValues() {

		assertThat(focus().resolve(target))
				.as("target")
				.isEqualTo(target);

		assertThat(focus(".").resolve(target))
				.as("member w/o trailing slash")
				.isEqualTo(iri("http://example.org/collection/member"));

		assertThat(focus("./").resolve(target))
				.as("member w/ trailing slash")
				.isEqualTo(iri("http://example.org/collection/member/"));

		assertThat(focus("..").resolve(target))
				.as("collection w/o trailing slash")
				.isEqualTo(iri("http://example.org/collection"));

		assertThat(focus("../").resolve(target))
				.as("collection w/ trailing slash")
				.isEqualTo(iri("http://example.org/collection/"));

		assertThat(focus("../sibling").resolve(target))
				.as("sibling")
				.isEqualTo(iri("http://example.org/collection/sibling"));


		assertThat(focus("sibling").resolve(target))
				.as("relative target")
				.isEqualTo(iri("http://example.org/collection/member/sibling"));

	}

}