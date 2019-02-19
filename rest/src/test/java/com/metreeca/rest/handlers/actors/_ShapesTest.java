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

package com.metreeca.rest.handlers.actors;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;

import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Shape.relate;
import static com.metreeca.form.Shape.required;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.rest.handlers.actors._Shapes.container;
import static com.metreeca.rest.handlers.actors._Shapes.entity;
import static com.metreeca.rest.handlers.actors._Shapes.resource;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toSet;


@Nested final class _ShapesTest {

	@Nested final class Splitters {

		private final Shape Container=and(
				relate().then(
						field(RDF.TYPE, LDP.DIRECT_CONTAINER),
						field(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
						field(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
				),
				field(RDFS.LABEL, Textual),
				field(RDFS.COMMENT, Textual)
		);


		@Nested final class Entity {

			@Test void testForwardAndAnnotateComboShape() {
				assertThat(entity(Employees))

						.satisfies(shape -> assertThat(fields(shape).keySet())
								.as("all fields retained")
								.isEqualTo(fields(Employees.map(new Optimizer())).keySet())
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						)

						.satisfies(shape -> assertThat(pass(shape.map(new Redactor(Form.role, Form.none))))
								.as("role-based authorization preserved")
								.isTrue()
						);
			}

			@Test void testForwardAndAnnotateContainerShape() {
				assertThat(entity(Container))

						.satisfies(shape -> assertThat(fields(shape))
								.as("only container fields retained")
								.isEqualTo(fields(Container.map(new Optimizer())))
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						);
			}

			@Test void testForwardResourceShape() {
				assertThat(entity(Employee))
						.as("only resource shape found")
						.isEqualTo(Employee.map(new Optimizer()));
			}

		}

		@Nested final class Resource {

			@Test void testExtractAndAnnotateResourceShapeFromComboShape() {
				assertThat(resource(Employees))

						.satisfies(shape -> assertThat(fields(shape))
								.as("only resource fields retained")
								.isEqualTo(fields(Employee.map(new Optimizer())))
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						);
			}

			@Test void testForwardAndAnnotateContainerShape() {
				assertThat(resource(Container))

						.satisfies(shape -> assertThat(fields(shape))
								.as("only container fields retained")
								.isEqualTo(fields(Container.map(new Optimizer())))
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						);
			}

			@Test void testForwardResourceShape() {
				assertThat(resource(Employee))
						.as("only resource shape found")
						.isEqualTo(Employee.map(new Optimizer()));
			}

			@Test void testPreserveExistingAnnotations() {

				final Shape shape=and(meta(RDF.TYPE, LDP.BASIC_CONTAINER));

				assertThat(resource(shape)).isEqualTo(meta(RDF.TYPE, LDP.BASIC_CONTAINER));

			}

		}

		@Nested final class Container {

			@Test void testExtractAndAnnotateContainerShapeFromComboShape() {
				assertThat(container(Employees))

						.satisfies(shape -> assertThat(fields(shape).keySet())
								.as("only container fields retained")
								.isEqualTo(fields(Employees)
										.keySet().stream()
										.filter(iri -> !iri.equals(LDP.CONTAINS))
										.collect(toSet())
								)
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
								)
						);
			}

			@Test void testIgnoreResourceShape() {
				assertThat(container(Employee))
						.as("no container shape found")
						.isEqualTo(pass());

			}

			@Test void testPreserveExistingAnnotations() {

				final Shape shape=and(meta(RDF.TYPE, LDP.BASIC_CONTAINER), field(LDP.CONTAINS, required()));

				assertThat(container(shape)).isEqualTo(meta(RDF.TYPE, LDP.BASIC_CONTAINER));

			}

		}

	}

}
