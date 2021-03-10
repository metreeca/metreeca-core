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
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * Source code generator.
 */
public abstract class Scribe {

	public static String code(final Scribe scribe) {

		if ( scribe == null ) {
			throw new NullPointerException("null scribe");
		}

		return scribe.toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Scribe nothing() {
		return new Scribe() {

			@Override protected Appendable scribe(final Appendable code) { return code; }

		};
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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Scribe() {}


	abstract Appendable scribe(final Appendable code);


	@Override public String toString() {
		return scribe(new Formatter(new StringBuilder(1000))).toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Formatter implements Appendable {

		private final Appendable code;

		private int indent;

		private int last;
		private int next;


		private Formatter(final Appendable code) {
			this.code=code;
		}


		@Override public Appendable append(final CharSequence sequence) {

			if ( sequence == null ) {
				throw new NullPointerException("null sequence");
			}

			for (int i=0, n=sequence.length(); i < n; ++i) { append(sequence.charAt(i)); }

			return this;
		}

		@Override public Appendable append(final CharSequence sequence, final int start, final int end) {

			if ( sequence == null ) {
				throw new NullPointerException("null sequence");
			}

			for (int i=start; i < end; ++i) { append(sequence.charAt(i)); }

			return this;
		}

		@Override public Appendable append(final char c) {
			if ( code == null ) { return this; } else {
				switch ( c ) {

					case '\f': return feed();
					case '\n': return newline();

					case '\t': return indent();
					case '\b': return outdent();

					case ' ': return space();

					default: return other(c);

				}
			}
		}


		@Override public String toString() {
			return code.toString();
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private Formatter feed() {

			append('\n');
			append('\n');

			return this;
		}

		private Formatter newline() {
			if ( last == '{' ) { ++indent; }

			if ( last != '\n' || next != '\n' ) { write('\n'); }

			return this;
		}

		private Formatter indent() {

			if ( last != '\0' && last != '\n' ) { ++indent; }

			return this;
		}

		private Formatter outdent() {

			--indent;

			return this;
		}

		private Formatter space() {

			if ( last != '\0' && last != '\n' && last != ' ' ) { write(' '); }

			return this;
		}


		private Formatter other(final char c) {

			if ( last == '\n' ) {

				if ( c == '}' ) { --indent; }

				for (int i=4*indent; i > 0; --i) { write(' '); }
			}

			write(c);

			return this;
		}


		////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		private void write(final char c) {
			try {

				code.append(c);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			} finally {

				next=last;
				last=c;

			}
		}

	}

}
