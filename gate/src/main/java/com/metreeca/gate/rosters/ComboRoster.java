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


import com.metreeca.gate.Permit;
import com.metreeca.gate.Roster;
import com.metreeca.rest.Result;

import org.eclipse.rdf4j.model.IRI;


/**
 * Combo roster.
 *
 * <p>Delegate users credentials management to a {@linkplain #delegate(Roster) delegate} roster, possibly assembled as a
 * combination of other rosters.</p>
 */
public abstract class ComboRoster implements Roster {

	private Roster delegate;


	/**
	 * Retrieves the delegate roster.
	 *
	 * @return the roster user credentials management is delegated to
	 *
	 * @throws IllegalStateException if the delegate roster wasn't {@linkplain #delegate(Roster) configured}
	 */
	protected Roster delegate() {

		if ( delegate == null ) {
			throw new IllegalStateException("undefined delegate");
		}

		return delegate;
	}

	/**
	 * Configures the delegate roster.
	 *
	 * @param delegate the roster user credentials management is delegated to
	 *
	 * @return this combo roster
	 *
	 * @throws NullPointerException if {@code delegate} is null
	 */
	protected ComboRoster delegate(final Roster delegate) {

		if ( delegate == null ) {
			throw new NullPointerException("null delegate");
		}

		this.delegate=delegate;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Result<IRI, String> resolve(final String handle) {
		return delegate().resolve(handle);
	}


	@Override public Result<Permit, String> verify(final IRI user, final String secret) {
		return delegate().verify(user, secret);
	}

	@Override public Result<Permit, String> verify(final IRI user, final String secret, final String update) {
		return delegate().verify(user, secret, update);
	}


	@Override public Result<Permit, String> lookup(final IRI user) {
		return delegate().lookup(user);
	}

	@Override public Result<Permit, String> insert(final IRI user, final String secret) {
		return delegate().insert(user, secret);
	}

	@Override public Result<Permit, String> remove(final IRI user) {
		return delegate().remove(user);
	}

}
