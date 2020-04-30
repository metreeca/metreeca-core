/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Objects;
import java.util.function.Supplier;

import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.Arrays.stream;


/**
 * Opaque id generator.
 *
 * <p>Manages random and hash-based opaque id generation.</p>
 */
public interface Crypto {

	/**
	 * The default secure random generator algorithm ({@value}).
	 */
	public static String SecureRandomDefault="SHA1PRNG";

	/**
	 * The default message digest algorithm ({@value}).
	 */
	public static String MessageDigestDefault="SHA-1";


	/**
	 * Retrieves the default opaque id generator factory.
	 *
	 * @return the default opaque id generator factory, which creates generators based on the default secure random
	 * 		generator ({@value #SecureRandomDefault}) and message digest ({@value #MessageDigestDefault}) algorithms
	 */
	public static Supplier<Crypto> crypto() {
		return () -> new Crypto() {

			private final SecureRandom random=random(SecureRandomDefault);
			private final MessageDigest digest=digest(MessageDigestDefault);


			@Override public byte[] id(final int length) {

				if ( length <= 0 ) {
					throw new IllegalArgumentException("id length less than or equal to zero");
				}

				synchronized ( random ) {

					final byte[] id=new byte[length];

					random.nextBytes(id);

					return id;
				}
			}

			@Override public byte[] id(final byte[]... data) {

				if ( data == null ) {
					throw new NullPointerException("null data");
				}
				if ( stream(data).anyMatch(Objects::isNull) ) {
					throw new NullPointerException("null data chunk");
				}

				synchronized ( digest ) {

					for (final byte[] chunk : data) { digest.digest(chunk); }

					return digest.digest();

				}
			}

		};
	}


	/**
	 * Creates a secure random generator instance.
	 *
	 * @param algorithm the id of the required secure random generator algorithm
	 *
	 * @return a new secure random generator instance based on the required {@code algorithm}
	 *
	 * @throws NullPointerException          if {@code algorithm} is null
	 * @throws UnsupportedOperationException if {@code algorithm} is not supported
	 */
	public static SecureRandom random(final String algorithm) {

		if ( algorithm == null ) {
			throw new NullPointerException("null algorithm id");
		}

		try {

			return SecureRandom.getInstance(algorithm);

		} catch ( final NoSuchAlgorithmException e ) {

			throw new UnsupportedOperationException("unsupported secure random generator algorithm ["+algorithm+"]", e);

		}
	}

	/**
	 * Creates a message digest instance.
	 *
	 * @param algorithm the id of the required message digest algorithm
	 *
	 * @return a new message digest instance based on the required {@code algorithm}
	 *
	 * @throws NullPointerException          if {@code algorithm} is null
	 * @throws UnsupportedOperationException if {@code algorithm} is not supported
	 */
	public static MessageDigest digest(final String algorithm) {

		if ( algorithm == null ) {
			throw new NullPointerException("null algorithm id");
		}

		try {

			return MessageDigest.getInstance(algorithm);

		} catch ( final NoSuchAlgorithmException e ) {

			throw new UnsupportedOperationException("unsupported secure message digest algorithm ["+algorithm+"]", e);

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Generates a random id.
	 *
	 * @param length the length in length of the generated random id
	 *
	 * @return a random id of {@code length} bytes
	 *
	 * @throws IllegalArgumentException if {@code length} is less than or equal to zero
	 */
	public byte[] id(final int length);

	/**
	 * Generates an opaque hash-based id.
	 *
	 * @param data the data to be hashed
	 *
	 * @return an opaque hash-based id generated from the supplied {@code data}
	 *
	 * @throws NullPointerException if {@code data} is null or contains null values
	 */
	public byte[] id(final byte[]... data);


	/**
	 * Generates a random token.
	 *
	 * @param length the length in bytes of the generated random token
	 *
	 * @return a textually {@linkplain #encode(byte...) encoded} {@linkplain #id(int) random id} of {@code length} bytes
	 *
	 * @throws IllegalArgumentException if {@code length} is less than or equal to zero
	 */
	public default String token(final int length) {

		if ( length <= 0 ) {
			throw new IllegalArgumentException("id length less than or equal to zero");
		}

		return encode(id(length));
	}

	/**
	 * Generates an opaque hash-based token.
	 *
	 * @param text the text to be hashed
	 *
	 * @return a textually {@linkplain #encode(byte...) encoded} {@linkplain #id(byte[][]) hash-based id} generated from
	 * 		the supplied {@code text}
	 *
	 * @throws NullPointerException if {@code text} is null or contains null values
	 */
	public default String token(final String... text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		if ( stream(text).anyMatch(Objects::isNull) ) {
			throw new IllegalArgumentException("null text chunk");
		}

		return encode(id(stream(text).map(chunk -> chunk.getBytes(UTF_8)).toArray(byte[][]::new)));
	}


	/**
	 * Encodes an opaque id.
	 *
	 * @param id the id to be encoded
	 *
	 * @return a token generated by textually encoding {@code id} (by default using the {@linkplain Base64} encoding
	 * 		scheme)
	 */
	public default String encode(final byte... id) {
		return Base64.getEncoder().encodeToString(id);
	}

	/**
	 * Decodes an opaque token.
	 *
	 * @param token the token to be decoded
	 *
	 * @return the id generated by textually decoding {@code token} (by default using the {@linkplain Base64} encoding
	 * 		scheme)
	 *
	 * @throws NullPointerException if {@code token} is null
	 */
	public default byte[] decode(final String token) {

		if ( token == null ) {
			throw new NullPointerException("null token");
		}

		return Base64.getDecoder().decode(token);
	}

}
