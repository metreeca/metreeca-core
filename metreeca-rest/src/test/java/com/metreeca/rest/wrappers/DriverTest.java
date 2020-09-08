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

package com.metreeca.rest.wrappers;

import com.metreeca.core.Request;
import com.metreeca.json.Shape;

import org.junit.jupiter.api.Test;

import static com.metreeca.core.MessageException.status;
import static com.metreeca.core.RequestAssert.assertThat;
import static com.metreeca.core.Response.OK;
import static com.metreeca.core.ResponseAssert.assertThat;
import static com.metreeca.json.Shape.*;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.When.when;
import static org.assertj.core.api.Assertions.assertThat;


final class DriverTest {

	private static final String Root="root";
	private static final String None="none";

	private static final Shape RootShape=optional();
	private static final Shape NoneShape=required();

	private static final Shape TestShape=and(
			filter().then(minCount(1)),
			when(role(Root), RootShape),
			when(role(None), NoneShape)
	);


	private static Request request() {
		return new Request()
				.user(Root)
				.roles(None)
				.method(Request.GET)
				.base("http://example.org/")
				.path("/resource");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testConfigureExchangeShape() {
		Driver.driver(TestShape)

				.wrap(request -> {

					assertThat(request)
							.hasAttribute(shape(), shape -> assertThat(shape).isEqualTo(TestShape));

					return request.reply(status(OK));

				})

				.handle(request())

				.accept(response -> assertThat(response)
						.hasStatus(OK)
				);
	}

}
