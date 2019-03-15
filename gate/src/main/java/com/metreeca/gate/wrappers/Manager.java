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

package com.metreeca.gate.wrappers;

import com.metreeca.form.things.Values;
import com.metreeca.gate.Permit;
import com.metreeca.gate.Roster;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Worker;

import io.jsonwebtoken.*;
import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.crypto.KeyGenerator;
import javax.crypto.spec.SecretKeySpec;
import javax.json.JsonObject;
import javax.json.JsonString;

import static com.metreeca.form.things.Codecs.UTF8;
import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Maps.map;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.gate.Coffer.coffer;
import static com.metreeca.gate.Roster.CredentialsInvalid;
import static com.metreeca.gate.Roster.roster;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.bodies.JSONBody.json;
import static com.metreeca.tray.Tray.tool;

import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Single page app session manager.
 *
 * <p>Manages session lifecycle using permits issued by shared {@link Roster#roster() roster} tool.</p>
 */
public final class Manager implements Wrapper {

	/**
	 * The id of the coffer entry containing the key string used for signing JWT tokens ({@code
	 * com.metreeca.gate.wrapper.manager.key}).
	 */
	public static final String KeyCofferId=Manager.class.getName().toLowerCase(Locale.ROOT)+".key";


	public static final long Minutes=60*1000;
	public static final long Hours=60*Minutes;
	public static final long Days=24*Hours;

	public static final String TicketMalformed="ticket-malformed";
	public static final String SessionExpired="session-expired";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final SignatureAlgorithm algorithm=SignatureAlgorithm.HS512;

	private static final Pattern BearerPattern=Pattern.compile("\\s*Bearer\\s*(?<token>\\S*)\\s*");


	private static Failure malformed(final String error) {
		return new Failure().status(Response.BadRequest).error(error);
	}

	private static Failure forbidden(final String error) {
		return new Failure().status(Response.Forbidden).error(error);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String path;

	private final long soft;
	private final long hard;


	private final Handler endpoint=new Worker().post(create());

	private final Roster roster=tool(roster());

	private final Key key=tool(coffer())

			.get(KeyCofferId)

			.map(string -> // generate from supplied key string
					(Key)new SecretKeySpec(string.getBytes(UTF8), algorithm.getJcaName())
			)

			.orElseGet(() -> { // generate random key
				try {

					final KeyGenerator generator=KeyGenerator.getInstance(algorithm.getJcaName());

					generator.init(algorithm.getMinKeyLength());

					return generator.generateKey();

				} catch ( final NoSuchAlgorithmException e ) {
					throw new RuntimeException(e);
				}
			});


	/**
	 * Creates a session manager.
	 *
	 * @param path the root relative path of the virtual session endpoint
	 * @param soft the soft session duration (ms); after the soft duration has expired the session is automatically
	 *             extended on further activity, provided the {@linkplain Permit#hash() hash} of the user permit
	 *             didn't change since the session was opened
	 * @param hard the hard session duration (ms); after the hard duration has expired the session is closed, unless
	 *             automatically extended
	 *
	 * @throws NullPointerException     if {@code path} is null
	 * @throws IllegalArgumentException if {@code path} is not root relative or either {@code soft} or {@code hard} is
	 *                                  less than or equal to 0
	 */
	public Manager(final String path, final long soft, final long hard) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( !path.startsWith("/") ) {
			throw new IllegalArgumentException("not a root relative path <"+path+">");
		}

		if ( soft <= 0 ) {
			throw new IllegalArgumentException("illegal soft duration <"+soft+">");
		}

		if ( hard <= 0 ) {
			throw new IllegalArgumentException("illegal hard duration <"+hard+">");
		}

		this.path=path;

		this.soft=soft;
		this.hard=hard;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return handler
				.with(endpoint())
				.with(challenger())
				.with(authenticator());
	}


	//// Wrappers //////////////////////////////////////////////////////////////////////////////////////////////////////

	/***
	 * @return a wrapper managing the virtual session endpoint
	 */
	private Wrapper endpoint() {
		return handler -> request -> request.path().equals(path) ? endpoint.handle(request) : handler.handle(request);
	}

	/**
	 * @return a wrapper adding authentication challenge to unauthorized responses, unless already provided by nested
	 * authorization schemes
	 */
	private Wrapper challenger() {
		return handler -> request -> handler.handle(request).map(response ->
				response.status() == Response.Unauthorized && response.headers("WWW-Authenticate").isEmpty()
						? response.header("WWW-Authenticate", format("Bearer realm=\"%s\"", request.base()))
						: response
		);
	}

	/**
	 * @return a wrapper managing token-based authentication
	 */
	private Wrapper authenticator() {
		return handler -> request -> {

			// !!! handle token in form/query parameter (https://tools.ietf.org/html/rfc6750#section-2)

			final long now=currentTimeMillis();
			final String authorization=request.header("Authorization").orElse("");

			return token(authorization)

					// bearer token > authenticate

					.map(token -> session(token)

							// authenticated > authorize and handle request

							.map(session -> (now >= session.issued()+hard) ? reject(request)
									: (now >= session.issued()+soft) ? extend(request, handler, session)
									: handle(request, handler, session))

							// not authenticated > report error

							.orElseGet(() -> request.reply(response -> response
									.status(Response.Unauthorized)
									.header("WWW-Authenticate", format(
											"Bearer realm=\"%s\", error=\"invalid_token\"", response.request().base()
									))
							))

					)

					// no bearer token > fall-through to other authorization schemes

					.orElseGet(() -> handler.handle(request));
		};
	}


	private Responder reject(final Request request) {
		return request.reply(forbidden(SessionExpired));
	}

	private Responder extend(final Request request, final Handler handler, final Session session) {
		return permit(session.user()).fold( // try to update the permit

				permit -> session.hash().equals(permit.hash()) ? // account not modified

						// handle the request

						handler.handle(request
								.user(permit.user())
								.roles(permit.roles())
						)

								// generate a new token for the extended session

								.map(response -> response.success()
										? response.header("Authorization", bearer(permit))
										: response
								)

						: request.reply(forbidden(SessionExpired)), // account modified

				request::reply

		);
	}

	private Responder handle(final Request request, final Handler handler, final Session session) {
		return handler

				.handle(request
						.user(session.user())
						.roles(session.roles())
				)

				.map(response -> response.success() // present again the current token
						? response.header("Authorization", request.header("Authorization").orElse(""))
						: response
				);
	}


	//// Endpoint Methods //////////////////////////////////////////////////////////////////////////////////////////////

	private Handler create() {
		return request -> request.reply(response -> request.body(json()).fold(

				ticket -> permit(ticket).fold(

						permit -> response.status(Response.Created)
								.header("Authorization", bearer(permit))
								.body(json(), permit.profile()),

						response::map

				),

				response::map

		));
	}


	//// Permit Retrieval //////////////////////////////////////////////////////////////////////////////////////////////

	private Result<Permit, Failure> permit(final IRI user) {
		return roster.lookup(user)
				.error(Manager::forbidden);
	}

	private Result<Permit, Failure> permit(final JsonObject ticket) {
		return ticket.values().stream().allMatch(value -> value instanceof JsonString)

				? ticket.keySet().equals(set("handle", "secret")) ? signon(ticket)
				: ticket.keySet().equals(set("handle", "secret", "update")) ? update(ticket)
				: Error(malformed(TicketMalformed))

				: Error(malformed(TicketMalformed));

	}

	private Result<Permit, Failure> signon(final JsonObject ticket) {
		return roster.resolve(ticket.getString("handle"))
				.map(user -> roster.verify(user, ticket.getString("secret")).error(Manager::forbidden))
				.orElseGet(() -> Error(forbidden(CredentialsInvalid)));
	}

	private Result<Permit, Failure> update(final JsonObject ticket) {
		return roster.resolve(ticket.getString("handle"))
				.map(user -> roster.verify(user, ticket.getString("secret"), ticket.getString("update")).error(Manager::forbidden))
				.orElseGet(() -> Error(forbidden(CredentialsInvalid)));
	}


	//// Token Codecs //////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<String> token(final CharSequence authorization) {
		return Optional
				.of(BearerPattern.matcher(authorization))
				.filter(Matcher::matches)
				.map(matcher -> matcher.group("token"));
	}

	private String bearer(final Permit permit) {
		return format("Bearer %s", token(new Session(permit)));
	}


	private String token(final Session session) {
		return Jwts.builder()

				.setClaims(map(

						entry("issued", session.issued()),
						entry("hash", session.hash()),

						entry("user", session.user().stringValue()),
						entry("roles", session.roles().stream().map(Value::stringValue).collect(toList()))

				))

				.signWith(key, algorithm)

				.compressWith(CompressionCodecs.GZIP)
				.compact();
	}

	private Optional<Session> session(final String token) {
		try {

			final Claims claims=Jwts.parser()
					.setSigningKey(key)
					.parseClaimsJws(token)
					.getBody();

			final long issued=Optional
					.ofNullable(claims.get("issued", Long.class))
					.orElseThrow(IllegalArgumentException::new);

			final String hash=Optional
					.ofNullable(claims.get("hash", String.class))
					.orElseThrow(IllegalArgumentException::new);

			final IRI user=Optional
					.ofNullable(claims.get("user", String.class))
					.map(Values::iri)
					.orElseThrow(IllegalArgumentException::new);

			final Set<IRI> roles=Optional
					.ofNullable(claims.get("roles", List.class))
					.map(list -> (List<String>)list)
					.map(Collection::stream)
					.orElseThrow(IllegalArgumentException::new)
					.map(Values::iri)
					.collect(toSet());

			return Optional.of(new Session(
					issued, hash,
					user, roles
			));

		} catch ( final Exception e ) {

			return Optional.empty();

		}
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Session {

		private final long issued;
		private final String hash;

		private final IRI user;
		private final Set<IRI> roles;


		private Session(final Permit permit) {
			this(
					currentTimeMillis(), permit.hash(),
					permit.user(), permit.roles()
			);
		}

		private Session(
				final long issued, final String hash,
				final IRI user, final Set<IRI> roles
		) {

			this.hash=hash;
			this.issued=issued;

			this.user=user;
			this.roles=roles;

		}


		private long issued() { return issued; }

		private String hash() { return hash; }


		private IRI user() { return user; }

		private Set<IRI> roles() { return roles; }

	}

}
