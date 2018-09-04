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

package com.metreeca.form.things;

import java.util.Locale;


/**
 * String utilities.
 */
public final class Strings {

	public static String lower(final String string) {
		return string == null ? null : string.toLowerCase(Locale.ROOT);
	}

	public static String upper(final String string) {
		return string == null ? null : string.toUpperCase(Locale.ROOT);
	}

	public static String title(final String string) {
		if ( string == null || string.isEmpty() ) { return string; } else {

			final StringBuilder builder=new StringBuilder(string.length());

			boolean trailing=false;

			for (int n=string.length(), i=0; i < n; ++i) {

				final char c=string.charAt(i);

				builder.append(trailing ? c : Character.toUpperCase(c));

				trailing=Character.isAlphabetic(c);
			}

			return builder.toString();
		}
	}


	public static String capitalize(final String string) {
		return string == null || string.isEmpty() ? string
				: Character.toUpperCase(string.charAt(0))+string.substring(1);
	}

	public static String normalize(final String string) {
		if ( string == null || string.isEmpty() ) { return string; } else {

			final StringBuilder builder=new StringBuilder(string.length());

			boolean spacing=false;

			for (int n=string.length(), i=0; i < n; ++i) {

				final char c=string.charAt(i);

				if ( !Character.isWhitespace(c) ) {

					if ( spacing ) {
						builder.append(' ');
					}

					builder.append(c);

					spacing=false;

				} else if ( builder.length() > 0 ) {

					spacing=true;

				}

			}

			return builder.toString();
		}
	}


	public static String indent(final Object object) {
		return indent(object, false);
	}

	public static String indent(final Object object, final boolean trailing) {
		return object == null ? null : object.toString().replaceAll((trailing ? "(\n)" : "(^|\n)")+"([^\n])", "$1\t$2");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Strings() {}

}
