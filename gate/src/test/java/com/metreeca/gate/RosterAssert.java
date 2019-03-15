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

package com.metreeca.gate;

import com.metreeca.rest.Result;

import org.assertj.core.api.AbstractAssert;
import org.eclipse.rdf4j.model.IRI;

import java.util.*;

import static com.metreeca.form.things.Codecs.UTF8;
import static com.metreeca.form.things.JsonValues.field;
import static com.metreeca.form.things.JsonValues.object;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;

import static java.util.Arrays.stream;
import static java.util.function.Function.identity;
import static java.util.stream.Collectors.toMap;


public final class RosterAssert extends AbstractAssert<RosterAssert, Roster> {

	public static RosterAssert assertThat(final Roster actual) {
		return new RosterAssert(actual);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private RosterAssert(final Roster actual) {
		super(actual, RosterAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public RosterAssert hasUser(final IRI user, final String secret) {

		isNotNull();

		actual.verify(user, secret).error(error -> {

			failWithMessage(
					"expected roster to have user <%s:%s>", user, secret
			);

			return this;

		});

		return this;
	}

	public RosterAssert doesNotHaveUser(final IRI user, final String secret) {

		isNotNull();

		actual.verify(user, secret).value(permit -> {

			failWithMessage(
					"expected roster not to have user <%s:%s>", user, secret
			);

			return this;

		});

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static IRI user(final String handle) {
		return item("users/"+handle);
	}

	public static final class MockRoster implements Roster {

		private final Map<IRI, String> user2secret;


		public MockRoster(final String... handles) {
			this.user2secret=new LinkedHashMap<>(stream(handles).collect(toMap(RosterAssert::user, identity())));
		}


		@Override public Optional<IRI> resolve(final String handle) {
			return Optional.of(user(handle)).filter(user2secret::containsKey);
		}


		@Override public Result<Permit, String> lookup(final IRI user) {
			return Optional.ofNullable(user2secret.get(user))
					.map(secret -> Result.<Permit, String>Value(permit(user, secret)))
					.orElseGet(() -> Error(CredentialsIllegal));
		}

		@Override public Result<Permit, String> verify(final IRI user, final String secret) {
			return secret.equals(user2secret.get(user))
					? Value(permit(user, secret))
					: Error(CredentialsIllegal);
		}

		@Override public Result<Permit, String> verify(final IRI user, final String secret, final String update) {
			return update.equals(user2secret.computeIfPresent(user, (_user, _secret) -> update))
					? Value(permit(user, update))
					: Error(CredentialsIllegal);
		}

		@Override public Result<Permit, String> update(final IRI user, final String update) {
			return user2secret.computeIfPresent(user, (_user, _secret) -> update) != null
					? Value(permit(user, update))
					: Error(CredentialsIllegal);
		}

		@Override public Result<Permit, String> delete(final IRI user) {
			return Optional.ofNullable(user2secret.remove(user))
					.map(secret -> Result.<Permit, String>Value(permit(user, secret)))
					.orElseGet(() -> Error(CredentialsIllegal));
		}


		private Permit permit(final IRI user, final String secret) {
			return new Permit(
					UUID.nameUUIDFromBytes((user+":"+secret).getBytes(UTF8)).toString(),
					user, set(),
					object(field("user", user.getLocalName()))
			);
		}

	}

}
