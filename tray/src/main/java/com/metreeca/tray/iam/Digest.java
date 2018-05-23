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

import com.metreeca.tray.iam.digests.PBKDF2Digest;


/**
 * Password digest.
 */
public abstract class Digest {

	public static final com.metreeca.tray.Tool<Digest> Tool=tools -> new PBKDF2Digest(); // !!! auto-verify switch on digest tag


	public abstract String digest(final String secret);

	public abstract boolean verify(final String secret, final String digest) throws IllegalArgumentException;

}
