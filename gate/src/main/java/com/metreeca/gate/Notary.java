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

import com.metreeca.tray.sys.Vault;

import io.jsonwebtoken.*;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;

import static com.metreeca.form.things.Codecs.UTF8;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Vault.vault;


/**
 * JWT token notary.
 *
 * <p>Creates and verifies signed claims-based JWT tokens.</p>
 *
 * @see <a href="https://tools.ietf.org/html/rfc7519">RFC 7519 - JSON Web Token (JWT)</a>
 */
public final class Notary {

	/**
	 * The id of the {@linkplain Vault vault} entry containing the key used for signing JWT tokens ({@code
	 * com.metreeca.gate.notary:key}); if no key is provided a random one is generated automatically.
	 */
	public static final String KeyVaultId=Notary.class.getName().toLowerCase(Locale.ROOT)+":key";


	/**
	 * Retrieves the default JWT token notary factory.
	 *
	 * @return the default JWT token notary factory, configured with the {@link SignatureAlgorithm#HS256} signing
	 * algorithm and the signing key {@linkplain #KeyVaultId retrieved} from the shared {@linkplain Vault vault}, if one
	 * is available, or a random key, otherwise
	 */
	public static Supplier<Notary> notary() {

		return () -> tool(vault()).get(KeyVaultId)

				.map(key -> new Notary(SignatureAlgorithm.HS256, key)) // generate from supplied key string

				.orElseGet(() -> new Notary(SignatureAlgorithm.HS256));  // generate random key

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final SignatureAlgorithm algorithm;


	private final Key key;


	public Notary(final SignatureAlgorithm algorithm) {

		if ( algorithm == null ) {
			throw new NullPointerException("null algorithm");
		}

		try {

			final KeyGenerator generator=KeyGenerator.getInstance(algorithm.getJcaName());

			generator.init(algorithm.getMinKeyLength());

			this.algorithm=algorithm;
			this.key=generator.generateKey();

		} catch ( final NoSuchAlgorithmException e ) {
			throw new RuntimeException(e);
		}
	}

	public Notary(final SignatureAlgorithm algorithm, final String key) {

		if ( algorithm == null ) {
			throw new NullPointerException("null algorithm");
		}

		if ( key == null ) {
			throw new NullPointerException("null key");
		}

		this.algorithm=algorithm;
		this.key=new SecretKeySpec(key.getBytes(UTF8), this.algorithm.getJcaName());
	}

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


	public String create(final Consumer<Claims> consumer) {

		if ( consumer == null ) {
			throw new NullPointerException("null consumer");
		}

		final Claims claims=Jwts.claims();

		consumer.accept(claims);

		return Jwts.builder()
				.addClaims(claims)
				.signWith(key, algorithm)
				.compressWith(CompressionCodecs.GZIP)
				.compact();
	}

	public Optional<Claims> verify(final String token) {

		if ( token == null ) {
			throw new NullPointerException("null token");
		}

		try {

			return Optional.of(Jwts.parser()
					.setSigningKey(key)
					.parseClaimsJws(token)// validates signature and expiration
					.getBody()
			);

		} catch ( final Exception e ) {

			return Optional.empty();

		}

	}

}
