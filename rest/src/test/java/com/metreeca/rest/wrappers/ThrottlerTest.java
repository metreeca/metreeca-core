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
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Collection;

import javax.json.JsonValue;

import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.statement;
import static com.metreeca.form.things.ValuesTest.decode;
import static com.metreeca.form.truths.JSONAssert.assertThat;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.ResponseAssert.assertThat;
import static com.metreeca.rest.formats.JSONFormat.json;
import static com.metreeca.rest.formats.RDFFormat.rdf;

import static org.assertj.core.api.Assertions.assertThat;


final class ThrottlerTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(task)
				.clear();
	}


	private Request request() {
		return new Request().base(ValuesTest.Base);
	}

	private Handler handler() {
		return request -> request.reply(response -> response.status(Response.OK));
	}

	private Handler handler(final Collection<Statement> model) {
		return request -> request.reply(response -> response.status(Response.OK).body(rdf(), model));
	}

	private Handler handler(final Collection<Statement> model, final Shape shape) {
		return request -> request.reply(response -> response.status(Response.OK).shape(shape).body(rdf(), model));
	}


	@Nested final class Simple {

		private Throttler throttler() {
			return new Throttler(Form.any, Form.any);
		}


		@Test void testAcceptEmptyRequestPayload() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request())

					.accept(response -> assertThat(response).hasStatus(Response.OK))
			);
		}

		@Test void testAcceptDescriptionRequestPayload() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request().body(rdf(), decode("<> rdf:value rdf:nil.")))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)
			);
		}

		@Test void testRejectExceedingRequestPayload() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request().body(rdf(), decode("<> rdf:value rdf:nil. rdf:first rdf:value rdf:rest.")))

					.accept(response -> assertThat(response)
							.hasStatus(Response.UnprocessableEntity)
							.hasBody(json(), json -> assertThat((JsonValue)json).hasField("error"))
					)
			);
		}


		@Test void testTrimDisconnectedResponsePayload() {
			exec(() -> throttler()

					.wrap(handler(decode("<> rdf:value rdf:nil. rdf:first rdf:value rdf:rest.")))

					.handle(request())

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(rdf(), rdf -> assertThat(rdf)
									.isIsomorphicTo(statement(response.item(), RDF.VALUE, RDF.NIL))
							)
					)
			);

		}

	}

	@Nested final class Shaped {

		private final Shape shape=ValuesTest.Employee;

		private final IRI task=Form.relate;
		private final IRI view=Form.detail;
		private final IRI role=ValuesTest.Salesman;


		private Throttler throttler() {
			return new Throttler(task, view);
		}

		private Request request() {
			return ThrottlerTest.this.request()
					.shape(shape)
					.roles(role);
		}


		@Test void testRedactRequestShape() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request())

					.accept(response -> assertThat(response.request().shape())
							.isEqualTo(shape.map(new Redactor(map(
									entry(Form.task, set(task)),
									entry(Form.view, set(view)),
									entry(Form.role, set(role))
							))).map(new Optimizer()))
					)
			);
		}

		@Test void testRejectUnauthorizedRequests() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request().roles(Form.none))

					.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))
			);
		}

		@Test void testRejectUnauthorizedRequestsIgnoringAnnotations() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request().roles(Form.none).shape(and(shape, meta(RDF.VALUE, RDF.NIL))))

					.accept(response -> assertThat(response).hasStatus(Response.Unauthorized))
			);
		}

		@Test void testRejectForbiddenRequests() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request().user(RDF.NIL).roles(Form.none))

					.accept(response -> assertThat(response).hasStatus(Response.Forbidden))
			);
		}


		@Test void testAcceptEmptyRequestPayload() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request())

					.accept(response -> assertThat(response).hasStatus(Response.OK))
			);
		}

		@Test void testAcceptCompatibleRequestPayload() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request().body(rdf(), decode("<> :email 'tino.faussone@example.com'.")))

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
					)
			);
		}

		@Test void testRejectExceedingRequestPayload() {
			exec(() -> throttler()

					.wrap(handler())

					.handle(request().body(rdf(), decode("<> :seniority 5 .")))

					.accept(response -> assertThat(response)
							.hasStatus(Response.UnprocessableEntity)
							.hasBody(json(), json -> assertThat((JsonValue)json).hasField("error"))
					)
			);
		}


		@Test void testRedactResponseShape() {
			exec(() -> throttler()

					.wrap(handler(decode(""), shape))

					.handle(request())

					.accept(response -> assertThat(response.request().shape())
							.isEqualTo(shape.map(new Redactor(map(
									entry(Form.task, set(task)),
									entry(Form.view, set(view)),
									entry(Form.role, set(role))
							))).map(new Optimizer()))
					)
			);
		}

		@Test void testTrimExceedingResponsePayload() {
			exec(() -> throttler()

					.wrap(handler(decode("<> :email 'tino.faussone@example.com'; :seniority 5 ."), shape))

					.handle(request())

					.accept(response -> assertThat(response)
							.hasStatus(Response.OK)
							.hasBody(rdf(), rdf -> assertThat(rdf)
									.isIsomorphicTo(decode("<> :email 'tino.faussone@example.com'."))
							)
					)
			);

		}

	}

}
