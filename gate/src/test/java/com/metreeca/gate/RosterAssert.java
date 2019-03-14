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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

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

	public RosterAssert hasUser(final String handle, final String secret) {

		isNotNull();

		actual.lookup(handle, secret).error(error -> {

			failWithMessage(
					"expected roster to have user <%s:%s>", handle, secret
			);

			return this;

		});

		return this;
	}

	public RosterAssert doesNotHaveUser(final String handle, final String secret) {

		isNotNull();

		actual.lookup(handle, secret).value(permit -> {

			failWithMessage(
					"expected roster not to have user <%s:%s>", handle, secret
			);

			return this;

		});

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class MockRoster implements Roster {

		private final Map<String, String> secrets;


		public MockRoster(final String... handles) {
			this.secrets=new LinkedHashMap<>(stream(handles).collect(toMap(identity(), identity())));
		}


		@Override public Result<Permit, String> lookup(final String handle) {
			return secrets.containsKey(handle)
					? Value(permit(handle, secrets.get(handle)))
					: Error(CredentialsIllegal);
		}

		@Override public Result<Permit, String> lookup(final String handle, final String secret) {
			return secret.equals(secrets.get(handle))
					? Value(permit(handle, secret))
					: Error(CredentialsIllegal);
		}

		@Override public Result<Permit, String> lookup(final String handle, final String secret, final String update) {
			return update.equals(secrets.computeIfPresent(handle, (_handle, _secret) -> update))
					? Value(permit(handle, update))
					: Error(CredentialsIllegal);
		}


		private Permit permit(final String handle, final String secret) {
			return new Permit(
					handle, UUID.nameUUIDFromBytes((handle+":"+secret).getBytes(UTF8)).toString(),
					item("users/"+handle), set(),
					object(field("user", handle))
			);
		}

	}

}
