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

package com.metreeca.rest.actions;

import com.metreeca.rest.Request;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public final class Query implements Function<String, Optional<Request>> {

	private static final Pattern ItemPattern=Pattern.compile("(?<base>https?+://[^/]*)/?(?<path>.*)");


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Function<Request, Request> customizer;


	public Query() {
		this(request -> request);
	}

	public Query(final Function<Request, Request> customizer) {

		if ( customizer == null ) {
			throw new NullPointerException("null customizer");
		}

		this.customizer=customizer;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Optional<Request> apply(final String url) {
		return Optional.ofNullable(url)

				.map(ItemPattern::matcher)
				.filter(Matcher::matches)

				.map(matcher -> new Request()

						.method(Request.GET)

						.base(matcher.group("base")+'/')
						.path('/'+matcher.group("path"))

				)

				.map(customizer);
	}

}
