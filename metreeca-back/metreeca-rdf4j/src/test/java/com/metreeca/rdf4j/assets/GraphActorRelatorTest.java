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

package com.metreeca.rdf4j.assets;

import com.metreeca.rdf4j.assets.GraphEngine.Options;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;

import org.junit.jupiter.api.Test;

import static com.metreeca.json.ModelAssert.assertThat;
import static com.metreeca.json.Values.Base;
import static com.metreeca.json.ValuesTest.small;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.rdf4j.assets.GraphQueryBaseTest.EmployeeShape;
import static com.metreeca.rdf4j.assets.GraphTest.exec;
import static com.metreeca.rdf4j.assets.GraphTest.model;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONLDFormat.jsonld;
import static com.metreeca.rest.formats.JSONLDFormat.shape;

import static org.assertj.core.api.Assertions.assertThat;

final class GraphActorRelatorTest {

	private static Options options() {
		return new Options(new GraphEngine());
	}

	private Request request() {
		return new Request()
				.base(Base)
				.path("/employees/1370")
				.attribute(shape(), EmployeeShape);
	}


	@Test void testRelate() {
		exec(model(small()), () -> new GraphActorRelator(options())

				.handle(request())

				.accept(response -> assertThat(response)

						.hasStatus(Response.OK).hasAttribute(shape(),
								shape -> assertThat(shape).isNotEqualTo(and()))

						.hasBody(jsonld(), rdf -> assertThat(rdf)
								.as("items retrieved")
								.isSubsetOf(model(
										"construct where { <employees/1370> ?p ?o }"
								))
						)
				)
		);
	}

}
