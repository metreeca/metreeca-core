/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.rest.engines;

import java.util.function.Supplier;
import java.util.stream.Stream;


/**
 * SPARQL generation tool.
 *
 * <p>Converts structured objects into properly indented and spaced SPARQL source code.</p>
 */
final class SPARQLBuilder {

	private int indent;

	private char last;
	private char next;

	private final StringBuilder text=new StringBuilder(1000);


	public String text() {
		try {
			return text.toString();
		} finally {
			indent=0;
			last=0;
			next=0;
			text.setLength(0);
		}
	}


	public SPARQLBuilder text(final Object object) {
		return object == null ? this
				: object instanceof Object[] ? text((Object[])object)
				: object instanceof Iterable ? text((Iterable<?>)object)
				: object instanceof Stream ? text((Stream<?>)object)
				: object instanceof Supplier ? text((Supplier<?>)object)
				: text(String.valueOf(object));
	}

	public SPARQLBuilder text(final Object... array) {

		if ( array != null ) {
			for (final Object object : array) { text(object); }
		}

		return this;
	}

	public SPARQLBuilder text(final Iterable<?> iterable) {

		if ( iterable != null ) {
			for (final Object object : iterable) { text(object); }
		}

		return this;
	}

	public SPARQLBuilder text(final Stream<?> stream) {

		if ( stream != null ) {
			stream.forEach(this::text);
		}

		return this;
	}

	public SPARQLBuilder text(final Supplier<?> supplier) {

		if ( supplier != null ) {
			text(supplier.get());
		}

		return this;
	}

	public SPARQLBuilder text(final CharSequence chars) {

		if ( chars != null ) {
			for (int i=0, length=chars.length(); i < length; ++i) { text(chars.charAt(i)); }
		}

		return this;
	}

	public SPARQLBuilder text(final char c) {

		if ( c == '\t' ) {

			++indent;

		} else if ( c == '\b' ) {

			--indent;

		} else if ( c == '\f' ) {

			text('\n');
			text('\n');

		} else if ( c == '\n' ) {

			if ( last == '{' ) { ++indent; }

			if ( last != '\n' || next != '\n' ) {
				append('\n');
			}

		} else if ( c == ' ' ) {

			if ( next == ' ' && last == '('
					|| last != '\0' && last != ' ' && last != '\n' && last != '(' && last != '[' ) {
				append(' ');
			}

		} else {

			if ( last == '\n' ) {

				if ( c == '}' ) { --indent; }

				for (int i=4*indent; i > 0; --i) { append(' '); }
			}

			append(c);
		}

		return this;
	}


	private void append(final char c) {
		try {
			text.append(c);
		} finally {
			next=last;
			last=c;
		}
	}

}
