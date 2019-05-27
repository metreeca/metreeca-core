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

import java.time.Duration;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.gate.Gate.random;
import static com.metreeca.gate.Notary.notary;
import static com.metreeca.tray.Tray.tool;

import static java.lang.String.format;
import static java.util.regex.Pattern.compile;


/**
 * XSS protector.
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

	private static final long SecureDefault=86400;
	private static final String PolicyDefault="default-src 'self'; base-uri 'self'";
	private static final String XSSDefault="1; mode=block";

	public static final String XSRFCookie="XSRF-TOKEN";
	public static final String XSRFHeader="X-XSRF-TOKEN";

	private static final Pattern XSRFCookiePattern=compile(format("\\b(?<name>%s)\\s*=\\s*(?<value>[^\\s;]+)", XSRFCookie));


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private long lease=0; // XSRF token duration (s); 0 for no expiry
	private long secure=0; // enforced secure transport period (s); 0 for no enforcing

	private String policy=""; // browser security policy; empty for no policy

	private final Notary notary=tool(notary());
	private final Random random=tool(random());


	public Protector lease(final long lease) {

		if ( lease < 0 ) {
			throw new IllegalArgumentException("illegal XSRF token duratio ["+lease+"]");
		}

		this.lease=lease;

		return this;
	}


	public Protector secure(final boolean secure) {
		return secure(secure ? SecureDefault : 0L);
	}

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

	public Protector policy(final String policy) {

		if ( policy == null ) {
			throw new NullPointerException("null policy");
		}

		this.policy=policy;

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
				.header("~X-XSS-Protection", XSSDefault)
		);
	}


	//// XSRF //////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Wrapper xsrf() { // !!! refactor
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

					final Request request1=response.request();

					if ( request1.safe() && request1.headers("Cookie").stream().noneMatch(value -> XSRFCookiePattern.matcher(value).find()) ) {

						response.header("Set-Cookie", format("%s=%s; Path=%s; SameSite=Lax%s",
								XSRFCookie, token(), request.base(), secure > 0 ? "; Secure" : ""
						));
					}

					return response;

				});

			} else {

				return request.reply(new Failure().status(Response.Forbidden)); // no details disclosed

			}

		};
	}


	private String token() {
		return notary.create(claims -> {

			final byte[] bytes=new byte[256/8];

			random.nextBytes(bytes);

			claims
					.setId(Base64.getEncoder().encodeToString(bytes))
					.setExpiration(new Date(System.currentTimeMillis()+(lease > 0 ? lease : Duration.ofDays(1).toMillis())));
		});
	}

	private boolean token(final String token) {
		return notary.verify(token).isPresent(); // signature/expiration validated by Jwts.parser()
	}

}
