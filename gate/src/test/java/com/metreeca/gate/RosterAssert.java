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

import java.util.Collection;
import java.util.Optional;
import java.util.Random;

import static com.metreeca.form.things.JsonValues.field;
import static com.metreeca.form.things.JsonValues.object;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.ValuesTest.item;
import static com.metreeca.rest.Result.Error;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;


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

		actual.login(handle, secret).error(error -> {

			failWithMessage(
					"expected roster to have user <%s:%s>", handle, secret
			);

			return this;

		});

		return this;
	}

	public RosterAssert doesNotHaveUser(final String handle, final String secret) {

		isNotNull();

		actual.login(handle, secret).value(permit -> {

			failWithMessage(
					"expected roster not to have user <%s:%s>", handle, secret
			);

			return this;

		});

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class MockRoster implements Roster {

		private static final Random random=new Random();

		private static String opaque() {
			return String.valueOf(random.nextLong());
		}


		private static final class Entry {

			private String opaque=opaque();

			private String handle;
			private String secret;


			private Entry(final String handle, final String secret) {
				this.handle=handle;
				this.secret=secret;
			}

		}

		private final Collection<Entry> entries;


		public MockRoster(final String... handles) {
			this.entries=stream(handles).map(handle -> new Entry(handle, handle)).collect(toList());
		}


		@Override public Result<Permit, String> lookup(final String handle) {
			return resolve(handle)
					.map(entry -> Result.<Permit, String>Value(permit(entry)))
					.orElseGet(() -> Error(CredentialsIllegal));
		}

		@Override public Result<Permit, String> login(final String handle, final String secret) {
			return resolve(handle)
					.filter(entry -> entry.secret.equals(secret))
					.map(entry -> {

						entry.opaque=opaque();

						return entry;

					})
					.map(entry -> Result.<Permit, String>Value(permit(entry)))
					.orElseGet(() -> Error(CredentialsIllegal));
		}

		@Override public Result<Permit, String> login(final String handle, final String secret, final String update) {
			return resolve(handle)
					.filter(entry -> entry.secret.equals(secret))
					.map(entry -> {

						entry.opaque=opaque();
						entry.secret=update;

						return entry;

					})
					.map(entry -> Result.<Permit, String>Value(permit(entry)))
					.orElseGet(() -> Error(CredentialsIllegal));
		}

		@Override public Result<Permit, String> logout(final String handle) {
			return resolve(handle)
					.map(entry -> {

						entry.opaque=opaque();

						return entry;

					})
					.map(entry -> Result.<Permit, String>Value(permit(entry)))
					.orElseGet(() -> Error(CredentialsIllegal));
		}


		private Optional<Entry> resolve(final String handle) {
			return entries.stream()
					.filter(entry -> entry.handle.equals(handle) || entry.opaque.equals(handle))
					.findFirst();
		}

		private Permit permit(final Entry entry) {
			return new Permit(entry.opaque,
					item("users/"+entry.handle), set(),
					object(field("user", entry.handle))
			);
		}

	}

}
