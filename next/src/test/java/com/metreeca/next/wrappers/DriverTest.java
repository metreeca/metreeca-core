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

package com.metreeca.next.wrappers;

import com.metreeca.form.Shape;
import com.metreeca.next.Request;
import com.metreeca.next.formats._RDF;
import com.metreeca.next.formats._Shape;

import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.next.Response.OK;

import static org.junit.jupiter.api.Assertions.*;


final class DriverTest {

	private static final Shape TestShape=and();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testIgnoreUndefinedShape() {
		new Driver()

				.wrap((Request request) -> {

					assertFalse(request.body(_Shape.Format).isPresent());

					return request.reply(response -> response);

				})

				.handle(new Request())

				.accept(response -> {

					assertFalse(response.header("link").isPresent());
					assertFalse(response.body(_Shape.Format).isPresent());

				});
	}

	@Test void testConfigureExchangeShape() {
		new Driver().shape(TestShape)

				.wrap((Request request) -> {

					assertEquals(TestShape, request.body(_Shape.Format).orElseGet(() -> fail("missing shape")));

					return request.reply(response -> response.header("link", "existing"));

				})

				.handle(new Request()
						.base("http://example.org/")
						.path("/resource"))

				.accept(response -> {

					assertIterableEquals(
							list("existing", "<http://example.org/resource?specs>; rel="+LDP.CONSTRAINED_BY),
							response.headers("link")
					);

					assertEquals(TestShape, response.body(_Shape.Format).orElseGet(() -> fail("missing shape")));

				});
	}

	@Test void testHandleSpecsQuery() {
		new Driver().shape(TestShape)

				.wrap((Request request) -> request.reply(response -> response))

				.handle(new Request()
						.method(Request.GET)
						.base("http://example.org/")
						.path("/resource")
						.query("specs"))

				.accept(response -> {

					assertEquals(OK, response.status());

					assertTrue(new LinkedHashModel(response.body(_RDF.Format).orElseGet(() -> fail("missing RDF body")))
							.contains(response.item(), LDP.CONSTRAINED_BY, null));

				});
	}

}
