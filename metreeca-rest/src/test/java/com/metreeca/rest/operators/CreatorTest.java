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

package com.metreeca.rest.operators;

import com.metreeca.json.Shape;
import com.metreeca.rest.Request;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.json.Frame.frame;
import static com.metreeca.json.Values.item;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.rest.Response.Created;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.shape;
import static com.metreeca.rest.operators.Creator.creator;
import static com.metreeca.rest.operators.OperatorTest.exec;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;

final class CreatorTest {

	private final IRI focus=item("/");
	private final Shape shape=field(RDF.VALUE);


	@Test void testCreateResource() {
		exec(

				frame -> {

					assertThat(frame.focus()).as("generated unique iri").isNotEqualTo(focus);
					assertThat(frame.values(RDF.VALUE)).as("rewritten body").containsExactly(frame.focus());

					return true;

				},

				() -> creator()

						.handle(new Request()
								.set(shape(), shape)
								.body(jsonld(), frame(focus)
										.value(RDF.VALUE, focus)
								)
						)

						.accept(response -> assertThat(response)
								.hasStatus(Created)
								.hasAttribute(shape(), shape -> assertThat(shape).isEqualTo(or()))
								.doesNotHaveBody(jsonld())
						)

		);
	}

	@Test void testReportClash() {
		assertThatIllegalStateException().isThrownBy(() -> exec(frame -> false, () -> creator()

				.handle(new Request()
						.set(shape(), shape)
						.body(jsonld(), frame(item("/")))
				)

		));
	}

}