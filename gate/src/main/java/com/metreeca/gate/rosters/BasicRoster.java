/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.gate.rosters;

import com.metreeca.gate.Permit;
import com.metreeca.gate.Roster;
import com.metreeca.rest.Result;

import org.eclipse.rdf4j.model.IRI;


public final class BasicRoster implements Roster {

	@Override public Result<IRI, String> resolve(final String handle) {
		return null;
	}


	@Override public Result<Permit, String> lookup(final IRI user) {
		return null;
	}

	@Override public Result<Permit, String> verify(final IRI user, final String secret) {
		return null;
	}

	@Override public Result<Permit, String> verify(final IRI user, final String secret, final String update) {
		return null;
	}

	@Override public Result<Permit, String> update(final IRI user, final String update) {
		return null;
	}

	@Override public Result<Permit, String> delete(final IRI user) {
		return null;
	}

}
