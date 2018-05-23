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

package com.metreeca.mill;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


/**
 * Simple textual template.
 */
public final class _Template {

	// !!! parametrize text only if actually containing placeholders
	// !!! improve efficiency by splitting the template in advance
	// !!! support escaping braces around placeholders
	// !!! multi-valued substitutions
	// !!! concat vs cartesian product semantics for multi-valued placeholders

	private static final Pattern IRIPattern=Pattern.compile("^\\w+:");
	private static final Pattern PlaceholderPattern=Pattern.compile("(?<encode>[?&=:])?\\{(?<name>\\w+)}");


	private final String template;
	private final boolean iri;


	public _Template(final String template) {

		if ( template == null ) {
			throw new NullPointerException("null template");
		}

		this.template=template;
		this.iri=IRIPattern.matcher(template).lookingAt();
	}


	public String fill(final Function<String, String> resolver) {

		if ( resolver == null ) {
			throw new NullPointerException("null resolver");
		}

		final StringBuilder builder=new StringBuilder(template.length());
		final Matcher matcher=PlaceholderPattern.matcher(template);

		int index=0;

		while ( matcher.find() ) {

			final String encode=matcher.group("encode");
			final String name=matcher.group("name");

			final String value=resolver.apply(name);

			try {

				builder.append(template.substring(index, matcher.start()))
						.append(encode == null ? "" : encode)
						.append(value == null ? ""
								: !iri || encode == null ? value
								: URLEncoder.encode(value, "UTF-8"));

			} catch ( final UnsupportedEncodingException unexpected ) {
				throw new UncheckedIOException(unexpected);
			}

			index=matcher.end();
		}

		builder.append(template.substring(index, template.length()));

		return builder.toString();
	}

}
