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

import com.metreeca.gate.Notary;
import com.metreeca.rest.*;
import com.metreeca.tray.sys.Trace;

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.gate.Gate.random;
import static com.metreeca.gate.Notary.notary;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Trace.trace;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;


/**
 * XSS/XSRF protector.
 *
 * <p>Configures the following standard XSS protection headers (unless already defined by wrapper handlers):</p>
 *
 * <ul>
 *
 * <li><a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP">Content Security Policy (CSP)</a>: {@value
 * PolicyDefault}</li>
 *
 * <li><a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/Strict-Transport-Security">Strict-Transport-Security</a>:
 * {@value StrictTransportSecurity}</li>
 *
 * <li><a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection">X-XSS-Protection</a>:
 * {@value XSSDefault}</li>
 *
 * </ul>
 */
public final class Protector implements Wrapper {

	private static final int SessionIdLength=256; // [bits]

	private static final long SecureDefault=86400;

	private static final String PolicyDefault="default-src 'self'; base-uri 'self'";
	private static final String XSSDefault="1; mode=block";

	public static final String XSRFCookie="XSRF-TOKEN";
	public static final String XSRFHeader="X-XSRF-TOKEN";
	public static final String SameSiteDefault="Lax";

	private static final Pattern XSRFCookiePattern=compile(format("\\b(?<name>%s)\\s*=\\s*(?<value>[^\\s;]+)", XSRFCookie));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private long secure;
	private long period;

	private String policy="";

	private final Notary notary=tool(notary());
	private final Random random=tool(random());

	private final Trace trace=tool(trace());


	/**
	 * Configures strict transport security.
	 *
	 * @param secure enables strict {@linkplain #secure(long) secure} transport with a default period equal to {@value
	 *               #SecureDefault}, if true; disables it, otherwise
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


	public Protector policy(final boolean policy) {
		return policy(policy ? PolicyDefault : "");
	}

	/**
	 * Configures content security policy.
	 *
	 * <p>If a content securiti policy is defined:</p>
	 *
	 *
	 * <li>the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/CSP">Content-Security-Policy</a> header is
	 * set on responses with the provided value, unless already defined by nested handlers;</li>
	 *
	 * <li>the <a href="https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/X-XSS-Protection">X-XSS-Protection</a>
	 * header is set as a fallback measure with the {@value XSSDefault} value, unless already defined by nested
	 * handlers;</li>
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


	public Protector lease(final long lease) {

		if ( lease < 0 ) {
			throw new IllegalArgumentException("illegal XSRF token duratio ["+lease+"]");
		}

		this.period=lease;

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
						.header("~Strict-Transport-Security", format("max-age=%d", secure))
				);

			}

		};
	}


	//// XSS ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper xss() {
		return handler -> request -> handler.handle(request).map(response -> response
				.header("~Content-Security-Policy", policy)
				.header("~X-XSS-Protection", policy.isEmpty()? "" : XSSDefault)
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

			if ( header.isEmpty() && cookie.isEmpty() && request.safe()
					|| !header.isEmpty() && !cookie.isEmpty() && header.equals(cookie) && token(header)
			) {

				return handler.handle(request).map(response -> {

					// add cookie on safe request if not already defined

					if ( request.safe() && request.headers("Cookie").stream()
							.noneMatch(value -> XSRFCookiePattern.matcher(value).find())
					) {

						response.header("Set-Cookie", format("%s=%s; Path=%s; SameSite=Lax%s",
								XSRFCookie, token(), request.base(), secure > 0 ? "; Secure" : ""
						));

					}

					return response;

				});

			} else {

				trace.warning(this, header.isEmpty() ? "missing XSRF header"
						: cookie.isEmpty() ? "missing XSRF cookie"
						: header.equals(cookie) ? format("invalid XSRF token: %s", header)
						: format("mismatched XSRF header/cookie: %s / %s", header, cookie)
				);

				return request.reply(new Failure().status(Response.Forbidden)); // no details disclosed

			}

		};
	}


	private String token() {
		return notary.create(claims -> {

			final byte[] bytes=new byte[SessionIdLength/8];

			random.nextBytes(bytes);

			claims
					.setId(Base64.getEncoder().encodeToString(bytes))
					.setExpiration(new Date(System.currentTimeMillis()+(period > 0 ? period : Duration.ofDays(1).toMillis())));
		});
	}

	private boolean token(final String token) {
		return notary.verify(token).isPresent(); // signature/expiration validated by Jwts.parser()
	}

}
