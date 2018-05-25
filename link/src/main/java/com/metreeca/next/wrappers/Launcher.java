/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.next.wrappers;

import com.metreeca.link.*;
import com.metreeca.spec.things.Transputs;
import com.metreeca.tray.sys.Loader;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import static com.metreeca.tray.Tray.tool;

import static java.lang.String.format;


/**
 * Single page app launcher.
 *
 * <p>Replaces {@linkplain Response.Reader#interactive() non-interactive responses} to {@linkplain
 * Request#interactive() interactive requests} with a relocated version of a (x)html page where absolute {@code href} or
 * {@code src} links are relocated to the page home folder.</p>
 */
public final class Launcher implements Wrapper {

	private static final Pattern LinkPattern=Pattern.compile("\\b(href|src)\\s*=\\s*([\"'])/(.*)\\2");
	private static final String LinkReplacement="$1=$2%s%s$3$2";


	/**
	 * Creates a new single page app launcher for the default page path.
	 *
	 * <p>Equivalent to {@link #launcher(String) launcher("/index.html")}.</p>
	 *
	 * @return the new single page app launcher
	 */
	public static Launcher launcher() {
		return launcher("/index.html");
	}

	/**
	 * Creates a new single page app launcher.
	 *
	 * @param path the absolute path of the app page (to be loaded through the {@linkplain Loader#Tool system loader}
	 *             tool)
	 *
	 * @return the new single page app launcher
	 *
	 * @throws IllegalArgumentException if {@code path} is not absolute
	 */
	public static Launcher launcher(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( !path.startsWith("/") ) {
			throw new IllegalArgumentException("relative path ["+path+"]");
		}

		return new Launcher(path);
	}


	private final String path; // the path of the app folder (no leading slash, trailing slash)
	private final String type; // the MIME type of the app (derived from page filename extension)
	private final String text; // the raw text of the app page


	private Launcher(final String path) {

		this.path=path // strip leading slash
				.substring(1, path.lastIndexOf('/')+1);

		this.type=path.endsWith(".html") ? "text/html"
				: path.endsWith(".xhtml") ? "application/xhtml+xml"
				: "";

		this.text=tool(Loader.Tool)
				.load(path)
				.map(Transputs::reader)
				.map(Transputs::text)
				.orElseThrow(() -> new NoSuchElementException("missing app page ["+path+"]"));

	}


	@Override public Handler wrap(final Handler handler) {
		return (request, response) -> handler.exec(

				writer ->

						writer.copy(request).done(),

				reader -> {

					if ( request.interactive() && !reader.interactive() ) {

						response.status(Response.OK)
								.header("Content-Type", type)
								.text(LinkPattern.matcher(text).replaceAll(
										format(LinkReplacement, request.base(), path)
								));

					} else {

						response.copy(reader).done();

					}
				}

		);
	}

}
