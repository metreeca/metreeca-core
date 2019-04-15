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

package com.metreeca.gate.rosters;

import com.metreeca.form.things.ValuesTest;
import com.metreeca.gate.Permit;
import com.metreeca.gate.digests.PBKDF2Digest;
import com.metreeca.rest.Result;
import com.metreeca.tray.Tray;
import com.metreeca.tray.rdf.GraphTest;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.query.TupleQuery;
import org.eclipse.rdf4j.repository.RepositoryConnection;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.form.things.JsonValues.object;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.form.things.ValuesTest.small;
import static com.metreeca.form.things.ValuesTest.term;
import static com.metreeca.gate.Digest.digest;
import static com.metreeca.gate.Policy.policy;
import static com.metreeca.gate.Roster.CredentialsIllegal;
import static com.metreeca.gate.Roster.CredentialsInvalid;
import static com.metreeca.gate.policies.ComboPolicy.lowercases;
import static com.metreeca.gate.policies.ComboPolicy.only;
import static com.metreeca.rest.ResultAssert.assertThat;
import static com.metreeca.tray.rdf.Graph.graph;
import static com.metreeca.tray.rdf.GraphTest.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.rdf4j.common.iteration.Iterations.stream;
import static org.eclipse.rdf4j.query.QueryLanguage.SPARQL;


final class BasicRosterTest {

	private static final IRI Hernandez=item("employees/1370");


	private void exec(final Runnable... tasks) {
		new Tray()

				.set(graph(), GraphTest::graph)
				.set(digest(), PBKDF2Digest::new)
				.set(policy(), () -> only(lowercases()))

				.exec(model(small()))

				.exec(tasks)

				.clear();
	}


	private BasicRoster roster() {
		return new BasicRoster(this::resolve, this::profile);
	}


	private Optional<IRI> resolve(final RepositoryConnection connection, final String handle) {
		return stream(connection.getStatements(

				null, term("email"), literal(handle)

		))

				.map(Statement::getSubject)
				.filter(resource -> resource instanceof IRI)
				.map(resource -> (IRI)resource)

				.findFirst();
	}

	private Optional<Permit> profile(final RepositoryConnection connection, final IRI user) {

		final TupleQuery query=connection.prepareTupleQuery(SPARQL, "\n"
						+"prefix : <terms#>\n"
						+"\n"
						+"prefix rdfs: <http://www.w3.org/2000/01/rdf-schema#>\n"
						+"\n"
						+"select * {\n"
						+"\n"
						+"\t$user a :Employee; \n"
						+"\t\trdfs:label ?label;\n"
						+"\t\t:title ?title.\n"
						+"\n"
						+"}",
				ValuesTest.Base
		);

		query.setBinding("user", user);

		return stream(query.evaluate())

				.map(bindings -> new Permit(
						"", user,
						set(), // !!!
						object() // !!!
				))

				.findFirst();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Test void testResolve() {
		exec(() -> {

			assertThat(roster().resolve("ghernande@example.com"))
					.as("known handle resolved")
					.hasValue(Hernandez);

			assertThat(roster().resolve("unknown@example.com"))
					.as("unknown handle rejected")
					.hasError(CredentialsInvalid);

		});
	}


	@Test void testVerify() {
		exec(() -> {

			final BasicRoster roster=roster();

			assertThat(roster.insert(Hernandez, "secret"))
					.as("secret inserted")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.verify(Hernandez, "secret"))
					.as("valid secret accepted")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.verify(Hernandez, "qwertyuiop"))
					.as("invalid secret rejected")
					.hasError(CredentialsInvalid);

		});
	}

	@Test void testUpdate() {
		exec(() -> {

			final BasicRoster roster=roster();

			final Result<Permit, String> inserted=roster.insert(Hernandez, "secret");

			assertThat(inserted)
					.as("secret inserted")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			final Result<Permit, String> updated=roster.verify(Hernandez, "secret", "updated");

			assertThat(updated)
					.as("verify and update")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.verify(Hernandez, "updated"))
					.as("updated secret accepted")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.verify(Hernandez, "secret"))
					.as("original secret rejected")
					.hasError(CredentialsInvalid);

			assertThat(updated.value().map(Permit::hash))
					.as("permit hash changed")
					.isNotEqualTo(inserted.value().map(Permit::hash));

		});
	}

	@Test void testUpdateInvalid() {
		exec(() -> {

			final BasicRoster roster=roster();

			assertThat(roster.insert(Hernandez, "secret"))
					.as("secret inserted")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.verify(Hernandez, "secret", "UPDATE"))
					.as("illegal secret rejected")
					.hasError(CredentialsIllegal);

		});
	}


	@Test void testLookup() {
		exec(() -> {

			final BasicRoster roster=roster();

			assertThat(roster.insert(Hernandez, "secret"))
					.as("secret inserted")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.lookup(Hernandez))
					.as("known user looked up")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.lookup(item("employees/9999")))
					.as("unknown user rejected")
					.hasError(CredentialsInvalid);

		});
	}

	@Test void testInsert() {
		exec(() -> {

			final BasicRoster roster=roster();

			assertThat(roster.insert(Hernandez, "secret"))
					.as("secret inserted")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.verify(Hernandez, "secret"))
					.as("inserted secret accepted")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

		});
	}

	@Test void testInsertInvalid() {
		exec(() -> {

			final BasicRoster roster=roster();

			assertThat(roster.insert(Hernandez, "SECRET"))
					.as("illegal secret rejected")
					.hasError(CredentialsIllegal);

		});
	}

	@Test void testRemove() {
		exec(() -> {

			final BasicRoster roster=roster();

			assertThat(roster.insert(Hernandez, "secret"))
					.as("secret inserted")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.remove(Hernandez))
					.as("secret removed")
					.hasValue(permit -> assertThat(permit.user())
							.isEqualTo(Hernandez)
					);

			assertThat(roster.verify(Hernandez, "secret"))
					.as("removed secret rejected")
					.hasError(CredentialsInvalid);

		});
	}


	@Test void testGenerateHashes() {
		exec(() -> {

			final BasicRoster roster=roster();

			final Function<Result<Permit, String>, Permit> value=result ->
					result.value().orElseThrow(() -> new RuntimeException("missing value"));

			final String inserted=value.apply(roster.insert(Hernandez, "secret")).hash();

			assertThat(inserted)

					.as("stable lookup hash after insert")
					.isEqualTo(value.apply(roster.lookup(Hernandez)).hash())

					.as("stable verify hash after insert")
					.isEqualTo(value.apply(roster.verify(Hernandez, "secret")).hash());

			final String updated=value.apply(roster.verify(Hernandez, "secret", "update")).hash();

			assertThat(updated)

					.as("hash changed on update")
					.isNotEqualTo(inserted)

					.as("stable lookup hash after update")
					.isEqualTo(value.apply(roster.lookup(Hernandez)).hash())

					.as("stable verify hash after update")
					.isEqualTo(value.apply(roster.verify(Hernandez, "update")).hash());

		});

	}

}
