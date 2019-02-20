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

import com.metreeca.form.Issue;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.junit.jupiter.api.Test;

import static com.metreeca.form.Issue.issue;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.truths.JSONAssert.assertThat;
import static com.metreeca.rest.HandlerTest.echo;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.bodies.JSONBody.json;


final class ValidatorTest {

	private void exec(final Runnable task) {
		new Tray().exec(task).clear();
	}


	@Test void testAcceptValidRequests() {
		exec(() -> new Validator((request, model) -> list(
				issue(Issue.Level.Warning, "failed")
				))

						.wrap(echo())

						.handle(new Request())

						.accept(response -> assertThat(response)
								.hasStatus(Response.OK)
						)
		);
	}

	@Test void testRejectInvalidRequests() {
		exec(() -> new Validator((request, model) -> list(
				issue(Issue.Level.Error, "failed"),
				issue(Issue.Level.Warning, "failed")
				))

						.wrap(echo())

						.handle(new Request())

						.accept(response -> assertThat(response)
								.hasStatus(Response.UnprocessableEntity)
								.hasBody(json(), json -> assertThat(json)
										.hasField("error")
								)
						)
		);
	}

}
