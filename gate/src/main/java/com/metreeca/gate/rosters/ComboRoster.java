/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.gate.rosters;


import com.metreeca.gate.Permit;
import com.metreeca.gate.Roster;
import com.metreeca.rest.Result;

import org.eclipse.rdf4j.model.IRI;


/**
 * Combo roster.
 *
 * <p>Delegate users credentials management to a delegate {@linkplain #delegate(Roster) delegate} roster, possibly
 * assembled as a combination of other rosters.</p>

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
