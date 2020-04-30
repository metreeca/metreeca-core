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

package com.metreeca.gate.wrappers;

import com.metreeca.gate.Crypto;
import com.metreeca.gate.Notary;
import com.metreeca.gate.Permit;
import com.metreeca.gate.Roster;
import com.metreeca.json.formats.JSONFormat;
import com.metreeca.rest.*;
import com.metreeca.rest.handlers.Worker;
import com.metreeca.rest.services.Clock;
import com.metreeca.rest.services.Logger;
import io.jsonwebtoken.Claims;

import javax.json.Json;
import javax.json.JsonObject;
import javax.json.JsonString;
import javax.json.JsonValue;
import java.net.URI;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.gate.Crypto.crypto;
import static com.metreeca.gate.Notary.notary;
import static com.metreeca.gate.Roster.roster;
import static com.metreeca.json.formats.JSONFormat.json;
import static com.metreeca.rest.Context.service;
import static com.metreeca.rest.Handler.handler;
import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.services.Clock.clock;
import static com.metreeca.rest.services.Logger.logger;
import static java.lang.Math.min;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.regex.Pattern.compile;


/**
 * Session manager.
 *
 * <p>Manages user authentication/authorization and session lifecycle using permits issued by shared {@link
 * Roster#roster() roster} tool.</p>
 *
 * <p>Provides a virtual session management handler at the {@linkplain #Manager(String, long, long) provided path}
 * with the methods described below.</p>
 *
 * <p><strong>Warning</strong> / Make sure XSS/XSRF {@linkplain Protector protection} is active when using session
 * managers.</p>
 *
 * <hr>
 *
 * <h3>Session Retrieval</h3>
 *
 * <pre>{@code GET <path>
 * Cookie: ID=<token> // optional}</pre>
 *
 * <p>Between session creation and deletion, any requests to the virtual session management handler will automatically
 * include the session token in a cookie header, enabling user authentication.</p>
 *
 * <p><strong>Active session</strong> / The current session is reported with a response including a session token in a
 * HTTP only session cookie, the session expiry date in milliseconds in the custom {@code timeout} directive of the
 * {@code Cache-Control} header and a JSON payload containing the user profile included in the {@linkplain
 * Permit#profile() permit} returned by the user {@linkplain Roster roster}.</p>
 *
 * <pre>{@code 200 OK
 * Set-Cooke: ID=<token>; Path=<base>; HttpOnly; Secure; SameSite=Lax
 * Cache-Control: no-store, timeout=1560940206395
 * Content-Type: application/json
 *
 * {
 *     …    // user profile
 * }}</pre>
 *
 *
 * <p><strong>No active session</strong> / The anonymous session is reported with a response including an empty session
 * token in a HTTP only self-expiring session cookie, a dummy session expiry date in milliseconds in the custom {@code
 * timeout} directive of the {@code Cache-Control} header and an empty JSON payload; malformed, invalid or expired
 * session tokens are ignored.</p>
 *
 * <pre>{@code 200 OK
 * Set-Cooke: ID=; Path=<base>; HttpOnly; Secure; SameSite=Lax; Max-Age=0
 * Cache-Control: no-store, timeout=0
 * Content-Type: application/json
 *
 * {}}</pre>
 *
 * <hr>
 *
 * <h3>Session Creation</h3>
 *
 * <pre>{@code POST <path>
 * Content-Type: application/json
 *
 * {
 *     "handle": "<handle>",            // user handle, e.g. an email address
 *     "secret": "<current password>",  // current user password
 *     "update": "<new password>"       // new user password (optional)
 * } }</pre>
 *
 * <p><strong>Successful credentials validation</strong> / A session is created and reported with a response including
 * a session token in a HTTP only session cookie, the session expiry date in milliseconds in the custom {@code timeout}
 * directive of the {@code Cache-Control} header and a JSON payload containing the user profile included in the
 * {@linkplain Permit#profile() permit} returned by the user {@linkplain Roster roster}.</p>
 *
 * <pre>{@code 200 OK
 * Set-Cooke: ID=<token>; Path=<base>; HttpOnly; Secure; SameSite=Lax
 * Cache-Control: no-store, timeout=1560940206395
 * Content-Type: application/json
 *
 * {
 *     …    // user profile
 * }}</pre>
 *
 * <p><strong>Failed credentials validation</strong> / Reported with a response providing a machine-readable error code
 * among those defined by {@link Roster}.</p>
 *
 * <pre>{@code 403 Forbidden
 * Content-Type: application/json
 *
 * {
 *      "error": "<error>"
 * }}</pre>
 *
 * <p><strong>Malformed session ticket</strong> / Reported with a response providing a machine-readable error code.</p>
 *
 * <pre>{@code 400 Bad Request
 * Content-Type: application/json
 *
 * {
 *      "error": "ticket-malformed"
 * }}</pre>
 *
 * <hr>
 *
 * <h3>Session Deletion</h3>
 *
 * <pre>{@code POST <path>
 * Cookie: ID=<token> // optional
 * Content-Type: application/json
 *
 * {}}</pre>
 *
 * <p><strong>Successful session deletion</strong> / The current session is delete and reported with a response
 * including an empty session token in a HTTP only self-expiring session cookie, a dummy session expiry date in
 * milliseconds in the custom {@code timeout} directive of the {@code Cache-Control} header and an empty JSON payload;
 * malformed, invalid and expired session tokens are ignored.</p>
 *
 * <pre>{@code 200 OK
 * Set-Cooke: ID=; Path=<base>; HttpOnly; Secure; SameSite=Lax; Max-Age=0
 * Cache-Control: no-store, timeout=0
 * Content-Type: application/json
 *
 * {}}</pre>
 *
 * <hr>
 *
 * <h3>Secured Resource Access</h3>
 *
 * <p>Between session creation and deletion, any requests to restricted REST endpoints will automatically include the
 * session token in a cookie header, enabling user authentication and authorization.</p>
 *
 * <pre>{@code <METHOD> <resource>
 * Cookie: ID=<token> // optional
 *
 * …}</pre>
 *
 * <p><strong>Successful token validation</strong> / The response includes the response generated by the secured
 * handler; a new session cookie may be included if the session was automatically extended.</p>
 *
 * <pre>{@code <###> <Status>
 * Set-Cooke: ID=<token>; Path=<base>; HttpOnly; Secure; SameSite=Lax // optional
 *
 * … }</pre>
 *
 * <p><strong>Failed token validation</strong> / Due either to session deletion or expiration or to a modified user
 * {@linkplain Permit#id() opaque handle}; no details about the failure are disclosed.</p>
 *
 * <pre>{@code 401 Unauthorized
 * WWW-Authenticate: …
 * Content-Type: application/json
 *
 * {} }</pre>
 *
 * <hr>
 *
 * @see <a href="https://tools.ietf.org/html/rfc7234#section-5.2.3">RFC 7234 Hypertext Transfer Protocol (HTTP/1.1):
 * 		Caching - 5.2.3.  Cache Control Extensions</a>
 */
public final class Manager implements Wrapper {

	public static final String SessionCookie="ID";

	public static final String TicketMalformed="ticket-malformed";


	private static final int TokenIdLength=32; // [bytes]

	private static final Pattern SessionCookiePattern=compile(format("\\b%s\\s*=\\s*(?<value>[^\\s;]+)",
			SessionCookie));
	private static final Collection<String> TicketFields=new HashSet<>(asList("handle", "secret", "update"));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String path;

	private final long soft;
	private final long hard;


	private final Roster roster=service(roster());
	private final Notary notary=service(notary());
	private final Crypto crypto=service(crypto());

	private final Clock clock=service(clock());
	private final Logger logger=service(logger());

	private final JSONFormat json=json();


	/**
	 * Creates a session manager.
	 *
	 * @param path the root relative path of the virtual session handler
	 * @param soft the soft session timeout in milliseconds; on soft timeout session are automatically deleted if no
	 *             activity was registered in the given period
	 * @param hard the hard session timeout in milliseconds; on hard timeout session are unconditionally deleted
	 *
	 * @throws NullPointerException     if {@code path} is null
	 * @throws IllegalArgumentException if {@code path} is not root relative or either {@code idle} or {@code hard} is
	 *                                  less than or equal to 0 or {@code idle} is greater than {@code hard}
	 */
	public Manager(final String path, final long soft, final long hard) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( !path.startsWith("/") ) {
			throw new IllegalArgumentException("not a root relative path <"+path+">");
		}

		if ( soft <= 0 || hard <= 0 || soft > hard ) {
			throw new IllegalArgumentException("illegal timeouts <"+soft+"/"+hard+">");
		}

		this.path=path;

		this.soft=soft;
		this.hard=hard;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return handler
				.with(challenger())
				.with(housekeeper())
				.with(gatekeeper());
	}


	//// Wrappers //////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper challenger() { // add authentication challenge, unless already provided by wrapped handlers
		return handler -> request -> handler.handle(request).map(response -> response.status() == Response.Unauthorized
				? response.header("~WWW-Authenticate", format("Session realm=\"%s\"", response.request().base()))
				: response
		);
	}

	private Wrapper housekeeper() {
		return handler -> handler(request -> request.path().equals(path),

				new Worker()

						.get(relate())
						.post(create()),

				handler

		);
	}

	private Wrapper gatekeeper() {
		return handler -> request -> cookie(request) // look for session token

				.map(token -> notary.verify(token) // token found >> verify

						.map(claims -> lookup(claims.getSubject()).fold( // valid token >> look for permit

								permit -> handler.handle(request // permit found > authenticate and process request

										.user(permit.user())
										.roles(permit.roles())

								).map(response -> {

									final long now=clock.time();

									final long issued=claims.getIssuedAt().getTime();
									final long expiry=claims.getExpiration().getTime();
									final long extend=min(now+soft, issued+hard);

									if ( expiry-now < soft*90/100 ) { // extend if residual lease is < 90% soft timeout
										response.header("Set-Cookie", cookie(request, permit.id(), issued, extend));
									}

									return response // disable caching of protected content

											.header("~Cache-Control", "no-store")
											.header("~Pragma", "no-cache");

								}),

								error -> { // permit not found >> reject request

									logger.warning(this, error);

									return request.reply(new Failure().status(Response.Unauthorized));

								}
						))

						.orElseGet(() -> { // invalid token >> reject request

							logger.warning(this, "invalid session token");

							return request.reply(new Failure().status(Response.Unauthorized));

						})

				)

				.orElseGet(() -> handler.handle(request)); // token not found >> process request
	}


	//// Endpoint Methods //////////////////////////////////////////////////////////////////////////////////////////////

	private Handler relate() {
		return request -> request.reply(cookie(request) // look for session token

				.flatMap(notary::verify) // token found >> verify

				.flatMap(claims -> lookup(claims.getSubject()).value().map(permit -> // valid token >> look for permit

						identified(permit, claims.getIssuedAt().getTime(), claims.getExpiration().getTime())

				))

				.orElseGet(this::anonymous) // report anonymous session

		);
	}

	private Handler create() { // !!! refactor
		return request -> request.reply(request.body(json).fold(

				ticket -> {

					if ( ticket.isEmpty() ) {

						cookie(request)
								.flatMap(notary::verify)
								.map(Claims::getSubject)
								.ifPresent(this::logout);

						return anonymous();

					} else {

						return login(ticket).fold(
								permit -> identified(permit, clock.time(), clock.time()+soft),
								failure -> failure
						);

					}

				},

				failure -> failure

		));
	}


	private Function<Response, Response> identified(final Permit permit, final long issued, final long expiry) {
		return response -> response.status(Response.OK)

				.header("Set-Cookie", cookie(response.request(), permit.id(), issued, expiry))
				.header("Cache-Control", "no-store, timeout="+expiry)
				.header("Pragma", "no-cache")

				.body(json, Json.createObjectBuilder(permit.profile()).build());
	}


	private Function<Response, Response> anonymous() {
		return response -> response.status(Response.OK)

				.header("Set-Cookie", cookie(response.request(), "", 0, 0))
				.header("Cache-Control", "no-store, timeout=0")
				.header("Pragma", "no-cache")

				.body(json, JsonValue.EMPTY_JSON_OBJECT);
	}


	//// Roster Delegates //////////////////////////////////////////////////////////////////////////////////////////////

	private Result<Permit, String> lookup(final String handle) {
		return roster.lookup(handle);
	}

	private Result<Permit, Failure> login(final JsonObject ticket) {
		return Optional.of(ticket)

				.filter(t -> t.values().stream().allMatch(value -> value instanceof JsonString))
				.filter(t -> TicketFields.containsAll(t.keySet()))

				.flatMap(t -> {

					final String handle=t.getString("handle", null);
					final String secret=t.getString("secret", null);
					final String update=t.getString("update", null);

					return handle == null || secret == null ? Optional.empty()
							: update == null ? Optional.of(roster.login(handle, secret))
							: Optional.of(roster.login(handle, secret, update));

				})

				.map(result -> result.<Result<Permit, Failure>>fold(

						permit -> { // log opaque handle to support entry correlation without exposing sensitive info

							logger.info(this, "login "+session(permit));

							return Value(permit);

						},

						error -> { // log only error without exposing sensitive info

							logger.warning(this, "login error "+error);

							return Error(new Failure()
									.status(Response.Forbidden).error(error));

						}

				))

				.orElseGet(() -> { // log only error without exposing sensitive info

					logger.warning(this, "login error "+TicketMalformed);

					return Error(new Failure()
							.status(Response.BadRequest).error(TicketMalformed));

				});
	}

	private void logout(final String handle) {
		roster.logout(handle).fold(

				permit -> { // log opaque handle to support entry correlation without exposing sensitive info

					logger.info(this, "logout "+session(permit));

					return this;

				},

				error -> { // log only error without exposing sensitive info

					logger.warning(this, "logout error "+error);

					return this;

				}

		);
	}


	private String session(final Permit permit) {
		return crypto.token(permit.id(), permit.user());
	}


	//// Cookie Codecs /////////////////////////////////////////////////////////////////////////////////////////////////

	private Optional<String> cookie(final Request request) {
		return request.headers("Cookie")
				.stream()
				.map(value -> {

					final Matcher matcher=SessionCookiePattern.matcher(value);

					return matcher.find() ? matcher.group("value") : null;

				})
				.filter(Objects::nonNull)
				.findFirst();
	}

	private String cookie(final Request request, final String handle, final long issued, final long expiry) {
		return format("%s=%s; Path=%s; SameSite=Lax; HttpOnly%s%s",
				SessionCookie,
				handle.isEmpty() ? "" : notary.create(claims -> claims
						.setId(crypto.token(TokenIdLength))
						.setSubject(handle)
						.setIssuedAt(new Date(issued))
						.setExpiration(new Date(expiry))
				),
				URI.create(request.base()).getPath(),
				request.base().startsWith("https:") ? "; Secure" : "",
				handle.isEmpty() ? "; Max-Age=0" : ""
		);
	}

}
