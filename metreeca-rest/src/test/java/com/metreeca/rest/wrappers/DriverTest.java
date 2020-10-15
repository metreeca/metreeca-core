/*
 * Copyright © 2013-2020 Metreeca srl
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

package com.metreeca.rest.wrappers;

import com.metreeca.json.Shape;
import com.metreeca.rest.*;
import com.metreeca.rest.formats.JSONLDFormat;

import org.junit.jupiter.api.Test;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Guard.filter;
import static com.metreeca.json.shapes.Guard.role;
import static com.metreeca.json.shapes.MinCount.minCount;
import static com.metreeca.json.shapes.When.when;
import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.OK;
import static org.assertj.core.api.Assertions.assertThat;


final class DriverTest {

	private static final String Root="root";
	private static final String None="none";

	private static final Shape RootShape=Shape.optional();
	private static final Shape NoneShape=Shape.required();

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

					RequestAssert.assertThat(request)
							.hasAttribute(JSONLDFormat.shape(), shape -> assertThat(shape).isEqualTo(TestShape));

					return request.reply(status(OK));

				})

				.handle(request())

				.accept(response -> ResponseAssert.assertThat(response)
						.hasStatus(OK)
				);
	}

}
