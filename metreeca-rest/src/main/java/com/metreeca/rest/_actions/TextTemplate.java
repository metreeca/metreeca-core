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

package com.metreeca.rest._actions;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.nio.charset.StandardCharsets.UTF_8;


/**
 * Textual template.
 */
final class TextTemplate {

	private static final Pattern PlaceholderPattern=Pattern.compile("(?<modifier>[\\\\%])?\\{(?<name>\\w+)}");


	private final String template;


	public TextTemplate(final String template) {

		if ( template == null ) {
			throw new NullPointerException("null template");
		}

		this.template=template;
	}


	public String fill(final Map<String, String> values) {

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return fill(values::get);
	}

	public String fill(final Function<String, String> resolver) {

		if ( resolver == null ) {
			throw new NullPointerException("null resolver");
		}

		final StringBuilder builder=new StringBuilder(template.length());
		final Matcher matcher=PlaceholderPattern.matcher(template);

		int index=0;

		while ( matcher.find() ) {

			final String modifier=matcher.group("modifier");
			final String name=matcher.group("name");

			final String value=resolver.apply(name);

			try {

				builder.append(template, index, matcher.start()).append(
						"\\".equals(modifier) ? matcher.group().substring(1)
								: "%".equals(modifier) ? URLEncoder.encode(value, UTF_8.name())
								: value != null ? value
								: ""
				);

			} catch ( final UnsupportedEncodingException unexpected ) {
				throw new UncheckedIOException(unexpected);
			}

			index=matcher.end();
		}

		builder.append(template.substring(index));

		return builder.toString();
	}

}
