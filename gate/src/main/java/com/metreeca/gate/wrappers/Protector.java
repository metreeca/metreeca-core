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

import com.metreeca.gate.Crypto;
import com.metreeca.gate.Notary;
import com.metreeca.rest.*;
import com.metreeca.tray.sys.Clock;
import com.metreeca.tray.sys.Trace;

import java.util.Date;
import java.util.Objects;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.gate.Crypto.crypto;
import static com.metreeca.gate.Notary.notary;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Clock.clock;
import static com.metreeca.tray.sys.Trace.trace;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;


/**
 * XSS/XSRF protector.
 *
 * <p>Enforces transport security and content security policies and manages
 * <a href="https://angular.io/guide/http#security-xsrf-protection">Angular-compatible</a>
 * token-based XSRF protection.</p>
 */
public final class Protector implements Wrapper {

	public static final String XSRFCookie="XSRF-TOKEN";
	public static final String XSRFHeader="X-XSRF-TOKEN";

	private static final long SecureDefault=86_400_000; // [ms]

	private static final String PolicyDefault="default-src 'self'; base-uri 'self'";
	private static final String XSSDefault="1; mode=block";

	private static final long CookieDefault=86_400_000; // [ms]
	private static final int TokenIdLength=32; // [bytes]
	private static final String SameSiteDefault="Lax";

	private static final Pattern XSRFCookiePattern=compile(format("\\b%s\\s*=\\s*(?<value>[^\\s;]+)", XSRFCookie));
	private static final Pattern SelfPolicyPattern=compile("\\bdefault-src\\s*'self'");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private long secure;
	private long cookie;

	private String policy="";

	private final Notary notary=tool(notary());
	private final Crypto crypto=tool(crypto());

	private final Clock clock=tool(clock());
	private final Trace trace=tool(trace());


	/**
	 * Configures strict transport security.
	 *
	 * @param secure enables strict {@linkplain #secure(long) secure} transport with a default period equal to {@value
	 *               #SecureDefault} milliseconds, if true; disables it, otherwise
	 *
	 * @return this protector
	 */
	public Protector secure(final boolean secure) {
		return secure(secure ? SecureDefault : 0L);
	}

	/**
	 * Configures strict transport security.
	 *
	 * <p>If an enforced strict secure transport period is defined:</p>
	 *
	 * <ul>
	 *
	 * <li>{@code HTTP} requests are temporarily redirected to {@code HTTPS};</li>
	 *
	 * <li>the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security">Strict-Transport-Security</a>
	 * header is set on responses with the provided {@code max-age} value, unless already defined by nested
	 * handlers;</li>
	 *
	 * <li>XSRF cookies are created with the {@code Secure} attribute.</li>
	 *
	 * </ul>
	 *
	 * @param secure enforced secure transport period in seconds; 0 for no secure transport enforcing
	 *
	 * @return this protector
	 *
	 * @throws IllegalArgumentException if {@code secure} is less than 0
	 */
	public Protector secure(final long secure) {

		if ( secure < 0 ) {
			throw new IllegalArgumentException("illegal secure transport period ["+secure+"]");
		}

		this.secure=secure;

		return this;
	}


	/**
	 * Configures content security policy.
	 *
	 * @param policy enables content security {@linkplain #policy(String) policy} with a default value equal to {@value
	 *               #PolicyDefault}, if true; disables it, otherwise
	 *
	 * @return this protector
	 */
	public Protector policy(final boolean policy) {
		return policy(policy ? PolicyDefault : "");
	}

	/**
	 * Configures content security policy.
	 *
	 * <p>If a content security policy is defined:</p>
	 *
	 * <ul>
	 *
	 * <li>the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP">Content-Security-Policy</a> header is
	 * set on responses with the provided value, unless already defined by nested handlers;</li>
	 *
	 * <li>the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection">X-XSS-Protection</a>
	 * header is set as a fallback measure with the {@value XSSDefault} value, unless already defined by nested
	 * handlers;</li>
	 *
	 * <li>if the policy includes the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Content-Security-Policy/default-src">default-src
	 * 'self'</a> directive, the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Referrer-Policy">Referrer-Policy</a>
	 * header is set on responses with the {@code same-origin} value, unless already defined by nested handlers.</li>
	 *
	 * </ul>
	 *
	 * @param policy the content security policy to be enforced by the browser; empty for no policy enforcing
	 *
	 * @return this protector
	 *
	 * @throws NullPointerException if {@code policy} is null
	 */
	public Protector policy(final String policy) {

		if ( policy == null ) {
			throw new NullPointerException("null policy");
		}

		this.policy=policy;

		return this;
	}


	/**
	 * Configures cookie-based XSRF protection.
	 *
	 * @param cookie enables {@linkplain #cookie(long) cookie}-based XSRF protection with a default cookie lease equal
	 *               to {@value #CookieDefault} milliseconds, if true; disables it, otherwise
	 *
	 * @return this protector
	 */
	public Protector cookie(final boolean cookie) {
		return cookie(cookie ? CookieDefault : 0L);
	}

	/**
	 * Configures cookie-based XSRF protection.
	 *
	 * <p>If cookie-based XSRF protection is enabled:</p>
	 *
	 * <ul>
	 *
	 * <li>a unique XSRF token is returned in the {@value XSRFCookie} session cookie on {@linkplain Request#safe()
	 * safe} requests;</li>
	 *
	 * <li>unsafe requests are processed only if a valid XSRF token is included both in the {@value XSRFCookie}
	 * cookie and in the {@value XSRFHeader} header and refused with a {@link Response#Forbidden Forbidden} status
	 * otherwise.</li>
	 *
	 * </ul>
	 *
	 * <p><strong>Warning</strong> / XSRF session cookie lease time must exceed the maximum allowed session duration:
	 * no automatic XSRF cookie extension/rotation is performed until the session expires.</p>
	 *
	 * <p><strong>Note</strong> / XSRF cookie/header names are compatible by default with <a
	 * href="https://angular.io/guide/http#security-xsrf-protection">Angular XSRF protection</a> scheme.</p>
	 *
	 * @param cookie XSRF cookie lease time in milliseconds; 0 for no cookie-based XSRF protection
	 *
	 * @return this protector
	 *
	 * @throws IllegalArgumentException if {@code cookie} is less than 0
	 */
	public Protector cookie(final long cookie) {

		if ( cookie < 0 ) {
			throw new IllegalArgumentException("illegal XSRF token lease time ["+cookie+"]");
		}

		this.cookie=cookie;

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return handler
				.with(transport())
				.with(xss())
				.with(xsrf());
	}


	//// Transport /////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper transport() {
		return handler -> request -> {

			if ( secure == 0 ) {

				return handler.handle(request);

			} else if ( request.base().startsWith("http:") ) {

				return request.reply(response -> response
						.status(Response.TemporaryRedirect)
						.header("Location", request.item().stringValue().replace("http:", "https:"))
				);

			} else {

				return handler.handle(request).map(response -> response
						.header("~Strict-Transport-Security", format("max-age=%d", secure/1000))
				);

			}

		};
	}


	//// XSS ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper xss() {
		return handler -> request -> policy.isEmpty() ? handler.handle(request) : handler.handle(request).map(response -> response
				.header("~Content-Security-Policy", policy)
				.header("~Referrer-Policy", SelfPolicyPattern.matcher(policy).find() ? "same-origin" : "")
				.header("~X-XSS-Protection", XSSDefault)
		);
	}


	//// XSRF //////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper xsrf() {
		return handler -> request -> {

			final String header=request.header(XSRFHeader)
					.orElse("")
					.trim();

			final String cookie=request.headers("Cookie")
					.stream()
					.map(value -> {

						final Matcher matcher=XSRFCookiePattern.matcher(value);

						return matcher.find() ? matcher.group("value") : null;

					})
					.filter(Objects::nonNull)
					.findFirst()
					.orElse("")
					.trim();

			if ( this.cookie == 0 ) {

				return handler.handle(request);

			} else if ( header.isEmpty() && cookie.isEmpty() && request.safe()
					|| !header.isEmpty() && !cookie.isEmpty() && header.equals(cookie) && token(header)
			) {

				return handler.handle(request).map(response -> {

					if ( request.safe() && cookie.isEmpty() ) {

						response.header("Set-Cookie", format("%s=%s; Path=%s; SameSite=%s%s",
								XSRFCookie,
								token(clock.time()),
								request.base(),
								SameSiteDefault,
								secure > 0 ? "; Secure" : ""
						));

					}

					return response;

				});

			} else {

				trace.warning(this, header.isEmpty() ? "missing XSRF header"
						: cookie.isEmpty() ? "missing XSRF cookie"
						: header.equals(cookie) ? "invalid XSRF token"
						: "mismatched XSRF header/cookie"
				);

				return request.reply(new Failure().status(Response.Forbidden)); // no details disclosed

			}

		};
	}


	private String token(final long now) {
		return notary.create(claims -> claims
				.setId(crypto.token(TokenIdLength))
				.setIssuedAt(new Date(now))
				.setExpiration(new Date(now+cookie))
		);
	}

	private boolean token(final String token) {
		return notary.verify(token).isPresent(); // signature/expiration validated by Jwts.parser()
	}

}
