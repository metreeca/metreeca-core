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

import com.metreeca.form.Form;
import com.metreeca.form.Issue;
import com.metreeca.form.probes.Cleaner;
import com.metreeca.form.probes.Optimizer;
import com.metreeca.form.probes.Redactor;
import com.metreeca.rest.Engine;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.LDP;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static com.metreeca.form.queries.Edges.edges;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shapes.Field.field;
import static com.metreeca.form.shapes.Meta.meta;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.wrappers.Throttler.resource;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.rdf.GraphTest.graph;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class ShapedResourceTest {

	private void exec(final Runnable... tasks) {
		new Tray()
				.exec(graph(dataset()))
				.exec(tasks)
				.clear();
	}


	private Model dataset() {
		return small();
	}

	private Engine engine() {
		return new ShapedResource(tool(Graph.Factory), Employee);
	}


	@Nested final class Relate {

		@Test void testRelate() {
			exec(() -> {

				final IRI hernandez=item("employees/1370");
				final IRI bondur=item("employees/1102");

				assertThat(engine().relate(hernandez, shape -> Value(edges(shape)), (shape, model) -> {

					assertThat(shape.map(new Cleaner()).map(new Optimizer()))
							.as("resource shape in convey mode (ignoring metadata)")
							.isEqualTo(resource().apply(shape)
									.map(new Cleaner())
									.map(new Redactor(Form.mode, Form.convey))
									.map(new Optimizer())
							);

					assertThat(model)

							.as("resource description")
							.hasStatement(hernandez, term("code"), literal("1370"))
							.hasStatement(hernandez, term("supervisor"), bondur)

							.as("connected resource description")
							.hasStatement(bondur, RDFS.LABEL, literal("Gerard Bondur"))
							.doesNotHaveStatement(bondur, term("code"), null);

					return model;

				}).value()).isPresent();

			});
		}

		@Test void testUnknown() {
			exec(() -> assertThat(engine().relate(item("employees/9999")))
					.as("empty description")
					.isEmpty());
		}

	}

	@Nested final class Create {

		@Test void testUnsupported() {
			exec(() -> assertThatThrownBy(() -> engine().create(
					item("employees/1370"), item("employees/9999"), set()
			)).isInstanceOf(UnsupportedOperationException.class));
		}

	}

	@Nested final class Update {

		@Test void testUpdate() {
			exec(() -> {

				final Model update=decode("</employees/1370>"
						+":forename 'Tino';"
						+":surname 'Faussone';"
						+":email 'tfaussone@example.com';"
						+":title 'Sales Rep' ;"
						+":seniority 5 ."
				);

				assertThat(engine().update(item("employees/1370"), update))
						.isPresent()
						.hasValueSatisfying(focus -> assertThat(focus.assess(Issue.Level.Warning))
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

				assertThat(engine().update(item("employees/1370"), update))
						.isPresent()
						.hasValueSatisfying(focus -> assertThat(focus.assess(Issue.Level.Error))
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

				assertThat(engine().update(item("employees/9999"), set()))
						.as("not found ")
						.isNotPresent();

				assertThat(graph())
						.as("graph unchanged")
						.isIsomorphicTo(small());

			});
		}

	}

	@Nested final class Delete {

		@Test void testDelete() {
			exec(() -> {

				assertThat(engine().delete(item("employees/1370")))
						.as("success reported")
						.isPresent();

				assertThat(graph("construct where { <employees/1370> ?p ?o }"))
						.as("cell deleted")
						.isEmpty();

				assertThat(graph("construct where { <employees/1102> rdfs:label ?o }"))
						.as("connected resources preserved")
						.isNotEmpty();

			});
		}

		@Test void testUnknown() {
			exec(() -> {

				assertThat(engine().delete(item("employees/9999")))
						.as("failure reported")
						.isNotPresent();

				assertThat(graph())
						.as("graph unchanged")
						.isIsomorphicTo(dataset());

			});
		}

		@Test void testDeleteFromBasicContainer() {
			new Tray()

					.exec(graph(decode("<> ldp:contains <resource>. <resource> rdf:value rdf:nil.")))

					.exec(() -> {

						final Engine engine=new ShapedResource(tool(Graph.Factory), and(
								meta(RDF.TYPE, LDP.BASIC_CONTAINER),
								field(RDF.VALUE)
						));

						assertThat(engine.delete(item("resource")))
								.as("success reported")
								.isPresent();

						assertThat(graph())
								.as("membership triples removed")
								.isEmpty();

					})

					.clear();
		}

		@Test void testDeleteFromDirectContainer() {
			new Tray()

					.exec(graph(decode("<resource> a rdfs:Resource; rdf:value rdf:nil.")))

					.exec(() -> {

						final Engine engine=new ShapedResource(tool(Graph.Factory), and(
								meta(RDF.TYPE, LDP.DIRECT_CONTAINER),
								meta(LDP.MEMBERSHIP_RESOURCE, RDFS.RESOURCE),
								meta(LDP.IS_MEMBER_OF_RELATION, RDF.TYPE),
								field(RDF.VALUE)
						));

						assertThat(engine.delete(item("resource")))
								.as("success reported")
								.isPresent();

						assertThat(graph())
								.as("membership triples removed")
								.isEmpty();

					})

					.clear();
		}

	}

}
