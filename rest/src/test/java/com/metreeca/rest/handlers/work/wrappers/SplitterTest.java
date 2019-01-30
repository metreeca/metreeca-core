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

package com.metreeca.rest.handlers.work.wrappers;

import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Request;
import com.metreeca.rest.Response;
import com.metreeca.tray.Tray;

import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Shape.pass;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.things.ValuesTest.Employees;
import static com.metreeca.form.things.ValuesTest.term;
import static com.metreeca.rest.handlers.work.wrappers.Splitter.container;
import static com.metreeca.rest.handlers.work.wrappers.Splitter.resource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

import static java.util.stream.Collectors.toSet;


final class SplitterTest {

	private void exec(final Runnable task) {
		new Tray().exec(task).clear();
	}


	private Request request() {
		return new Request();
	}

	private Handler handler() {
		return request -> request.reply(response -> response.status(Response.OK).shape(request.shape()));
	}


	@Nested final class ContainerTest {

		@Test void testContainerShape() {
			exec(() -> new Splitter(container()).wrap(handler())

					.handle(request().shape(Employees))

					.accept(response -> assertThat(fields(response.shape()).keySet())
							.as("only container fields retained")
							.isEqualTo(fields(Employees)
									.keySet().stream()
									.filter(iri -> !iri.equals(LDP.CONTAINS))
									.collect(toSet())
							)
					)
			);
		}

		@Test void testResourceShape() {
			exec(() -> new Splitter(container()).wrap(handler())

					.handle(request().shape(ValuesTest.Employee))

					.accept(response -> assertThat(response.shape())
							.as("no container shape found")
							.isEqualTo(pass())
					)
			);
		}

	}

	@Nested final class ResourceTest {

		@Test void testContainerShape() {
			exec(() -> new Splitter(resource()).wrap(handler())

					.handle(request().shape(Employees))

					.accept(response -> {

						assertThat(fields(response.shape()))
								.as("only container fields retained")
								.isEqualTo(fields(ValuesTest.Employee.map(new Optimizer())));

						assertThat(metas(response.shape()))
								.as("annotated with container properties")
								.containsOnly(
										entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								);
					})
			);
		}

		@Test void testResourceShape() {
			exec(() -> new Splitter(resource()).wrap(handler())

					.handle(request().shape(ValuesTest.Employee))

					.accept(response -> assertThat(response.shape())
							.as("only resource shape found")
							.isEqualTo(ValuesTest.Employee.map(new Optimizer()))
					)
			);
		}

	}

}
