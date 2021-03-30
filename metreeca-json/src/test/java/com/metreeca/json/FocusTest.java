/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
