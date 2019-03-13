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

import java.util.function.Supplier;


/**
 * User roster.
 */
public interface Roster {

	/**
	 * Roster factory.
	 *
	 * <p>By default throws an exception reporting the user roster as undefined.</p>
	 */
	public static Supplier<Roster> Factory=() -> { throw new IllegalStateException("undefined roster tool"); };


	public static String CredentialsRejected="credentials-rejected"; // unknown user or invalid secret
	public static String CredentialsDisabled="credentials-disabled";
	public static String CredentialsPending="credentials-pending";
	public static String CredentialsExpired="credentials-expired";
	public static String CredentialsIllegal="credentials-illegal"; // secret unacceptable by policy

	public static String CredentialsLocked="credentials-locked"; // account locked by system


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Permit profile(final String alias);


	public Permit acquire(final String alias, final String secret);

	public Permit acquire(final String alias, final String secret, final String update);


	public Permit refresh(final String alias);

	public Permit release(final String alias);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

}
