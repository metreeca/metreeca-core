/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 *  Metreeca is free software: you can redistribute it and/or modify it under the terms
 *  of the GNU Affero General Public License as published by the Free Software Foundation,
 *  either version 3 of the License, or(at your option) any later version.
 *
 *  Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 *  without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 *  See the GNU Affero General Public License for more details.
 *
 *  You should have received a copy of the GNU Affero General Public License along with Metreeca.
 *  If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.rest.wrappers;

import com.metreeca.form.things.Transputs;
import com.metreeca.rest.Handler;
import com.metreeca.rest.Message;
import com.metreeca.rest.Wrapper;
import com.metreeca.tray.sys.Loader;

import java.util.NoSuchElementException;
import java.util.regex.Pattern;

import static com.metreeca.tray.Tray.tool;


/**
 * Single page app launcher (work in progress…).
 *
 *  and advertises the base LDP server URL to
 *  * interactive apps through the {@value #BaseCookie} cookie.
 *
 * <p>Replaces non-interactive responses to {@linkplain Message#interactive() interactive requests} with a relocated
 * version of a (x)html page where absolute {@code href} or {@code src} links are relocated to the page home
 * folder.</p>
 *
 * @deprecated Work in progress
 */
@Deprecated final class Launcher implements Wrapper {

	private static final String BaseCookie="com.metreeca.rest";

	private static final Pattern LinkPattern=Pattern.compile("\\b(href|src)\\s*=\\s*([\"'])/(.*)\\2");
	private static final String LinkReplacement="$1=$2%s%s$3$2";


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private String path=""; // the path of the app folder (no leading slash, trailing slash)
	private String type=""; // the MIME type of the app (derived from page filename extension)
	private String text=""; // the raw text of the app page

	private final Loader loader=tool(Loader.Factory);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Configures the path of the app page.
	 *
	 * @param path the absolute path of the app page (to be loaded through the {@linkplain Loader#Factory system loader}
	 *             tool)
	 *
	 * @return this launcher
	 *
	 * @throws NullPointerException     if {@code path} is null
	 * @throws IllegalArgumentException if {@code path} is not absolute
	 */
	public Launcher path(final String path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		if ( !path.startsWith("/") ) {
			throw new IllegalArgumentException("relative path ["+path+"]");
		}

		this.path=path // strip leading slash
				.substring(1, path.lastIndexOf('/')+1);

		this.type=path.endsWith(".html") ? "text/html"
				: path.endsWith(".xhtml") ? "application/xhtml+xml"
				: "";

		this.text=loader
				.load(path)
				.map(Transputs::reader)
				.map(Transputs::text)
				.orElseThrow(() -> new NoSuchElementException("missing app page ["+path+"]"));


		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {

		throw new UnsupportedOperationException("to be implemented"); // !!! tbi
		//return request -> handler.handle(request).map(response ->
		//		path.isEmpty() || !request.interactive() || response.interactive() ? response : request.reply(r ->
		//				r.status(Response.OK)
		//						.header("Content-Type", type)
		//						.body(Text.Format, LinkPattern.matcher(text).replaceAll(
		//								String.format(LinkReplacement, request.base(), path)
		//						)))
		//);


		// advertise LDP server base to interactive apps

		//if ( request.interactive() ) {
		//	response.header("Set-Cookie", BaseCookie+"="+request.base()+";path=/");
		//}

	}

}
