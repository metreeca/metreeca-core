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

import com.metreeca.form.Form;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.truths.JSONAssert.assertThat;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.RDFFormat.rdf;


final class ModulatorTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(task)
				.clear();
	}


	private Request request() {
		return new Request()
				.base(ValuesTest.Base);
	}

	private Handler echo() {
		return request -> request.reply(response -> response
				.status(Response.OK)
				.body(rdf(), response.request().body(rdf()).value().orElse(set())) // echo request body
		);
	}


	@Nested final class Simple {

		@Test void testAcceptPublicRequests() {
			exec(() -> new Modulator()

					.wrap(echo())

					.handle(request().roles(Form.none))

					.accept(response -> assertThat(response).hasStatus(Response.OK))

			);
		}

		@Test void testAcceptAuthorizedRequests() {
			exec(() -> new Modulator()

					.role(Form.root)

					.wrap(echo())

					.handle(request().roles(Form.root))

					.accept(response -> assertThat(response).hasStatus(Response.OK))

			);
		}

		@Test void testRejectUnauthorizedRequests() {
			exec(() -> new Modulator()

					.role(Form.root)

					.wrap(echo())

					.handle(request())

					.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))

			);
		}

		@Test void testRejectForbiddenRequests() {
			exec(() -> new Modulator()

					.role(Form.root)

					.wrap(echo())

					.handle(request().user(RDF.NIL))

					.accept(response -> assertThat(response).hasStatus(Response.Forbidden))

			);
		}


		@Test void testAcceptEmptyRequestPayload() {
			exec(() -> new Modulator()

					.wrap(echo())

					.handle(request())

					.accept(response -> assertThat(response).hasStatus(Response.OK))
			);
		}

		@Test void testAcceptConnectedRequestPayload() {
			exec(() -> new Modulator()

					.wrap(echo())

					.handle(request()
							.body(rdf(), decode("<> rdf:value rdf:nil."))
					)

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(rdf(), rdf -> assertThat(rdf)
									.hasStatement(response.item(), RDF.VALUE, RDF.NIL)
							)
					)
			);
		}

		@Test void testRejectDisconnectedRequestPayload() {
			exec(() -> new Modulator()

					.wrap(echo())

					.handle(request().body(rdf(), decode("<> rdf:value rdf:nil. rdf:first rdf:value rdf:rest.")))

					.accept(response -> assertThat(response)
							.hasStatus(Response.UnprocessableEntity)
							.hasBody(json(), json -> assertThat(json).hasField("error"))
					)
			);
		}


		@Test void testTrimDisconnectedResponsePayload() {
			exec(() -> new Modulator()

					.wrap((Request request) -> request.reply(response -> response
							.status(Response.OK)
							.body(rdf(), decode("<> rdf:value rdf:nil. rdf:first rdf:value rdf:rest."))
					))

					.handle(request())

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(rdf(), rdf -> assertThat(rdf)
									.hasStatement(response.item(), RDF.VALUE, RDF.NIL)
									.doesNotHaveStatement(RDF.FIRST, RDF.VALUE, RDF.REST)
							)
					)
			);

		}

	}

	@Nested final class Driven {

		// !!! redaction => mode == verify

		//@Test void testTrimRequestRDFPayloadToRequestShape() {
		//	exec(() -> new Processor()
		//
		//			.pre((response, model) -> {
		//
		//				model.add(response.item(), RDF.FIRST, RDF.NIL);
		//				model.add(response.item(), RDF.REST, RDF.NIL);
		//
		//				return model;
		//
		//			})
		//
		//			.wrap(echo())
		//
		//			.handle(new Request()
		//
		//					.shape(trait(RDF.FIRST))
		//					.body(rdf(), emptyList())) // empty body to activate pre-processing
		//
		//			.accept(response -> assertThat(response)
		//					.hasBody(rdf(), rdf -> ModelAssert.assertThat(rdf)
		//							.as("statements outside shape envelope trimmed")
		//							.isIsomorphicTo(statement(response.item(), RDF.FIRST, RDF.NIL))
		//					)
		//			)
		//	);
		//}

		//@Test void testTrimResponseRDFPayloadToResponseShape() {
		//	exec(() -> new Processor()
		//
		//			.post((response, model) -> {
		//
		//				model.add(response.item(), RDF.FIRST, RDF.NIL);
		//				model.add(response.item(), RDF.REST, RDF.NIL);
		//
		//				return model;
		//
		//			})
		//
		//			.wrap(echo())
		//
		//			.handle(new Request()
		//
		//					.shape(trait(RDF.FIRST))
		//					.body(rdf(), emptyList())) // empty body to activate post-processing
		//
		//			.accept(response -> assertThat(response)
		//					.hasBody(rdf(), rdf -> ModelAssert.assertThat(rdf)
		//							.as("statements outside shape envelope trimmed")
		//							.isIsomorphicTo(statement(response.item(), RDF.FIRST, RDF.NIL))
		//					)
		//			)
		//	);
		//}
	}

}
