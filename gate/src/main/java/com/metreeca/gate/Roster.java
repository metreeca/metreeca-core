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

import java.util.function.Supplier;


/**
 * User roster.
 *
 * <p>Manages user credentials and authentication.</p>
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


	/**
	 * Error tag for reporting invalid user handles or secrets.
	 */
	public static String CredentialsInvalid="credentials-invalid";

	/**
	 * Error tag for reporting user secrets not compliant with the enforced {@linkplain Policy secret policy}.
	 */
	public static String CredentialsIllegal="credentials-illegal";

	// !!! public static String CredentialsExpired="credentials-expired"; // expired secret
	// !!! public static String CredentialsPending="credentials-pending"; // account to be activated
	// !!! public static String CredentialsRevoked="credentials-revoked"; // account revoked or locked


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	/**
	 * Looks up a user.
	 *
	 * @param handle a handle uniquely identifying a user (e.g. username, email, current session id, …)
	 *
	 * @return a value result with the current permit for the user identified by {@code handle}, if successful
	 * identified; an error result with a roster-specific error tag, otherwise
	 *
	 * @throws NullPointerException if {@code handle} is null
	 */
	public Result<Permit, String> lookup(final String handle);

	/**
	 * Inserts a user.
	 *
	 * @param handle a handle uniquely identifying the user to be inserted into this roster (e.g. username, email, …)
	 * @param secret the initial secret for the user identified by {@code handle}
	 *
	 * @return a value result with the current permit for the user identified by {@code handle}, if successful inserted
	 * into this roster using {@code secret}; an error result with a roster-specific error tag, otherwise
	 *
	 * @throws NullPointerException if either {@code handle} or {@code secret} is null
	 */
	public Result<Permit, String> insert(final String handle, final String secret);

	/**
	 * Removes a user.
	 *
	 * @param handle a handle uniquely identifying the user to be removed from this roster (e.g. username, email,
	 *               current session id, …)
	 *
	 * @return a value result with the current permit for the user identified by {@code handle}, if successful removed
	 * from this roster; an error result with a roster-specific error tag, otherwise
	 *
	 * @throws NullPointerException if {@code handle} is null
	 */
	public Result<Permit, String> remove(final String handle);


	/**
	 * Logs a user in.
	 *
	 * @param handle a handle uniquely identifying a user (e.g. username, email, current session id, …)
	 * @param secret the current secret for the user identified by {@code handle}
	 *
	 * @return a value result with the current permit for the user identified by {@code handle}, if successful logged in
	 * using {@code secret}; an error result with a roster-specific error tag, otherwise
	 *
	 * @throws NullPointerException if either {@code handle} or {@code secret} is null
	 */
	public Result<Permit, String> login(final String handle, final String secret);

	/**
	 * Logs a user in with an updated secret.
	 *
	 * @param handle a handle uniquely identifying a user (e.g. username, email, current session id, …)
	 * @param secret the current secret for the user identified by {@code handle}
	 * @param update the updated secret for the user identified by {@code handle}
	 *
	 * @return a value result with the current permit for the user identified by {@code handle}, if successful logged in
	 * using the current {@code secret} and associated with the {@code updated} one; an error result with a
	 * roster-specific error tag, otherwise
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public Result<Permit, String> login(final String handle, final String secret, final String update);

	/**
	 * Logs a user out.
	 *
	 * @param handle a handle uniquely identifying a user (e.g. username, email, current session id, …)
	 *
	 * @return a value result with the current permit for the user identified by {@code handle}, if successful logged
	 * out; an error result with a roster-specific error tag, otherwise
	 *
	 * @throws NullPointerException if {@code handle} is null
	 */
	public Result<Permit, String> logout(final String handle);

}
