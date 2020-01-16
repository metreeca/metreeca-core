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


import com.metreeca.rest.services.Clock;
import com.metreeca.rest.services.Vault;

import io.jsonwebtoken.*;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import static com.metreeca.rest.services.Clock.clock;
import static com.metreeca.rest.services.Vault.vault;
import static com.metreeca.rest.Context.service;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * JWT notary.
 *
 * <p>Creates and verifies signed claims-based JWT tokens.</p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc7519">RFC 7519 - JSON Web Token (JWT)</a>
 */
public final class Notary {

	/**
	 * The id of the shared {@linkplain Vault vault} entry containing the key to be used for signing JWT tokens ({@code
	 * com.metreeca.gate.notary:key}).
	 */
	public static final String KeyVaultId=Notary.class.getName().toLowerCase(Locale.ROOT)+":key";

	/**
	 * Allowed clock skew in seconds.
	 */
	public static final int AllowedClockSkew=5;


	/**
	 * Retrieves the default JWT notary factory.
	 *
	 * @return the default JWT notary factory, configured with the {@link SignatureAlgorithm#HS256} signing algorithm
	 * and the signing key {@linkplain #KeyVaultId retrieved} from the shared {@linkplain Vault vault}, if one is
	 * available, or a random key, otherwise
	 */
	public static Supplier<Notary> notary() {

		return () -> new Notary(SignatureAlgorithm.HS256, service(vault()).get(KeyVaultId).orElse(""));

	}


	private static SecretKey key(final SignatureAlgorithm algorithm, final String key) {
		if ( key.isEmpty() ) { // generate random key

			try {

				final KeyGenerator generator=KeyGenerator.getInstance(algorithm.getJcaName());

				generator.init(algorithm.getMinKeyLength());

				return generator.generateKey();

			} catch ( final NoSuchAlgorithmException e ) {
				throw new RuntimeException(e);
			}

		} else { // convert string to key

			return new SecretKeySpec(key.getBytes(UTF_8), algorithm.getJcaName());

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final SignatureAlgorithm algorithm;
	private final Key key;

	private final Clock clock=service(clock());


	/**
	 * Creates a JWT token notary.
	 *
	 * @param algorithm the algorithm to be used for signing tokens; tokens are signed using a random key generated with
	 *                  {@link KeyGenerator}
	 *
	 * @throws NullPointerException if {@code algorithm} is null
	 */
	public Notary(final SignatureAlgorithm algorithm) {

		if ( algorithm == null ) {
			throw new NullPointerException("null algorithm");
		}


		this.algorithm=algorithm;
		this.key=key(algorithm, "");

	}

	/**
	 * Creates a JWT token notary.
	 *
	 * @param algorithm the algorithm to be used for signing tokens
	 * @param key       the key to be used for signing tokens; if empty tokens are signed using a random key generated
	 *                  with {@link KeyGenerator}; must satisfy length requirements for {@code algorithm}
	 *
	 * @throws NullPointerException if either {@code algorithm} or {@code key} is null
	 */
	public Notary(final SignatureAlgorithm algorithm, final String key) {

		if ( algorithm == null ) {
			throw new NullPointerException("null algorithm");
		}

		if ( key == null ) {
			throw new NullPointerException("null key");
		}

		this.algorithm=algorithm;
		this.key=key(algorithm, key);
	}

	/**
	 * Creates a JWT token notary.
	 *
	 * @param algorithm the algorithm to be used for signing tokens
	 * @param key       the key to be used for signing tokens; must satisfy length requirements for {@code algorithm}
	 *
	 * @throws NullPointerException if either {@code algorithm} or {@code key} is null
	 */
	public Notary(final SignatureAlgorithm algorithm, final Key key) {

		if ( algorithm == null ) {
			throw new NullPointerException("null algorithm");
		}

		if ( key == null ) {
			throw new NullPointerException("null key");
		}

		this.algorithm=algorithm;
		this.key=key;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Creates a signed claims-based JWT token.
	 *
	 * @param customizer claims customizer for the new token
	 *
	 * @return a signed, compact, compressed, URL-safe serialized JWT token
	 *
	 * @throws NullPointerException if {@code customizer} is null
	 */
	public String create(final Consumer<Claims> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		final Claims claims=Jwts.claims();

		customizer.accept(claims);

		return Jwts.builder()
				.addClaims(claims)
				.signWith(key, algorithm)
				.compressWith(CompressionCodecs.GZIP)
				.compact();
	}

	/**
	 * Verifies a signed claims-based JWT token.
	 *
	 * @param token the serialized JWT token
	 *
	 * @return the claims associated with {@code token}, if successfully verified; an empty optional, otherwise
	 *
	 * @throws NullPointerException if {@code token} is null
	 */
	public Optional<Claims> verify(final String token) {

		if ( token == null ) {
			throw new NullPointerException("null token");
		}

		try {

			return Optional.of(Jwts.parser()
					.setClock(() -> new Date(clock.time()))
					.setAllowedClockSkewSeconds(AllowedClockSkew)
					.setSigningKey(key)
					.parseClaimsJws(token)// validates signature and expiration
					.getBody()
			);

		} catch ( final Exception e ) {

			return Optional.empty();

		}

	}

}
