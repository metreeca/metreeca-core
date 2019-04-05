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

package com.metreeca.gate.rosters;

import com.metreeca.gate.*;
import com.metreeca.rest.Result;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.repository.RepositoryConnection;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;
import java.util.Optional;
import java.util.function.BiFunction;

import static com.metreeca.form.things.Codecs.UTF8;
import static com.metreeca.form.things.Values.literal;
import static com.metreeca.gate.Policy.policy;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.rdf.Graph.graph;

import static org.eclipse.rdf4j.common.iteration.Iterations.stream;


/**
 * Basic user roster.
 *
 * <p>Manages user credentials stored in the shared {@link Graph#graph() graph} tool using the shared {@link
 * Digest#digest() digest} and {@link Policy#policy() policy} tools.</p>
 *
 * <p>User secret digests are stored in the graph with the following layout:</p>
 *
 * <pre>{@code
 * <user> gate:secret 'digest' .
 * }</pre>
 */
public final class BasicRoster implements Roster {

	private static MessageDigest hashing() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch ( final NoSuchAlgorithmException unexpected ) {
			throw new RuntimeException(unexpected);
		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final BiFunction<RepositoryConnection, String, Optional<IRI>> resolver;
	private final BiFunction<RepositoryConnection, IRI, Optional<Permit>> profiler;

	private final Graph graph=tool(graph());

	private final Digest digest=tool(Digest.digest());
	private final Policy policy=tool(policy());


	/**
	 * Creates a basic user roster.
	 *
	 * @param resolver a function mapping from a user handle (e.g. an email address) to an IRI uniquely identifying the
	 *                 user; returns an empty optional if the user is not known
	 * @param profiler a function mapping from a IRI uniquely identifying a user to a permit describing the user
	 *                 (including current state hash, granted roles and public profile; returns an empty optional if the
	 *                 user is not known
	 */
	public BasicRoster(
			final BiFunction<RepositoryConnection, String, Optional<IRI>> resolver,
			final BiFunction<RepositoryConnection, IRI, Optional<Permit>> profiler
	) {

		if ( resolver == null ) {
			throw new NullPointerException("null resolver");
		}

		if ( profiler == null ) {
			throw new NullPointerException("null profiler");
		}

		this.resolver=resolver;
		this.profiler=profiler;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Result<IRI, String> resolve(final String handle) {

		if ( handle == null ) {
			throw new NullPointerException("null handle");
		}

		return graph.query(connection -> {

			return resolver.apply(connection, handle)
					.map(Result::<IRI, String>Value)
					.orElseGet(() -> Error(CredentialsInvalid));

		});
	}


	@Override public Result<Permit, String> verify(final IRI user, final String secret) {
		return graph.query(connection -> {

			return digest(user, secret).process(_digest -> profile(user, _digest));

		});
	}

	@Override public Result<Permit, String> verify(final IRI user, final String secret, final String update) {
		return digest(user, secret).process(_digest -> insert(user, update));
	}


	@Override public Result<Permit, String> lookup(final IRI user) {
		return digest(user).process(_digest -> profile(user, _digest));
	}

	@Override public Result<Permit, String> insert(final IRI user, final String secret) {
		return graph.update(connection -> {

			return Optional.of(secret)

					.filter(_secret -> policy.verify(user, secret))

					.map(_secret -> digest.digest(secret))

					.map(_digest -> profile(user, _digest).value(permit -> {

						connection.remove(user, Gate.secret, null);
						connection.add(user, Gate.secret, literal(_digest));

						return permit;

					}))

					.orElseGet(() -> Error(CredentialsIllegal));

		});
	}

	@Override public Result<Permit, String> remove(final IRI user) {
		return graph.update(connection -> {

			return lookup(user).value(permit -> {

				connection.remove(user, Gate.secret, null);

				return permit;

			});

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Retrieves the digest of the user secret.
	 *
	 * @param user an IRI uniquely identifying a user
	 *
	 * @return a value containing the digest of the user secret or an error if the digest is not defined
	 */
	private Result<String, String> digest(final IRI user) {
		return graph.query(connection -> {
			return stream(connection.getStatements(user, Gate.secret, null))

					.map(Statement::getObject)
					.map(Value::stringValue)
					.map(Result::<String, String>Value)

					.findFirst()
					.orElseGet(() -> Error(CredentialsInvalid));

		});
	}

	/**
	 * Retrieves and verifies the digest of the user secret.
	 *
	 * @param user   an IRI uniquely identifying a user
	 * @param secret the expected user secret
	 *
	 * @return a value containing the digest of the user secret or an error if the digest is either not defined or
	 * doesn't match the expected {@code secret}
	 */
	private Result<String, String> digest(final IRI user, final String secret) {
		return digest(user).process(_digest ->
				this.digest.verify(secret, _digest) ? Value(_digest) : Error(CredentialsInvalid)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Result<Permit, String> profile(final IRI user, final String digest) {
		return graph.query(connection -> {

			return profiler.apply(connection, user)

					.map(permit -> new Permit(
							hash(permit.hash(), digest), // update the hash taking into account the secret digest
							permit.user(),
							permit.roles(),
							permit.profile()
					))

					.map(Result::<Permit, String>Value)
					.orElseGet(() -> Error(CredentialsInvalid));

		});
	}


	private String hash(final String... chunks) {

		final MessageDigest hashing=hashing();

		for (final String chunk : chunks) {
			hashing.update(chunk.getBytes(UTF8));
		}

		return Base64.getEncoder().encodeToString(hashing.digest());
	}

}
