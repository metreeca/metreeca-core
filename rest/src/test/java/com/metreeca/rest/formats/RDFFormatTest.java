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

package com.metreeca.rest.formats;


import com.metreeca.form.things.Codecs;
import com.metreeca.rest.Request;

import org.junit.jupiter.api.Test;

import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.formats.InputFormat.input;
import static com.metreeca.rest.formats.RDFFormat.rdf;

import static org.junit.jupiter.api.Assertions.fail;


final class RDFFormatTest {

	@Test void testHandleMissingInput() {
		new Request().body(rdf()).fold(
				value -> assertThat(value).isEmpty(),
				error -> fail("unexpected error {"+error+"}")
		);
	}

	@Test void testHandleEmptyInput() {
		new Request().body(input(), Codecs::input).body(rdf()).fold(
				value -> assertThat(value).isEmpty(),
				error -> fail("unexpected error {"+error+"}")
		);
	}

}
