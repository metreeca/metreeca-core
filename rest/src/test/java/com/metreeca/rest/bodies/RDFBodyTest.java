/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.bodies;


import com.metreeca.form.Form;
import com.metreeca.form.things.Codecs;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.rest.ResponseAssert;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.function.Supplier;

import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.bodies.InputBody.input;
import static com.metreeca.rest.bodies.RDFBody.rdf;
import static com.metreeca.rest.bodies.TextBody.text;

import static org.junit.jupiter.api.Assertions.fail;


final class RDFBodyTest {

	@Test void testHandleMissingInput() {
		new Request().body(rdf()).use(
				value -> assertThat(value).isEmpty(),
				error -> fail("unexpected error {"+error+"}")
		);
	}

	@Test void testHandleEmptyInput() {
		new Request().body(input(), (Supplier<InputStream>)Codecs::input).body(rdf()).use(
				value -> assertThat(value).isEmpty(),
				error -> fail("unexpected error {"+error+"}")
		);
	}


	@Test void testConfigureWriterBaseIRI() {
		new Request()

				.base(ValuesTest.Base+"context/")
				.path("/container/")

				.reply(response -> response
						.status(Response.OK)
						.shape(field(LDP.CONTAINS, datatype(Form.IRIType)))
						.body(rdf(), decode("</context/container/> ldp:contains </context/container/x>."))
				)

				.accept(response -> ResponseAssert.assertThat(response)
						.hasBody(text(), text -> Assertions.assertThat(text)
								.contains("@base <" +ValuesTest.Base+"context/container/"+ ">")
						)
				);
	}

}
