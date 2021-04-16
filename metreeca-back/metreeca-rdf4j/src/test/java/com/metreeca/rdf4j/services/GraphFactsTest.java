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

package com.metreeca.rdf4j.services;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.OWL;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Values.inverse;
import static com.metreeca.json.Values.iri;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.rdf4j.services.GraphFacts.*;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;

final class GraphFactsTest {

	private static final IRI p=iri("test:p");
	private static final IRI q=iri("test:q");
	private static final IRI r=iri("test:r");

	private static final IRI x=iri("test:x");
	private static final IRI y=iri("test:y");


	@Nested final class Path {

		@Test void testRoot() {
			Assertions.assertThat(path(

					and(all(x), field(p)),

					emptyList()

			)).isEqualTo(

					and()

			);
		}

		@Test void testShallow() {
			Assertions.assertThat(path(

					and(all(x), field(p), field(q)),

					singletonList(p)

			)).isEqualTo(

					field(p)

			);
		}

		@Test void testDeep() {
			Assertions.assertThat(path(

					and(all(x), field(p, field(q, all(y))), field(r)),

					asList(p, q)

			)).isEqualTo(

					field(p, field(q))

			);
		}


		@Test void testRootLink() {
			Assertions.assertThat(path(

					link(OWL.SAMEAS, all(x)),

					emptyList()

			)).isEqualTo(

					link(OWL.SAMEAS)

			);
		}

		@Test void testShallowLink() {
			Assertions.assertThat(path(

					field(p, link(OWL.SAMEAS, field(q))),

					singletonList(p)

			)).isEqualTo(

					field(p, link(OWL.SAMEAS))

			);
		}

		@Test void testDeepLink() {
			Assertions.assertThat(path(

					field(p, field(q, link(OWL.SAMEAS))),

					asList(p, q)

			)).isEqualTo(

					field(p, field(q, link(OWL.SAMEAS)))

			);
		}

	}

	@Nested final class Hook {

		@Test void testRoot() {
			Assertions.assertThat(hook(

					and(all(x), field("p", p)),

					emptyList()

			)).isEqualTo(root);
		}

		@Test void testShallow() {
			Assertions.assertThat(hook(

					and(all(x), field("p", p), field("q", q)),

					singletonList(p)

			)).isEqualTo("p");
		}

		@Test void testDeep() {
			Assertions.assertThat(hook(

					and(all(x), field("p", p, field("q", q, all(y))), field("r", r)),

					asList(p, q)

			)).isEqualTo("q");
		}


		@Test void testRootDirectLink() {
			Assertions.assertThat(hook(

					link(OWL.SAMEAS, all(x)),

					emptyList()

			)).isEqualTo(value(root));
		}

		@Test void testShallowDirectLink() {
			Assertions.assertThat(hook(

					field("p", p, link(OWL.SAMEAS, field("q", q))),

					singletonList(p)

			)).isEqualTo(value("p"));
		}

		@Test void testDeepDirectLink() {
			Assertions.assertThat(hook(

					field(p, field("q", q, link(OWL.SAMEAS))),

					asList(p, q)

			)).isEqualTo(value("q"));
		}


		@Test void testRootInverseLink() {
			Assertions.assertThat(hook(

					link(inverse(OWL.SAMEAS), all(x)),

					emptyList()

			)).isEqualTo(alias(root));
		}

		@Test void testShallowInverseLink() {
			Assertions.assertThat(hook(

					field("p", p, link(inverse(OWL.SAMEAS), field("q", q))),

					singletonList(p)

			)).isEqualTo(alias("p"));
		}

		@Test void testDeepInverseLink() {
			Assertions.assertThat(hook(

					field(p, field("q", q, link(inverse(OWL.SAMEAS)))),

					asList(p, q)

			)).isEqualTo(alias("q"));
		}

	}

}