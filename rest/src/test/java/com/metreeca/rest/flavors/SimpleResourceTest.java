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

package com.metreeca.rest.flavors;

import com.metreeca.form.Focus;
import com.metreeca.form.Issue;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Model;
import org.eclipse.rdf4j.model.vocabulary.RDF;
import org.eclipse.rdf4j.model.vocabulary.RDFS;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.function.Consumer;

import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.*;
import static com.metreeca.form.truths.ModelAssert.assertThat;
import static com.metreeca.rest.HandlerAssert.graph;
import static com.metreeca.tray.Tray.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


final class SimpleResourceTest {

	private Model dataset() {
		return small();
	}

	private void exec(final Consumer<RepositoryConnection> task) {
		new Tray()
				.exec(graph(dataset()))
				.exec(() -> tool(Graph.Factory).update(task))
				.clear();
	}


	@Nested final class Relate {

		@Test void testRelate() {
			exec(connection -> {

				final IRI hernandez=item("employees/1370");
				final IRI bondur=item("employees/1102");

				assertThat(new SimpleResource(connection).relate(hernandez))
						.isPresent()
						.hasValueSatisfying(description -> assertThat(description)

								.as("resource description")
								.hasStatement(hernandez, term("code"), literal("1370"))
								.hasStatement(hernandez, term("supervisor"), bondur)

								.as("labelled connected resource description")
								.hasStatement(bondur, RDF.TYPE, term("Employee"))
								.hasStatement(bondur, RDFS.LABEL, literal("Gerard Bondur"))
								.doesNotHaveStatement(bondur, term("code"), null)
						);

			});
		}

		@Test void testUnknown() {
			exec(connection -> {

				assertThat(new SimpleResource(connection).relate(item("employees/9999")))
						.as("empty description")
						.isEmpty();

			});
		}

	}

	@Nested final class Create {

		@Test void testCreate() {
			exec(connection -> assertThatThrownBy(() ->
					new SimpleResource(connection).create(item("employees/1370"), set())
			).isInstanceOf(UnsupportedOperationException.class));
		}

	}


	@Nested final class Update {

		@Test void testUpdate() {
			exec(connection -> {

				final Model update=decode("</employees/1370>"
						+":forename 'Tino';"
						+":surname 'Faussone';"
						+":email 'tfaussone@example.com';"
						+":title 'Sales Rep' ;"
						+":seniority 5 ."
				);

				final Focus focus=new SimpleResource(connection).update(item("employees/1370"), update);

				assertThat(focus.assess(Issue.Level.Warning))
						.as("success reported")
						.isFalse();

				assertThat(graph())

						.as("graph updated")

						.hasSubset(update)

						.doesNotHaveStatement(item("employees/1370"), term("forename"), literal("Gerard"))
						.doesNotHaveStatement(item("employees/1370"), term("surname"), literal("Hernandez"));

			});
		}


		@Test void testExceedingData() {

			exec(connection -> {
				final Model update=decode("</employees/1370>"
						+" :forename 'Tino' ;"
						+" :surname 'Faussone' ;"
						+" :office <offices/1> . <offices/1> :value 'exceeding' ."
				);

				final Focus focus=new SimpleResource(connection).update(item("employees/1370"), update);

				assertThat(focus.assess(Issue.Level.Error))
						.as("failure reported")
						.isTrue();

				assertThat(graph())
						.as("graph unchanged")
						.isIsomorphicTo(small());

			});
		}

	}

	@Nested final class Delete {

		@Test void testDelete() {
			exec(connection -> {

				assertThat(new SimpleResource(connection).delete(item("employees/1370")))
						.as("success reported")
						.isTrue();

				assertThat(graph("construct where { <employees/1370> ?p ?o }"))
						.as("cell deleted")
						.isEmpty();

				assertThat(graph("construct where { ?s ?p <employees/1370> }"))
						.as("inbound links removed")
						.isEmpty();

				assertThat(graph("construct where { <employees/1102> rdfs:label ?o }"))
						.as("connected resources preserved")
						.isNotEmpty();

			});
		}

		@Test void testUnknown() {
			exec(connection -> {

				assertThat(new SimpleResource(connection).delete(item("employees/9999")))
						.as("failure reported")
						.isFalse();

				assertThat(graph())
						.as("graph unchanged")
						.isIsomorphicTo(dataset());

			});
		}

	}

}
