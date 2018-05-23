/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.tray.iam;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

import static com.metreeca.spec.things.ValuesTest.item;
import static com.metreeca.tray.iam.Roster.permit;


public final class RosterTest {

	public static final String This="this";
	public static final String That="that";


	public static final class MockRoster implements Roster {

		private static final Permit invalid=permit().issue(CredentialsRejected).done();


		private final Map<String, Permit> permits=new HashMap<>();


		public MockRoster() {
			permits.put(This, std(This).done());
			permits.put(That, std(That).done());
		}


		@Override public Permit profile(final String alias) {
			return Optional.ofNullable(permits.get(alias)).orElse(invalid);
		}

		@Override public Permit refresh(final String alias) {
			return process(alias, permit ->
					std(alias).token(alias).expiry(System.currentTimeMillis()+60*1000).done()
			);
		}

		@Override public Permit acquire(final String alias, final String secret) {
			return process(alias, permit -> secret.equals(permit.token()) ?
					std(alias).token(alias).expiry(System.currentTimeMillis()+60*1000).done() : null
			);
		}

		@Override public Permit acquire(final String alias, final String secret, final String update) {
			return process(alias, permit -> secret.equals(permit.token()) ?
					std(alias).token(update).expiry(System.currentTimeMillis()+60*1000).done() : null
			);
		}

		@Override public Permit release(final String alias) {
			return process(alias, permit ->
					std(alias).token(permit.token()).done()
			);
		}


		private Permit process(final String alias, final Function<Permit, Permit> action) {

			final String key=permits.values().stream()
					.filter(permit
							-> permit.alias().equals(alias)
							|| permit.user().stringValue().equals(alias)
					)
					.map(Permit::alias)
					.findFirst()
					.orElse(null);

			final Permit current=permits.get(key);
			final Permit updated=current == null ? null : action.apply(current);

			if ( updated != null ) {
				permits.put(key, updated);
			}

			return updated != null ? updated : invalid;
		}


		private Permit.Builder std(final String alias) {
			return permit()
					.user(item("users/"+alias))
					.label(alias)
					.alias(alias)
					.token(alias);
		}

	}

}
