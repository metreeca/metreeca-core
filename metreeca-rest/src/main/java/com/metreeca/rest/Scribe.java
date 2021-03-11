/*
 * Copyright © 2013-2021 Metreeca srl
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.metreeca.rest;

import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Collection;
import java.util.stream.Stream;

import static com.metreeca.json.Values.format;

import static java.lang.String.valueOf;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * Source code composer.
 */
public abstract class Scribe {

	public static String code(final Scribe scribe) {

		if ( scribe == null ) {
			throw new NullPointerException("null scribe");
		}

		return scribe.toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Scribe when(final boolean condition, final Scribe... scribes) {
		return condition ? list(scribes) : nothing();
	}


	public static Scribe block(final Scribe... scribes) {
		return list(text("\r{ "), list(scribes), text(" }"));
	}

	public static Scribe line(final Scribe... scribes) {
		return list(text('\n'), list(scribes), text('\n'));
	}


	public static Scribe space(final Scribe... scribes) {
		return list(text('\f'), list(scribes), text('\f'));
	}

	public static Scribe indent(final Scribe... scribes) {
		return list(text('\t'), list(scribes), text('\b'));
	}


	public static Scribe list(final Scribe... items) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		return list(stream(items));
	}

	public static Scribe list(final Stream<Scribe> items) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		return list(items.collect(toList())); // memoize to enable reuse
	}

	public static Scribe list(final Collection<Scribe> items) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		return new Scribe() {

			@Override protected Appendable scribe(final Appendable code) {
				return items.stream()
						.map(item -> item.scribe(code))
						.reduce(code, (x, y) -> code);
			}

		};
	}


	public static Scribe list(final Scribe[] items, final CharSequence separator) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		if ( separator == null ) {
			throw new NullPointerException("null separator");
		}

		return list(asList(items), separator);
	}

	public static Scribe list(final Stream<Scribe> items, final CharSequence separator) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		if ( separator == null ) {
			throw new NullPointerException("null separator");
		}

		return list(items.collect(toList()), separator); // memoize to enable reuse
	}

	public static Scribe list(final Collection<Scribe> items, final CharSequence separator) {

		if ( items == null ) {
			throw new NullPointerException("null items");
		}

		if ( separator == null ) {
			throw new NullPointerException("null separator");
		}

		return new Scribe() {

			@Override protected Appendable scribe(final Appendable code) {
				return items.stream()
						.flatMap(item -> Stream.of(text(separator), item))
						.skip(1)
						.map(item -> item.scribe(code))
						.reduce(code, (x, y) -> code);
			}

		};
	}


	public static Scribe text(final Object value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return text(valueOf(value));
	}

	public static Scribe text(final Value value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return text(format(value));
	}

	public static Scribe text(final char c) {
		return new Scribe() {
			@Override protected Appendable scribe(final Appendable code) {
				try {

					return code.append(c);

				} catch ( final IOException e ) {

					throw new UncheckedIOException(e);

				}
			}
		};
	}

	public static Scribe text(final CharSequence text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		return new Scribe() {
			@Override protected Appendable scribe(final Appendable code) {
				try {

					return code.append(text);

				} catch ( final IOException e ) {

					throw new UncheckedIOException(e);

				}
			}
		};
	}

	public static Scribe text(final String format, final Object... args) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		return text(String.format(format, args));
	}


	public static Scribe nothing() {
		return new Scribe() {

			@Override protected Appendable scribe(final Appendable code) { return code; }

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Scribe() {}


	abstract Appendable scribe(final Appendable code);


	@Override public String toString() {
		return scribe(new Formatter(new StringBuilder(1000))).toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Formatter implements Appendable {

		private final Appendable code; // output target

		private int indent; // indent level

		private char last; // last output
		private char wait; // pending optional whitespace


		private Formatter(final Appendable code) {
			this.code=code;
		}


		@Override public Appendable append(final CharSequence sequence) {

			for (int i=0, n=sequence.length(); i < n; ++i) { append(sequence.charAt(i)); }

			return this;
		}

		@Override public Appendable append(final CharSequence sequence, final int start, final int end) {

			for (int i=start; i < end; ++i) { append(sequence.charAt(i)); }

			return this;
		}

		@Override public Appendable append(final char c) {
			switch ( c ) {

				case '\f': return feed();
				case '\r': return fold();
				case '\n': return newline();
				case ' ': return space();

				case '\t': return indent();
				case '\b': return outdent();

				default: return other(c);

			}
		}


		@Override public String toString() {
			return code.toString();
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private Formatter feed() {

			if ( last != '\0' ) { wait='\f'; }

			return this;
		}

		private Formatter fold() {

			if ( last != '\0' && wait != '\f' ) { wait=(wait == '\n') ? '\f' : ' '; }

			return this;
		}

		private Formatter newline() {

			if ( last != '\0' && wait != '\f' ) { wait='\n'; }

			return this;
		}

		private Formatter space() {

			if ( last != '\0' && wait != '\f' && wait != '\n' ) { wait=' '; }

			return this;
		}


		private Formatter indent() {

			if ( last != '\0' ) { ++indent; }

			return this;
		}

		private Formatter outdent() {

			if ( indent > 0 ) { --indent; }

			return this;
		}


		private Formatter other(final char c) {
			try {

				if ( wait == '\f' || wait == '\n' ) {

					if ( last == '{' ) { ++indent; }

					if ( c == '}' && indent > 0 ) { --indent; }

					code.append(wait == '\f' ? "\n\n" : "\n");

					for (int i=4*indent; i > 0; --i) { code.append(' '); }

				} else if ( wait == ' ' && last != '(' && c != ')' && last != '[' && c != ']' ) {

					code.append(' ');

				}

				code.append(c);

				return this;

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} finally {

				last=c;
				wait='\0';

			}
		}

	}

}
