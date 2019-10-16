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
 * Secret digest.
 *
 * <p>Digests and verifies secrets.</p>
 */
public interface Digest {

	/**
	 * Retrieves the default digest factory.
	 *
	 * @return the default digest factory, which throws an exception reporting the service as undefined
	 */
	public static Supplier<Digest> digest() {
		return () -> { throw new IllegalStateException("undefined digest service"); };
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Digests a secret.
	 *
	 * @param secret the secret to be digested
	 *
	 * @return the digest derived from {@code secret}
	 *
	 * @throws NullPointerException if {@code secret} is null
	 */
	public String digest(final String secret);

	/**
	 * Verifies a secret.
	 *
	 * @param secret the secret to be verified
	 * @param digest the digest derived from the expected secret
	 *
	 * @return {@code true} if {@code secret} matches the expected {@code digest}; {@code false} otherwise
	 *
	 * @throws NullPointerException     if either {@code secret} or {@code digest} is null
	 * @throws IllegalArgumentException if {@code digest} is non a valid digest representation
	 */
	public boolean verify(final String secret, final String digest) throws IllegalArgumentException;

}
