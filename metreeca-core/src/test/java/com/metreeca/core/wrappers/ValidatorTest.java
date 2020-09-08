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

package com.metreeca.core.wrappers;

import com.metreeca.core.*;

import org.junit.jupiter.api.Test;

import static com.metreeca.core.ResponseAssert.assertThat;
import static com.metreeca.core.formats.OutputFormat.output;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;


final class ValidatorTest {

	private void exec(final Runnable... tasks) {
		new Context()
				.exec(tasks)
				.clear();
	}

	private Handler handler() {
		return request -> request.reply(response -> response.status(Response.OK));
	}


	@Test void testAcceptValidRequests() {
		exec(() -> Validator.validator(request -> emptyList())

				.wrap(handler())

				.handle(new Request())

				.accept(response -> assertThat(response)
						.hasStatus(Response.OK)
				)
		);
	}

	@Test void testRejectInvalidRequests() {
		exec(() -> Validator.validator(request -> asList("issue", "issue"))

				.wrap(handler())

				.handle(new Request())

				.accept(response -> assertThat(response)
						.hasStatus(Response.UnprocessableEntity)
						.hasBody(output())
				)
		);
	}

}
