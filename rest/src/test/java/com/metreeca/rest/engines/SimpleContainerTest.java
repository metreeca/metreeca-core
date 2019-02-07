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

import com.metreeca.form.Focus;
import com.metreeca.form.Issue;
import com.metreeca.rest.Engine;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static com.metreeca.form.queries.Stats.stats;
import static com.metreeca.form.things.Lists.list;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.tray.Tray.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class SimpleContainerTest {

	private void exec(final Runnable task) {
		new Tray()
				.exec(graph(dataset()))
				.exec(task)
				.clear();
	}


	private Model dataset() {
		return small();
	}


	@Nested final class Basic {

		private final IRI container=item("/employees-basic/");


		private Engine engine() {
			return new SimpleContainer(tool(Graph.Factory), map());
		}


		@Nested final class Browse {

			@Test void testBrowse() {
				exec(() -> assertThat(engine().browse(container))

						.as("item descriptions linked to container")
						.hasSubset(decode("<employees-basic/> ldp:contains <employees/1370>."))

						.as("item descriptions included")
						.hasSubset(decode("<employees/1370> :code '1370'.")));
			}

			@Test void testBrowseFiltered() {
				exec(() -> assertThatThrownBy(() -> engine()
						.browse(container, shape -> Value(stats(shape, list())), (shape, model) -> model)
				).isInstanceOf(UnsupportedOperationException.class));
			}

		}

		@Nested final class Create {

			private final IRI resource=item("/employees-basic/9999");


			@Test void testCreate() {
				exec(() -> {

					final Optional<Focus> report=engine().create(container, resource, decode(
							"<> :code '9999' .", resource.toString()
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
							"<> :code '9999'; rdf:value rdf:first. rdf:first rdf:value rdf:rest.", resource.toString()
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
			return new SimpleContainer(tool(Graph.Factory), map(
					entry(LDP.CONTAINER, LDP.DIRECT_CONTAINER),
					entry(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
					entry(LDP.MEMBERSHIP_RESOURCE, term("Employee"))
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
							"<> :code '9999'; rdf:value rdf:first. rdf:first rdf:value rdf:rest.", resource.toString()
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
