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

import org.assertj.core.api.AbstractAssert;


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

}
