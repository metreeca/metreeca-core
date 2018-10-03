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
import com.metreeca.form.Query;
import com.metreeca.rest.Request;
import com.metreeca.rest.Responder;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Test;

import static com.metreeca.rest.ResponseAssert.assertThat;


final class ActorTest {

	// !!! test on shapes

	@Test void testDirectEnforceRoleBasedAccessControl() {
		exec(() -> acccess(RDF.NIL).accept(response -> assertThat(response).hasStatus(Response.OK)));
		exec(() -> acccess(RDF.FIRST, RDF.FIRST).accept(response -> assertThat(response).hasStatus(Response.OK)));
		exec(() -> acccess(RDF.REST, RDF.FIRST).accept(response -> assertThat(response).hasStatus(Response.Unauthorized)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private void exec(final Runnable task) {
		new Tray().exec(task).clear();
	}

	private Responder acccess(final IRI effective, final IRI... permitted) {
		return new TestActor().roles(permitted).handle(new Request().roles(effective));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TestActor extends Actor<TestActor> {

		private TestActor() { super(Form.relate, Form.detail); }

		@Override protected Responder shaped(final Request request, final Query query) {
			return request.reply(response -> response.status(Response.OK));
		}

		@Override protected Responder direct(final Request request, final Query query) {
			return request.reply(response -> response.status(Response.OK));
		}

	}

}
