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
 * Secret policy.
 *
 * <p>Verifies secret conformance to well-formedness rules.</p>
 */
@FunctionalInterface public interface Policy {

	/**
	 * Retrieves the default policy factory.
	 *
	 * @return the default policy factory, which throws an exception reporting the tool as undefined
	 */
	public static Supplier<Policy> policy() {
		return () -> { throw new IllegalStateException("undefined policy tool"); };
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Verifies a secret.
	 *
	 * @param handle a handle identifying the user the secret belongs to
	 * @param secret the secret to be verified
	 *
	 * @return {@code true}, if {@code secret} conforms to the well-formedness rules defined by this policy; {@code
	 * false}, otherwise
	 *
	 * @throws NullPointerException if either {@code handle} or {@code secret} is null
	 */
	public boolean verify(final String handle, final String secret);

}
