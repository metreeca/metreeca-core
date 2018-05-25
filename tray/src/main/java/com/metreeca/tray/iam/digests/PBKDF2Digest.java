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

package com.metreeca.tray.iam.digests;

import com.metreeca.spec.things.Transputs;
import com.metreeca.tray.iam.Digest;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;


/**
 * PBKDF2 password digest.
 */
public final class PBKDF2Digest extends Digest {

	/**
	 * Opaque digest algorithm identifying tag.
	 */
	public static final String Tag=Base64.getEncoder().encodeToString("PBKDF2/1".getBytes(Transputs.UTF8));

	private static final int Length=24; // salt length
	private static final int Rounds=1000; // encryption rounds

	private static final SecureRandom random=new SecureRandom();

	private static final Pattern DigestPattern=Pattern.compile("(?<tag>.*):(?<rounds>\\d+):(?<salt>.+):(?<hash>.+)");


	public static void main(final String... args) {
		System.out.println(new PBKDF2Digest().digest("secret"));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String digest(final String secret) {

		if ( secret == null ) {
			throw new NullPointerException("null pwd");
		}

		final byte[] salt=salt(Length);
		final byte[] hash=hash(Length, secret.toCharArray(), salt, Rounds);

		return Tag+":"+Rounds+":"+encode(salt)+":"+encode(hash);
	}

	@Override public boolean verify(final String secret, final String digest) throws IllegalArgumentException {

		if ( secret == null ) {
			throw new NullPointerException("null pwd");
		}

		if ( digest == null ) {
			throw new NullPointerException("null digest");
		}

		final Matcher matcher=DigestPattern.matcher(digest);

		if ( !matcher.matches() ) {
			throw new IllegalArgumentException("malformed digest ["+digest+"]");
		}

		final String tag=matcher.group("tag");

		final byte[] salt=decode(matcher.group("salt"));
		final byte[] hash=decode(matcher.group("hash"));

		if ( Tag.equals(tag) ) {

			return equals(hash, hash(Length, secret.toCharArray(), salt, Rounds));

		} else {

			throw new UnsupportedOperationException("unsupported digest ["+tag+"]");

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Generates a random password salt.
	 *
	 * @param bytes the expected length of the hash in bytes
	 *
	 * @return a random password salt
	 */
	private byte[] salt(final int bytes) {

		final byte[] salt=new byte[bytes];

		random.nextBytes(salt);

		return salt;
	}

	/**
	 * Computes the PBKDF2 hash of a password.
	 *
	 * @param bytes      the expected length of the hash in bytes
	 * @param pwd        the password to be hashed
	 * @param salt       the password salt
	 * @param iterations the iteration count (slowness factor)
	 *
	 * @return the PBDKF2 hash of the given {@code password}
	 */
	private byte[] hash(final int bytes, final char[] pwd, final byte[] salt, final int iterations) {
		try {

			final KeySpec spec=new PBEKeySpec(pwd, salt, iterations, bytes*8);
			final SecretKeyFactory skf=SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");

			return skf.generateSecret(spec).getEncoded();

		} catch ( final NoSuchAlgorithmException|InvalidKeySpecException e ) {

			throw new UnsupportedOperationException(e);

		}
	}


	/**
	 * Compares two byte arrays in constant time.
	 *
	 * <p>This method is used to prevent password hashes extraction with timing attacks.</p>
	 *
	 * @param x the first byte array to be compared
	 * @param y the second byte array to be compared
	 *
	 * @return {@code true} if the byte arrays are equal, false otherwise
	 */
	private boolean equals(final byte[] x, final byte[] y) {

		int trace=x.length^y.length;

		for (int i=0; i < x.length && i < y.length; i++) { trace|=x[i]^y[i]; }

		return trace == 0;
	}


	private String encode(final byte... bytes) {
		return Base64.getEncoder().encodeToString(bytes);
	}

	private byte[] decode(final String chars) {
		return Base64.getDecoder().decode(chars);
	}

}
