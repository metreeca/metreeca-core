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

package com.metreeca.rest.engines_;

import com.metreeca.form.*;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.form.things.ValuesTest;
import com.metreeca.tray.Tray;

import org.assertj.core.api.Assertions;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.vocabulary.*;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.Form.stats;
import static com.metreeca.form.Shape.filter;
import static com.metreeca.form.probes.Evaluator.pass;
import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.queries.Items.items;
import static com.metreeca.form.queries.Stats.stats;
import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.Values.pattern;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.wrappers.Throttler.resource;
import static com.metreeca.tray.rdf.GraphTest.graph;

import static java.util.stream.Collectors.toSet;


final class GraphEngineTest {

	private final IRI Paris=item("offices/4");
	private final IRI Hernandez=item("employees/1370");
	private final IRI Bondur=item("employees/1102");

	private final IRI BasicContainer=item("/employees-basic/");
	private final IRI DirectContainer=item("/employees/");

	private static final Shape BasicMetadata=and(
			meta(RDF.TYPE, LDP.BASIC_CONTAINER)
	);

	private static final Shape DirectMetadata=and(
			meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
			meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
			meta(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
	);

	public static final Shape Employee=ValuesTest.Employee
			.map(resource())
			.map(new Redactor(Form.role, Salesman))
			.map(new Optimizer());

	private final Shape TitleFilter=filter().then(
			field(term("title"), all(literal("President")))
	);


	private void exec(final Runnable task) {
		new Tray()
				.exec(graph(dataset()))
				.exec(task)
				.clear();
	}


	private Model dataset() {
		return small();
	}

	private _Engine engine() {
		return new GraphEngine();
	}


	@Nested final class Relate {

		private final Shape Employee=GraphEngineTest.Employee
				.map(new Redactor(Form.task, Form.relate))
				.map(new Redactor(Form.view, Form.detail))
				.map(new Optimizer());


		@Nested final class SimpleResource {

			@Test void testRelate() {
				exec(() -> {

					assertThat(engine().relate(Hernandez, edges(pass())))

							.as("resource description")
							.hasStatement(Hernandez, term("code"), literal("1370"))
							.hasStatement(Hernandez, term("supervisor"), Bondur)

							.as("labelled connected resource description")
							.hasStatement(Bondur, RDF.TYPE, term("Employee"))
							.hasStatement(Bondur, RDFS.LABEL, literal("Gerard Bondur"))
							.doesNotHaveStatement(Bondur, term("code"), null);

				});
			}

			@Test void testUnknown() {
				exec(() -> assertThat(engine().relate(item("employees/9999"), edges(pass())))
						.as("empty description")
						.isEmpty());
			}

		}

		@Nested final class ShapedResource {

			@Test void testRelate() {
				exec(() -> {

					final IRI hernandez=item("employees/1370");

					assertThat(engine().relate(hernandez, edges(Employee)))

							.as("resource description")
							.hasStatement(hernandez, term("code"), literal("1370"))

							.as("restricted connections not included")
							.doesNotHaveStatement(hernandez, term("supervisor"), null);

				});
			}

			@Test void testUnknown() {
				exec(() -> assertThat(engine().relate(item("employees/9999"), edges(Employee)))
						.as("empty description")
						.isEmpty());
			}

		}

		@Nested final class SimpleBasicContainer {

			@Test void testRelate() {
				exec(() -> assertThat(engine().relate(BasicContainer, edges(and(BasicMetadata, and()))))

						.as("item descriptions linked to container")
						.hasStatement(BasicContainer, LDP.CONTAINS, Hernandez)

						.as("item descriptions included")
						.hasStatement(Hernandez, term("code"), literal("1370"))
						.hasStatement(Hernandez, term("supervisor"), Bondur)

						.as("connected resource short description included")
						.hasStatement(Paris, RDFS.LABEL, null)
						.doesNotHaveStatement(Paris, term("code"), null));

			}

			@Test void testEmpty() {
				exec(() -> assertThat(engine().relate(term("employees-none/"), edges(and(BasicMetadata, and()))))
						.isEmpty()
				);
			}

		}

		@Nested final class SimpleDirectContainer {

			@Test void testRelate() {
				exec(() -> assertThat(engine().relate(BasicContainer, edges(and(DirectMetadata, and()))))

						.as("item descriptions linked to container")
						.hasStatement(BasicContainer, LDP.CONTAINS, Hernandez)

						.as("item descriptions included")
						.hasStatement(Hernandez, term("code"), literal("1370"))
						.hasStatement(Hernandez, term("supervisor"), Bondur)

						.as("connected resource short description included")
						.hasStatement(Paris, RDFS.LABEL, null)
						.doesNotHaveStatement(Paris, term("code"), null));

			}

			@Test void testEmpty() {
				exec(() -> assertThat(engine().relate(DirectContainer, edges(and(and(
						meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
						meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
						meta(LDP.MEMBERSHIP_RESOURCE, RDF.NIL)
				), and())))).isEmpty());
			}

		}

		@Nested final class ShapedBasicContainer {

			@Test void testRelate() {
				exec(() -> assertThat(engine().relate(BasicContainer, edges(and(BasicMetadata, Employee))))

						.as("item descriptions linked to container")
						.hasStatement(BasicContainer, LDP.CONTAINS, Hernandez)

						.as("item descriptions included")
						.hasStatement(Hernandez, term("code"), literal("1370"))

						.as("restricted properties excluded")
						.doesNotHaveStatement(Hernandez, term("supervisor"), null)

				);
			}

		}

		@Nested final class ShapedDirectContainer {

			@Test void testRelate() {
				exec(() -> assertThat(engine().relate(DirectContainer, edges(and(DirectMetadata, Employee))))

						.as("item descriptions linked to container")
						.hasStatement(DirectContainer, LDP.CONTAINS, Hernandez)

						.as("item descriptions included")
						.hasStatement(Hernandez, term("code"), literal("1370"))

						.as("restricted properties excluded")
						.doesNotHaveStatement(Hernandez, term("supervisor"), null)

				);
			}

			@Test void testRelateFiltered() {
				exec(() -> Assertions.assertThat(engine()
								.relate(DirectContainer, edges(and(DirectMetadata, Employee, TitleFilter)))
								.stream()
								.filter(pattern(null, LDP.CONTAINS, null))
								.map(Statement::getObject)
								.collect(toSet())
						).containsOnly(item("employees/1002"))

				);

			}

			@Test void testRelateStats() {
				exec(() -> assertThat(engine().relate(DirectContainer, stats(and(DirectMetadata, Employee), list(term("title")))))

						.as("query-specific payload")
						.hasStatement(DirectContainer, Form.count, null)
						.hasStatement(DirectContainer, stats, XMLSchema.STRING)
						.hasStatement(XMLSchema.STRING, Form.count, null)

				);
			}

			@Test void testRelateItems() {
				exec(() -> assertThat(engine().relate(DirectContainer, items(and(DirectMetadata, Employee), list(term("title")))))

						.as("query-specific payload")
						.hasStatement(DirectContainer, Form.items, null)
						.hasStatement(null, Form.value, literal("President"))

				);
			}

		}

	}

	@Nested final class Create {

		private final Shape Employee=GraphEngineTest.Employee
				.map(new Redactor(Form.task, Form.create))
				.map(new Redactor(Form.view, Form.detail))
				.map(new Optimizer());

		//@Nested final class SimpleBasicContainer {
		//
		//	private final IRI resource=item("/employees-basic/9999");
		//
		//
		//	@Test void testCreate() {
		//		exec(() -> {
		//
		//			final Optional<Focus> report=engine().create(container, resource, decode(
		//					"<> :code '9999' .", resource.toString()
		//			));
		//
		//			Assertions.assertThat(report)
		//					.isPresent()
		//					.hasValueSatisfying(focus -> Assertions.assertThat(focus.assess(Issue.Level.Error))
		//							.as("success reported")
		//							.isFalse()
		//					);
		//
		//			assertThat(graph())
		//					.as("graph updated")
		//					.hasSubset(decode(""
		//							+"<employees-basic/> ldp:contains <employees-basic/9999>."
		//							+"<employees-basic/9999> :code '9999' ."
		//					));
		//
		//		});
		//	}
		//
		//	@Test void testConflict() {
		//		exec(() -> {
		//
		//			final Optional<Focus> report=engine().create(container, item("employees/1370"), decode(
		//					"<> :code '1370'.", resource.toString()
		//			));
		//
		//			Assertions.assertThat(report)
		//					.as("conflict detected")
		//					.isNotPresent();
		//
		//			assertThat(graph())
		//					.as("graph unaltered")
		//					.isIsomorphicTo(dataset());
		//
		//		});
		//	}
		//
		//	@Test void testExceedingData() {
		//		exec(() -> {
		//
		//			final Optional<Focus> report=engine().create(container, resource, decode(
		//					"<> :code '9999'; rdf:value rdf:first. rdf:first rdf:value rdf:rest.", resource.toString()
		//			));
		//
		//			Assertions.assertThat(report)
		//					.isPresent()
		//					.hasValueSatisfying(focus -> Assertions.assertThat(focus.assess(Issue.Level.Error))
		//							.as("failure reported")
		//							.isTrue()
		//					);
		//
		//			assertThat(graph())
		//					.as("graph unaltered")
		//					.isIsomorphicTo(dataset());
		//
		//		});
		//	}
		//
		//}

		//@Nested final class SimpleDirectContainer {
		//
		//	private Engine engine() {
		//		return new SimpleContainer(tool(Graph.Factory), map(
		//				entry(RDF.TYPE, LDP.DIRECT_CONTAINER),
		//				entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
		//				entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
		//		));
		//	}
		//
		//
		//	@Nested final class Create {
		//
		//		private final IRI container=item("/employees/");
		//		private final IRI resource=item("/employees/9999");
		//
		//
		//		@Test void testCreate() {
		//			exec(() -> {
		//
		//				final Optional<Focus> report=engine().create(container, resource, decode(
		//						"<> :code '9999'.", resource.toString()
		//				));
		//
		//				Assertions.assertThat(report)
		//						.isPresent()
		//						.hasValueSatisfying(focus -> Assertions.assertThat(focus.assess(Issue.Level.Error))
		//								.as("success reported")
		//								.isFalse()
		//						);
		//
		//				assertThat(graph())
		//						.as("graph updated")
		//						.hasSubset(decode(
		//								"<employees/9999> a :Employee; :code '9999'."
		//						));
		//
		//			});
		//		}
		//
		//		@Test void testConflict() {
		//			exec(() -> {
		//
		//				final Optional<Focus> report=engine().create(container, item("employees/1370"), decode(
		//						"<> :code '1370'.", resource.toString()
		//				));
		//
		//				Assertions.assertThat(report)
		//						.as("conflict detected")
		//						.isNotPresent();
		//
		//				assertThat(graph())
		//						.as("graph unaltered")
		//						.isIsomorphicTo(dataset());
		//
		//			});
		//		}
		//
		//		@Test void testExceedingData() {
		//			exec(() -> {
		//
		//				final Optional<Focus> report=engine().create(container, resource, decode(
		//						"<> :code '9999'; rdf:value rdf:first. rdf:first rdf:value rdf:rest.", resource.toString()
		//				));
		//
		//				Assertions.assertThat(report)
		//						.isPresent()
		//						.hasValueSatisfying(focus -> Assertions.assertThat(focus.assess(Issue.Level.Error))
		//								.as("failure reported")
		//								.isTrue()
		//						);
		//
		//				assertThat(graph())
		//						.as("graph unaltered")
		//						.isIsomorphicTo(dataset());
		//
		//			});
		//		}
		//
		//	}
		//
		//}

	}

	@Nested final class Update {

		private final Shape Employee=GraphEngineTest.Employee
				.map(new Redactor(Form.task, Form.update))
				.map(new Redactor(Form.view, Form.detail))
				.map(new Optimizer());

		@Nested final class SimpleResource {

			@Test void testUpdate() {
				exec(() -> {

					final Model update=decode("</employees/1370>"
							+":forename 'Tino';"
							+":surname 'Faussone';"
							+":email 'tfaussone@example.com';"
							+":title 'Sales Rep' ;"
							+":seniority 5 ."
					);


					Assertions.assertThat(engine().update(item("employees/1370"), and(), update))
							.isPresent()
							.hasValueSatisfying(focus -> FocusAssert.assertThat(focus)
									.as("success reported")
									.isValid()
							);

					assertThat(graph())

							.as("graph updated")

							.hasSubset(update)

							.doesNotHaveStatement(item("employees/1370"), term("forename"), literal("Gerard"))
							.doesNotHaveStatement(item("employees/1370"), term("surname"), literal("Hernandez"));

				});
			}

			@Test void testExceedingData() {
				exec(() -> {

					final Model update=decode("</employees/1370>"
							+" :forename 'Tino' ;"
							+" :surname 'Faussone' ;"
							+" :office <offices/1> . <offices/1> :value 'exceeding' ."
					);

					Assertions.assertThat(engine().update(item("employees/1370"), and(), update))
							.isPresent()
							.hasValueSatisfying(focus -> Assertions.assertThat(focus.assess(Issue.Level.Error))
									.as("failure reported")
									.isTrue()
							);

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(small());

				});
			}

			@Test void testUnknown() {
				exec(() -> {

					Assertions.assertThat(engine().update(item("employees/9999"), and(), set()))
							.as("not found ")
							.isNotPresent();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(small());

				});
			}

		}

		@Nested final class ShapedResource {

			@Test void testUpdate() {
				exec(() -> {

					final Model update=decode("</employees/1370>"
							+":forename 'Tino';"
							+":surname 'Faussone';"
							+":email 'tfaussone@example.com';"
							+":title 'Sales Rep' ;"
							+":seniority 5 ."
					);

					Assertions.assertThat(engine().update(item("employees/1370"), Employee, update))
							.isPresent()
							.hasValueSatisfying(focus -> Assertions.assertThat(focus.assess(Issue.Level.Warning))
									.as("success reported")
									.isFalse()
							);

					assertThat(graph())

							.as("graph updated")

							.hasSubset(update)

							.doesNotHaveStatement(item("employees/1370"), term("forename"), literal("Gerard"))
							.doesNotHaveStatement(item("employees/1370"), term("surname"), literal("Hernandez"));

				});
			}

			@Test void testExceedingData() {
				exec(() -> {

					final Model update=decode("</employees/1370>"
							+" :forename 'Tino' ;"
							+" :surname 'Faussone' ;"
							+" :code '9999' ." // write-once
					);

					Assertions.assertThat(engine().update(item("employees/1370"), Employee, update))
							.isPresent()
							.hasValueSatisfying(focus -> Assertions.assertThat(focus.assess(Issue.Level.Error))
									.as("failure reported")
									.isTrue()
							);

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(small());

				});
			}

			@Test void testUnknown() {
				exec(() -> {

					Assertions.assertThat(engine().update(item("employees/9999"), Employee, set()))
							.as("not found ")
							.isNotPresent();

					assertThat(graph())
							.as("graph unchanged")
							.isIsomorphicTo(small());

				});
			}

		}

	}

	@Nested final class Delete {

		private final Shape Employee=GraphEngineTest.Employee
				.map(new Redactor(Form.task, Form.delete))
				.map(new Redactor(Form.view, Form.detail))
				.map(new Optimizer());

	}

}
