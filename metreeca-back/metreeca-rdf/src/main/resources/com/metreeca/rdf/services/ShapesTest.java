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

package com.metreeca.rdf.services;

import com.metreeca.rdf.ModelAssert;
import com.metreeca.rdf.Values;
import com.metreeca.rdf.ValuesTest;
import com.metreeca.rdf._probes.Outliner;
import com.metreeca.rdf._probes._Optimizer;
import com.metreeca.tree.Shape;
import com.metreeca.tree.probes.Redactor;
import com.metreeca.tree.shapes.All;
import com.metreeca.tree.shapes.Meta;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.rdf.services.Shapes.container;
import static com.metreeca.rdf.services.Shapes.entity;
import static com.metreeca.rdf.services.Shapes.resource;
import static com.metreeca.tree.Shape.relate;
import static com.metreeca.tree.Shape.required;
import static com.metreeca.tree.probes.Evaluator.pass;
import static com.metreeca.tree.shapes.All.all;
import static com.metreeca.tree.shapes.And.and;
import static com.metreeca.tree.shapes.Field.field;
import static com.metreeca.tree.shapes.Field.fields;
import static com.metreeca.tree.shapes.Meta.meta;
import static com.metreeca.tree.shapes.Meta.metas;
import static com.metreeca.tree.things.ValuesTest.*;

import static org.assertj.core.api.Assertions.assertThat;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


@Nested final class ShapesTest {

	public static final IRI resource=ValuesTest.item("employees/1370");


	@Nested final class Splitters {

		private final Shape Container=and(
				relate().then(
						field(RDF.TYPE, LDP.DIRECT_CONTAINER),
						field(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
						field(LDP.MEMBERSHIP_RESOURCE, ValuesTest.term("Employee"))
				),
				field(RDFS.LABEL, ValuesTest.Textual),
				field(RDFS.COMMENT, ValuesTest.Textual)
		);


		@Nested final class Entity {

			@Test void testForwardAndAnnotateComboShape() {
				assertThat(entity(ValuesTest.Employees))

						.satisfies(shape -> assertThat(fields(shape).keySet())
								.as("all fields retained")
								.isEqualTo(fields(ValuesTest.Employees.map(new _Optimizer())).keySet())
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										Assertions.entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										Assertions.entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										Assertions.entry(LDP.MEMBERSHIP_RESOURCE, ValuesTest.term("Employee"))
								)
						)

						.satisfies(shape -> assertThat(pass(shape.map(new Redactor(Shape.Role, Values.none))))
								.as("role-based authorization preserved")
								.isTrue()
						);
			}

			@Test void testForwardAndAnnotateContainerShape() {
				assertThat(entity(Container))

						.satisfies(shape -> assertThat(fields(shape))
								.as("only container fields retained")
								.isEqualTo(fields(Container.map(new _Optimizer())))
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										Assertions.entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										Assertions.entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										Assertions.entry(LDP.MEMBERSHIP_RESOURCE, ValuesTest.term("Employee"))
								)
						);
			}

			@Test void testForwardResourceShape() {
				assertThat(entity(ValuesTest.Employee))
						.as("only resource shape found")
						.isEqualTo(ValuesTest.Employee.map(new _Optimizer()));
			}

		}

		@Nested final class Resource {

			@Test void testExtractAndAnnotateResourceShapeFromComboShape() {
				assertThat(resource(ValuesTest.Employees))

						.satisfies(shape -> assertThat(fields(shape))
								.as("only resource fields retained")
								.isEqualTo(fields(ValuesTest.Employee.map(new _Optimizer())))
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										Assertions.entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										Assertions.entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										Assertions.entry(LDP.MEMBERSHIP_RESOURCE, ValuesTest.term("Employee"))
								)
						);
			}

			@Test void testForwardAndAnnotateContainerShape() {
				assertThat(resource(Container))

						.satisfies(shape -> assertThat(fields(shape))
								.as("only container fields retained")
								.isEqualTo(fields(Container.map(new _Optimizer())))
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										Assertions.entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										Assertions.entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										Assertions.entry(LDP.MEMBERSHIP_RESOURCE, ValuesTest.term("Employee"))
								)
						);
			}

			@Test void testForwardResourceShape() {
				assertThat(resource(ValuesTest.Employee))
						.as("only resource shape found")
						.isEqualTo(ValuesTest.Employee.map(new _Optimizer()));
			}

			@Test void testPreserveExistingAnnotations() {

				final Shape shape=and(meta(RDF.TYPE, LDP.BASIC_CONTAINER));

				assertThat(resource(shape)).isEqualTo(meta(RDF.TYPE, LDP.BASIC_CONTAINER));

			}

		}

		@Nested final class Container {

			@Test void testExtractAndAnnotateContainerShapeFromComboShape() {
				assertThat(container(ValuesTest.Employees))

						.satisfies(shape -> assertThat(fields(shape).keySet())
								.as("only container fields retained")
								.isEqualTo(fields(ValuesTest.Employees)
										.keySet().stream()
										.filter(iri -> !iri.equals(LDP.CONTAINS))
										.collect(toSet())
								)
						)

						.satisfies(shape -> assertThat(metas(shape))
								.as("annotated with container properties")
								.containsOnly(
										Assertions.entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
										Assertions.entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
										Assertions.entry(LDP.MEMBERSHIP_RESOURCE, ValuesTest.term("Employee"))
								)
						);
			}

			@Test void testIgnoreResourceShape() {
				assertThat(container(ValuesTest.Employee))
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
					.map(new Redactor(Shape.Task, values3 -> true))
					.map(new Redactor(Shape.View, values2 -> true))
					.map(new Redactor(Shape.Mode, values1 -> true))
					.map(new Redactor(Shape.Role, values -> true))
					.map(new _Optimizer());
		}


		@Nested final class Resource {

			@Test void testAnchorToResource() {
				assertThat(all(redact(resource(resource, ValuesTest.Employee))))
						.isPresent()
						.hasValueSatisfying(values -> assertThat(values)
								.containsOnly(resource)
						);
			}

		}

		@Nested final class Container {

			private final IRI container=ValuesTest.item("employees");


			@Test void testAnchorToBasicContainer() {

				final Shape basic=redact(container(container, and(
						field(LDP.CONTAINS, ValuesTest.Employee)
				)));

				ModelAssert.assertThat(basic.map(new Outliner(resource)).collect(toList()))
						.hasStatement(container, LDP.CONTAINS, resource);
			}

			@Test void testAnchorToDirectContainer() {

				final Shape hasMemberRelation=redact(container(container, and(
						meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
						meta(LDP.HAS_MEMBER_RELATION, RDF.VALUE),
						field(LDP.CONTAINS, ValuesTest.Employee)
				)));

				ModelAssert.assertThat(hasMemberRelation.map(new Outliner(resource)).collect(toList()))
						.hasStatement(container, RDF.VALUE, resource);

				final Shape isMemberOfRelation=redact(container(container, and(
						meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
						meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
						Meta.meta(LDP.MEMBERSHIP_RESOURCE, ValuesTest.term("Employee")),
						field(LDP.CONTAINS, ValuesTest.Employee)
				)));

				ModelAssert.assertThat(isMemberOfRelation.map(new Outliner(resource)).collect(toList()))
						.hasStatement(resource, RDF.TYPE, ValuesTest.term("Employee"));
			}

			@Test void testAnchorToDirectContainerIgnoringFields() {

				final Shape direct=redact(container(container, and(
						meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
						meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
						Meta.meta(LDP.MEMBERSHIP_RESOURCE, ValuesTest.term("Employee")),
						field(LDP.CONTAINS, and(ValuesTest.Employee, field(RDF.TYPE, All.all(ValuesTest.term("Employee")))))
				)));

				ModelAssert.assertThat(direct.map(new Outliner(resource)).collect(toList()))
						.hasStatement(resource, RDF.TYPE, ValuesTest.term("Employee"));
			}

			@Test void testAnchorToDirectNestedContainer() {

				final Shape direct=redact(container(ValuesTest.item("employees/1370/customers/"), and(
						meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
						Meta.meta(LDP.HAS_MEMBER_RELATION, ValuesTest.term("customer")),
						meta(LDP.MEMBERSHIP_RESOURCE, LDP.RESOURCE),
						field(LDP.CONTAINS, ValuesTest.Employee)
				)));

				ModelAssert.assertThat(direct.map(new Outliner(ValuesTest.item("/customer/123"))).collect(toList()))
						.hasStatement(ValuesTest.item("employees/1370"), ValuesTest.term("customer"), ValuesTest.item("/customer/123"));
			}

		}

	}

}
