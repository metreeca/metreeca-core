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
import static com.metreeca.gate.Digest.digest;
import static com.metreeca.gate.Policy.policy;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.tray.Tray.tool;

import static org.eclipse.rdf4j.common.iteration.Iterations.stream;


/**
 * Basic user roster.
 *
 * <p>Manages user credentials stored in the shared {@link Graph#Factory graph} tool using the shared {@link
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

	private final Graph graph=tool(Graph.Factory);

	private final Digest digest=tool(digest());
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

			return secret(user, secret).process(s -> profile(user, secret));

		});
	}

	@Override public Result<Permit, String> verify(final IRI user, final String secret, final String update) {
		return graph.update(connection -> policy.verify(user, update)

				? secret(user, secret)

				.process(s -> profile(user, update))

				.value(permit -> {

					connection.remove(user, Gate.secret, null);
					connection.add(user, Gate.secret, literal(digest.digest(update)));

					return permit;

				})

				: Error(CredentialsIllegal));
	}


	@Override public Result<Permit, String> lookup(final IRI user) {
		return secret(user).process(secret -> profile(user, secret));
	}

	@Override public Result<Permit, String> insert(final IRI user, final String secret) {
		return graph.update(connection -> policy.verify(user, secret)

				? profile(user, secret)

				.value(permit -> {

					connection.remove(user, Gate.secret, null);
					connection.add(user, Gate.secret, literal(digest.digest(secret)));

					return permit;

				})

				: Error(CredentialsIllegal));
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

	private Result<String, String> secret(final Resource user) {
		return graph.query(connection -> {
			return stream(connection.getStatements(user, Gate.secret, null))

					.map(Statement::getObject)
					.map(Value::stringValue)
					.map(Result::<String, String>Value)

					.findFirst()
					.orElseGet(() -> Result.Error(CredentialsInvalid));

		});
	}

	private Result<String, String> secret(final Resource user, final String secret) {
		return secret(user).process(expected ->
				digest.verify(secret, expected) ? Value(secret) : Error(CredentialsInvalid)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Result<Permit, String> profile(final IRI user, final String secret) {
		return graph.query(connection -> {

			return profiler.apply(connection, user)

					.map(permit -> new Permit(
							hash(permit.hash(), secret),
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
