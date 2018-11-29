/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.codecs.ShapeCodec;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;

import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.form.Shape.optional;
import static com.metreeca.form.Shape.required;
import static com.metreeca.form.Shape.role;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.rest.RequestAssert.assertThat;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.RDFFormat.rdf;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;


final class DriverTest {

	private static final Shape RootShape=optional();
	private static final Shape NoneShape=required();

	private static final Shape TestShape=and(
			role(set(Form.root), RootShape),
			role(set(Form.none), NoneShape)
	);


	private static Request request() {
		return new Request()
				.user(Form.root)
				.roles(Form.root)
				.method(Request.GET)
				.base("http://example.org/")
				.path("/resource");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testIgnoreUndefinedShape() {
		new Driver()

				.wrap((Handler)request -> {

					assertThat(request).doesNotHaveShape();

					return request.reply(response -> response);

				})

				.handle(request())

				.accept(response -> assertThat(response)
						.doesNotHaveHeader("Link")
						.doesNotHaveShape()
				);
	}

	@Test void testConfigureExchangeShape() {
		new Driver().shape(TestShape)

				.wrap((Handler)request -> {

					assertThat(request).hasShape(TestShape);

					return request.reply(response -> response.header("link", "existing"));

				})

				.handle(request())

				.accept(response -> assertThat(response).hasHeaders("Link",
						"existing", "<http://example.org/resource?specs>; rel="+LDP.CONSTRAINED_BY
				));
	}

	@Test void testHandleSpecsQuery() {
		new Driver().shape(TestShape)

				.wrap((Handler)request -> request.reply(response -> response))

				.handle(request().query("specs"))

				.accept(response -> {

					assertEquals(OK, response.status());

					final Model model=new LinkedHashModel(response
							.body(rdf())
							.value()
							.orElseGet(() -> fail("missing RDF body"))
					);

					final Optional<Resource> specs=model
							.filter(response.item(), LDP.CONSTRAINED_BY, null)
							.objects()
							.stream()
							.map(v -> (Resource)v)
							.findFirst();

					final Optional<Resource> relate=specs.flatMap(s -> model
							.filter(s, Form.relate, null)
							.objects()
							.stream()
							.map(v -> (Resource)v)
							.findFirst()
					);

					final Optional<Shape> shape=relate.map(r -> new ShapeCodec().decode(r, model));

					assertTrue(specs.isPresent());
					assertTrue(relate.isPresent());

					assertEquals(RootShape, shape.orElseGet(() -> fail("missing relate specs")));

				});
	}

}
