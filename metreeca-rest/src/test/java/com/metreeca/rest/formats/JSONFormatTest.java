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

package com.metreeca.rest.formats;

import com.metreeca.rest.EitherAssert;
import com.metreeca.rest.Request;

import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.json.Json;
import javax.json.JsonObject;
import java.io.*;

import static java.nio.charset.StandardCharsets.UTF_8;


final class JSONFormatTest {

	private static final JsonObject TestJSON=Json.createObjectBuilder()
			.add("one", 1)
			.add("two", 2)
			.build();


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testRetrieveJSON() {

		final Request request=new Request()
				.header("content-type", JSONFormat.MIME)
				.body(InputFormat.input(), () -> new ByteArrayInputStream(TestJSON.toString().getBytes(UTF_8)));

		EitherAssert.assertThat(request.body(JSONFormat.json()))
				.hasRight(TestJSON);
	}

	@Test void testRetrieveJSONChecksContentType() {

		final Request request=new Request()
				.body(InputFormat.input(), () -> new ByteArrayInputStream(TestJSON.toString().getBytes(UTF_8)));

		EitherAssert.assertThat(request.body(JSONFormat.json()))
				.hasLeft();
	}

	@Test void testConfigureJSON() {

		final Request request=new Request().body(JSONFormat.json(), TestJSON);

		EitherAssert.assertThat

				(request

						.body(OutputFormat.output())

						.map(target -> {
							try ( final ByteArrayOutputStream output=new ByteArrayOutputStream() ) {

								target.accept(output);

								return new ByteArrayInputStream(output.toByteArray());

							} catch ( final IOException e ) {
								throw new UncheckedIOException(e);
							}
						})

						.map(input -> Json.createReader(input).readObject())

				)

				.hasRight(TestJSON);

	}

	@Test void testConfigureJSONSetsContentType() {

		final Request request=new Request().body(JSONFormat.json(), TestJSON);

		Assertions.assertThat(request.header("content-type"))
				.isPresent()
				.contains(JSONFormat.MIME);

	}

}
