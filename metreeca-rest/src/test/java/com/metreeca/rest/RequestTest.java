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

package com.metreeca.rest;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.Collections.emptySet;


final class RequestTest {

	@Test void testParametersIgnoreEmptyHeaders() {

		final Request request=new Request()
				.parameters("parameter", emptySet());

		assertThat(request.parameters().entrySet()).isEmpty();
	}

	@Test void testParametersOverwritesValues() {

		final Request request=new Request()
				.parameter("parameter", "one")
				.parameter("parameter", "two");

		assertThat(request.parameters("parameter")).containsExactly("two");

	}

}
