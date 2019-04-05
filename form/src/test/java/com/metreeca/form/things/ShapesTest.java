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

package com.metreeca.form.things;

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Outliner;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.truths.ModelAssert;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Shape.relate;
import static com.metreeca.form.Shape.required;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Field.fields;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.Meta.metas;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Shapes.container;
import static com.metreeca.form.things.Shapes.entity;
import static com.metreeca.form.things.Shapes.resource;
import static com.metreeca.form.things.ValuesTest.*;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


@Nested final class ShapesTest {

	public static final IRI resource=item("employees/1370");


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

	@Nested final class Mergers {

		private Shape redact(final Shape shape) {
			return shape
					.map(new Redactor(Form.task))
					.map(new Redactor(Form.view))
					.map(new Redactor(Form.mode))
					.map(new Redactor(Form.role))
					.map(new Optimizer());
		}


		@Nested final class Resource {

			@Test void testAnchorToResource() {
				assertThat(all(redact(resource(resource, Employee))))
						.isPresent()
						.hasValueSatisfying(values -> assertThat(values)
								.containsOnly(resource)
						);
			}

		}

		@Nested final class Container {

			private final IRI container=item("employees");


			@Test void testAnchorToBasicContainer() {

				final Shape basic=redact(container(container, and(
						field(LDP.CONTAINS, Employee)
				)));

				ModelAssert.assertThat(basic.map(new Outliner(resource)).collect(toList()))
						.hasStatement(container, LDP.CONTAINS, resource);
			}

			@Test void testAnchorToDirectContainer() {

				final Shape hasMemberRelation=redact(container(container, and(
						meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
						meta(LDP.HAS_MEMBER_RELATION, RDF.VALUE),
						field(LDP.CONTAINS, Employee)
				)));

				ModelAssert.assertThat(hasMemberRelation.map(new Outliner(resource)).collect(toList()))
						.hasStatement(container, RDF.VALUE, resource);

				final Shape isMemberOfRelation=redact(container(container, and(
						meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
						meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
						meta(LDP.MEMBERSHIP_RESOURCE, term("Employee")),
						field(LDP.CONTAINS, Employee)
				)));

				ModelAssert.assertThat(isMemberOfRelation.map(new Outliner(resource)).collect(toList()))
						.hasStatement(resource, RDF.TYPE, term("Employee"));
			}

			@Test void testAnchorToDirectContainerIgnoringFields() {

				final Shape direct=redact(container(container, and(
						meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
						meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
						meta(LDP.MEMBERSHIP_RESOURCE, term("Employee")),
						field(LDP.CONTAINS, and(Employee, field(RDF.TYPE, all(term("Employee")))))
				)));

				ModelAssert.assertThat(direct.map(new Outliner(resource)).collect(toList()))
						.hasStatement(resource, RDF.TYPE, term("Employee"));
			}

			@Test void testAnchorToDirectNestedContainer() {

				final Shape direct=redact(container(item("employees/1370/customers/"), and(
						meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
						meta(LDP.HAS_MEMBER_RELATION, term("customer")),
						meta(LDP.MEMBERSHIP_RESOURCE, LDP.RESOURCE),
						field(LDP.CONTAINS, Employee)
				)));

				ModelAssert.assertThat(direct.map(new Outliner(item("/customer/123"))).collect(toList()))
						.hasStatement(item("employees/1370"), term("customer"), item("/customer/123"));
			}

		}

	}

}
