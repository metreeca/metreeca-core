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
import com.metreeca.tray.sys.Loader;

import java.net.URI;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.form.things.Codecs.reader;
import static com.metreeca.rest.bodies.TextBody.text;
import static com.metreeca.tray.Tray.tool;
import static com.metreeca.tray.sys.Loader.loader;

import static java.lang.String.format;
import static java.util.regex.Matcher.quoteReplacement;


/**
 * App launcher.
 *
 * <p>Replaces non-{@linkplain Message#interactive() interactive} responses to {@linkplain Message#interactive()
 * interactive} requests with a HTML page {@linkplain Loader loaded} from the path {@linkplain #Launcher(Function)
 * selected} on the basis of the response.</p>
 *
 * @see Message#interactive()
 */
public final class Launcher implements Wrapper {

	private static final Pattern CommentPattern=Pattern.compile("<!--(?s:.)*?-->");
	private static final Pattern SpacePattern=Pattern.compile("[\n\\s]+");
	private static final Pattern LinkPattern=Pattern.compile("\\b(src|href)\\s*=\\s*(?:'([^']*)'|\"([^\"]*)\")");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Function<Response, Optional<String>> selector;

	private final Map<String, String> cache=new HashMap<>(); // path to page

	private final Loader loader=tool(loader());


	/**
	 * Creates an app launcher.
	 *
	 * @param path the absolute path of the page to be launched; {@code index.html} will be appended to paths ending
	 *             with a trailing slash
	 *
	 * @throws NullPointerException if {@code path} is null
	 */
	public Launcher(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		this.selector=request -> Optional.of(path);
	}

	/**
	 * Creates an app launcher.
	 *
	 * @param selector the page selector; must return the optional path of the page to be launched for response; {@code
	 *                 index.html} will be appended to paths ending with a trailing slash
	 *
	 * @throws NullPointerException if {@code selector} is null
	 */
	public Launcher(final Function<Response, Optional<String>> selector) {

		if ( selector == null ) {
			throw new NullPointerException("null selector");
		}

		this.selector=selector;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> handler.handle(request).map(response -> Optional.of(response)

				.filter(r -> request.interactive() && !response.interactive())
				.flatMap(selector)

				.filter(path -> !request.path().equals(path))
				.map(path -> path.endsWith("/") ? path+"index.html" : path)
				.map(path -> cache.computeIfAbsent(path, this::load))

				.map(page -> new Response(request)
						.status(Response.OK)
						.header("Content-Type", "text/html")
						.body(text(), page)
				)

				.orElse(response)
		);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String load(final String path) {
		return loader.load(path)

				.map(stream -> Codecs.text(reader(stream))) // load page

				.map(html -> CommentPattern.matcher(html).replaceAll("")) // remove comments
				.map(html -> SpacePattern.matcher(html).replaceAll(" ")) // collapses spaces

				.map(html -> { // transform relative links into absolute ones

					final URI base=URI.create(path);

					final Matcher matcher=LinkPattern.matcher(html);
					final StringBuffer buffer=new StringBuffer(html.length());

					while ( matcher.find() ) {

						final String attribute=matcher.group(1);
						final String squote=matcher.group(2);
						final String dquote=matcher.group(3);

						matcher.appendReplacement(buffer, quoteReplacement(format("%s=\"%s\"",
								attribute, base.resolve(squote != null ? squote : dquote != null ? dquote : "")
						)));

					}

					return matcher.appendTail(buffer).toString();

				})

				.orElseThrow(() -> new IllegalArgumentException("missing resource ["+path+"]"));
	}

}
