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

import com.metreeca.form.things.Codecs;
import com.metreeca.rest.*;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.rest.bodies.TextBody.text;


/**
 * Single page app launcher.
 *
 * <p>Replaces non-{@linkplain Message#interactive() interactive} responses to {@linkplain Message#interactive()
 * interactive} requests with a client-side loader for a provided {@linkplain #Launcher(String) URL}; relative links in
 * the target page are resolved against its URL.</p>
 *
 * @see Message#interactive()
 */
public final class Launcher implements Wrapper {

	private static final Pattern BasePattern=Pattern.compile("\\$\\{base}");
	private static final Pattern CommentPattern=Pattern.compile("(//[^\n]*)|(<!--(?s:.)*?-->)");
	private static final Pattern SpacePattern=Pattern.compile("[\n\\s]+");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final String path;
	private final String body;


	/**
	 * Creates an app launcher.
	 *
	 * @param url the URL of the single page app to be loaded
	 *
	 * @throws NullPointerException if {@code url} is null
	 */
	public Launcher(final String url) {

		if ( url == null ) {
			throw new NullPointerException("null url");
		}

		this.path=url;

		this.body=Optional
				.ofNullable(Codecs.text(Launcher.class, ".html"))
				.map(template -> CommentPattern.matcher(template).replaceAll("")) // remove comments
				.map(template -> SpacePattern.matcher(template).replaceAll(" ")) // collapses spaces
				.map(template -> BasePattern.matcher(template).replaceAll(Matcher.quoteReplacement(url))) // relocate
				.orElse("unexpected");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> consumer -> handler.handle(request).accept(response -> {

			if ( !request.path().equals(path) && request.interactive()
					&& response.status() != 0 && !response.interactive() ) {

				response.request().reply(loader -> loader

						.status(Response.OK)
						.header("Content-Type", "text/html")
						.body(text(), body)

				).accept(consumer);

			} else {

				consumer.accept(response);

			}

		});
	}
}
