/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.gate.rosters;

import com.metreeca.gate.*;
import com.metreeca.rest.Result;
import com.metreeca.tray.rdf.Graph;

import org.eclipse.rdf4j.model.*;
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

	private Result<String, String> digest(final Resource user) {
		return graph.query(connection -> {
			return stream(connection.getStatements(user, Gate.secret, null))

					.map(Statement::getObject)
					.map(Value::stringValue)
					.map(Result::<String, String>Value)

					.findFirst()
					.orElseGet(() -> Result.Error(CredentialsInvalid));

		});
	}

	private Result<String, String> digest(final Resource user, final String secret) {
		return digest(user).process(digest ->
				this.digest.verify(secret, digest) ? Value(digest) : Error(CredentialsInvalid)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Result<Permit, String> profile(final IRI user, final String digest) {
		return graph.query(connection -> {

			return profiler.apply(connection, user)

					.map(permit -> new Permit(
							hash(permit.hash(), digest),
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
