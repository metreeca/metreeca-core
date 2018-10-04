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

package com.metreeca.rest.handlers.shape;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.things.ValuesTest.small;
import static com.metreeca.rest.HandlerAssert.dataset;
import static com.metreeca.rest.ResponseAssert.assertThat;


final class ActorTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(dataset(small()))
				.exec(task)
				.clear();
	}


	//// Direct ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testDirectEnforceRoleBasedAccessControl() {
		exec(() -> access(RDF.NIL).accept(response2 -> assertThat(response2).hasStatus(Response.OK)));
		exec(() -> access(RDF.FIRST, RDF.FIRST).accept(response1 -> assertThat(response1).hasStatus(Response.OK)));
		exec(() -> access(RDF.REST, RDF.FIRST).accept(response -> assertThat(response).hasStatus(Response.Unauthorized)));
	}


	//// Driven ////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Disabled @Test void testDrivenEnforceRoleBasedAccessControl() { }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Responder access(final IRI effective, final IRI... permitted) {
		return new TestActor().roles(permitted).handle(new Request().roles(effective));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestActor extends Actor<TestActor> {

		private TestActor() { super(Form.relate, Form.detail); }

		@Override protected Responder shaped(final Request request, final Shape shape) {
			return request.reply(response -> response.status(Response.OK));
		}

		@Override protected Responder direct(final Request request) {
			return request.reply(response -> response.status(Response.OK));
		}

	}

}
