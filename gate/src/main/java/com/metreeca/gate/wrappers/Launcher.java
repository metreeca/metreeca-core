/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
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
