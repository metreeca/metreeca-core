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

package com.metreeca.rest.engines;

import com.metreeca.form.*;
import com.metreeca.form.probes.Cleaner;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.queries.Items;
import com.metreeca.form.queries.Stats;
import com.metreeca.rest.Engine;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.XMLSchema;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.form.Shape.filter;
import static com.metreeca.form.Shape.required;
import static com.metreeca.form.Shape.verify;
import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.queries.Items.items;
import static com.metreeca.form.queries.Stats.stats;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Datatype.datatype;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.shapes.Pattern.pattern;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.tray.Tray.tool;

import static org.assertj.core.api.Assertions.assertThat;


final class ShapedContainerTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(graph(dataset()))
				.exec(task)
				.clear();
	}


	private Model dataset() {
		return small();
	}

	private Shape shape() {
		return and(
				verify().then(field(term("code"), and(required(), datatype(XMLSchema.STRING), pattern("\\d{4}")))),
				filter().then(field(RDF.TYPE, term("Employee"))) // should not appear in output shapes
		);
	}


	@Nested final class Basic {

		private final IRI container=item("/employees-basic/");


		private Shape shape() {
			return and(
					meta(RDF.TYPE, LDP.BASIC_CONTAINER),
					field(LDP.CONTAINS, ShapedContainerTest.this.shape())
			);
		}

		private Engine engine() {
			return new ShapedContainer(tool(Graph.Factory), shape());
		}


		@Nested final class Browse {

			// !!! redact / cache final shape
			//		.map(new Redactor(Form.mode, Form.verify)) // hide filtering constraints
			//		.map(new Optimizer())


			@Test void testBrowse() {
				exec(() -> assertThat(engine().browse(container))

						.as("item descriptions linked to container")
						.hasSubset(decode("<employees-basic/> ldp:contains <employees/1370>, <employees/1166>."))

						.as("item descriptions included")
						.hasSubset(decode("<employees/1370> :code '1370'. <employees/1166> :code '1166'."))

						.as("out of shape properties excluded")
						.doesNotHaveSubset(decode("<employees/1370> a :Employee. <employees/1166> a :Employee."))
				);
			}

			@Test void testBrowseEdges() {
				exec(() -> assertThat(engine().browse(container,

						shape -> Value(edges(and(shape, filter().then(
								field(term("title"), all(literal("President"))))
						))),

						(shape, model) -> {

							assertThat(shape.map(new Cleaner()).map(new Optimizer()))
									.as("container+resource shape ignoring metadata")
									.isEqualTo(shape().map(new Cleaner()).map(new Optimizer()));

							assertThat(model).isIsomorphicTo(decode(""
									+"<employees-basic/> ldp:contains <employees/1002>. "
									+"<employees/1002> :code '1002'."
							));

							return model;

						}

						).value()).isPresent()
				);
			}

			@Test void testBrowseStats() {
				exec(() -> assertThat(engine().browse(container,

						shape -> Value(stats(shape, list(term("title")))),

						(shape, model) -> {

							assertThat(shape)
									.as("query-specific shape")
									.isEqualTo(Stats.Shape);

							assertThat(model)
									.as("query-specific payload")
									.hasStatement(container, Form.count, null)
									.hasStatement(container, Form.stats, XMLSchema.STRING)
									.hasStatement(XMLSchema.STRING, Form.count, null);

							return model;

						}

						).value()).isPresent()
				);
			}

			@Test void testBrowseItems() {
				exec(() -> assertThat(engine().browse(container,

						shape -> Value(items(shape, list(term("title")))),

						(shape, model) -> {

							assertThat(shape)
									.as("query-specific shape")
									.isEqualTo(Items.Shape);

							assertThat(model)
									.as("query-specific payload")
									.hasStatement(container, Form.items, null)
									.hasStatement(null, Form.value, literal("President"));

							return model;

						}

						).value()).isPresent()
				);
			}

		}


		@Nested final class Create {

			private final IRI resource=item("/employees-basic/9999");


			@Test void testCreate() {
				exec(() -> {

					final Optional<Focus> report=engine().create(container, resource, decode(
							"<> :code '9999' .",
							resource.toString()
					));

					assertThat(report)
							.isPresent()
							.hasValueSatisfying(focus -> assertThat(focus.assess(Issue.Level.Error))
									.as("success reported")
									.isFalse()
							);

					assertThat(graph())
							.as("graph updated")
							.hasSubset(decode(""
									+"<employees-basic/> ldp:contains <employees-basic/9999>."
									+"<employees-basic/9999> :code '9999' ."
							));

				});
			}

			@Test void testExceedingData() {
				exec(() -> {

					final Optional<Focus> report=engine().create(container, resource, decode(
							"<> :code '9999'; rdf:value rdf:nil.", resource.toString()
					));

					assertThat(report)
							.isPresent()
							.hasValueSatisfying(focus -> assertThat(focus.assess(Issue.Level.Error))
									.as("failure reported")
									.isTrue()
							);

					assertThat(graph())
							.as("graph unaltered")
							.isIsomorphicTo(dataset());

				});
			}

			@Test void testConflict() {
				exec(() -> {

					final Optional<Focus> report=engine().create(container, item("employees/1370"), decode(
							"<> :code '1370'.", resource.toString()
					));

					assertThat(report)
							.as("conflict detected")
							.isNotPresent();

					assertThat(graph())
							.as("graph unaltered")
							.isIsomorphicTo(dataset());

				});
			}

			@Test void testMalformedData() {
				exec(() -> {

					final Optional<Focus> report=engine().create(container, resource, decode(
							"<> :code 'xxxx'.", resource.toString()
					));

					assertThat(report)
							.isPresent()
							.hasValueSatisfying(focus -> assertThat(focus.assess(Issue.Level.Error))
									.as("failure reported")
									.isTrue()
							);

					assertThat(graph())
							.as("graph unaltered")
							.isIsomorphicTo(dataset());

				});
			}

		}

	}

	@Nested final class Direct {

		private Engine engine() {
			return new ShapedContainer(tool(Graph.Factory), and(
					meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
					meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
					meta(LDP.MEMBERSHIP_RESOURCE, term("Employee")),
					field(LDP.CONTAINS, shape())
			));
		}


		@Nested final class Relate {

		}

		@Nested final class Create {

			private final IRI container=item("/employees/");
			private final IRI resource=item("/employees/9999");


			@Test void testCreate() {
				exec(() -> {

					final Optional<Focus> report=engine().create(container, resource, decode(
							"<> :code '9999'.", resource.toString()
					));

					assertThat(report)
							.isPresent()
							.hasValueSatisfying(focus -> assertThat(focus.assess(Issue.Level.Error))
									.as("success reported")
									.isFalse()
							);

					assertThat(graph())
							.as("graph updated")
							.hasSubset(decode(
									"<employees/9999> a :Employee; :code '9999'."
							));

				});
			}

			@Test void testConflict() {
				exec(() -> {

					final Optional<Focus> report=engine().create(container, item("employees/1370"), decode(
							"<> :code '1370'.", resource.toString()
					));

					assertThat(report)
							.as("conflict detected")
							.isNotPresent();

					assertThat(graph())
							.as("graph unaltered")
							.isIsomorphicTo(dataset());

				});
			}

			@Test void testExceedingData() {
				exec(() -> {

					final Optional<Focus> report=engine().create(container, resource, decode(
							"<> :code '9999'; rdf:value rdf:nil.", resource.toString()
					));

					assertThat(report)
							.isPresent()
							.hasValueSatisfying(focus -> assertThat(focus.assess(Issue.Level.Error))
									.as("failure reported")
									.isTrue()
							);

					assertThat(graph())
							.as("graph unaltered")
							.isIsomorphicTo(dataset());

				});
			}

			@Test void testMalformedData() {
				exec(() -> {

					final Optional<Focus> report=engine().create(container, resource, decode(
							"<> :code 'xxxx'.", resource.toString()
					));

					assertThat(report)
							.isPresent()
							.hasValueSatisfying(focus -> assertThat(focus.assess(Issue.Level.Error))
									.as("failure reported")
									.isTrue()
							);

					assertThat(graph())
							.as("graph unaltered")
							.isIsomorphicTo(dataset());

				});
			}

		}

	}

}
