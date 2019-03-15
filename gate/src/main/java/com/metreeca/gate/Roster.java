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

import org.eclipse.rdf4j.model.IRI;

import java.util.Optional;
import java.util.function.Supplier;


/**
 * User roster.
 *
 * <p>Authenticates users and manages user credentials.</p>
 */
public interface Roster {

	/**
	 * Retrieves the default roster factory.
	 *
	 * @return the default roster factory, which throws an exception reporting the tool as undefined
	 */
	public static Supplier<Roster> roster() {
		return () -> { throw new IllegalStateException("undefined roster tool"); };
	}


	public static String CredentialsInvalid="credentials-invalid"; // invalid handle or secret
	public static String CredentialsIllegal="credentials-illegal"; // secret unacceptable by policy

	public static String CredentialsExpired="credentials-expired"; // expired secret
	public static String CredentialsPending="credentials-pending"; // account to be activated
	public static String CredentialsRevoked="credentials-revoked"; // account revoked or locked


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<IRI> resolve(final String handle);


	public Result<Permit, String> lookup(final IRI user);

	public Result<Permit, String> verify(final IRI user, final String secret);

	public Result<Permit, String> verify(final IRI user, final String secret, final String update);

	public Result<Permit, String> update(final IRI user, final String update);

	public Result<Permit, String> delete(final IRI user);

}