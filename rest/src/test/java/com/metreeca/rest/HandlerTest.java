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

package com.metreeca.rest;

import com.metreeca.rest.formats.TextFormat;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

import static java.util.Arrays.asList;


final class HandlerTest {

	@Test void testResultStreaming() {

		final List<String> transaction=new ArrayList<>();

		final Handler handler=request -> consumer -> {

			transaction.add("begin");

			request.reply(response -> response.body(TextFormat.asText, "inside")).accept(consumer);

			transaction.add("commit");

		};

		handler.handle(new Request()).accept(response -> transaction.add(response.body(TextFormat.asText).value().orElse("")));

		assertEquals(asList("begin", "inside", "commit"), transaction);
	}

}
