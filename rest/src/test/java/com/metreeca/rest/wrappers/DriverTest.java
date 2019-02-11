/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;

import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Shape.optional;
import static com.metreeca.form.Shape.required;
import static com.metreeca.form.Shape.role;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.When.when;
import static com.metreeca.rest.RequestAssert.assertThat;
import static com.metreeca.rest.Response.OK;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.TextFormat.text;


final class DriverTest {

	private static final Shape RootShape=optional();
	private static final Shape NoneShape=required();

	private static final Shape TestShape=and(
			when(role(Form.root), RootShape),
			when(role(Form.none), NoneShape)
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
		new Driver(and())

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
		new Driver(TestShape)

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
		new Driver(TestShape)

				.wrap((Handler)request -> request.reply(response -> response))

				.handle(request().query("specs"))

				.accept(response -> assertThat(response)
						.hasStatus(OK)
						.hasBody(text()));
	}

}
