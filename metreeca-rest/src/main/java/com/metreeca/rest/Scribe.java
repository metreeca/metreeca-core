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
import java.util.*;
import java.util.function.UnaryOperator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.json.Values.format;

import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * Indenting code writer and utilities.
 */
public final class Scribe implements Appendable {

	private static final Pattern VariablePattern=Pattern.compile("\\{\\w+}"); // e.g. {value}


	@SafeVarargs public static String code(final UnaryOperator<Appendable>... scribes) {
		return list(scribes).apply(new Scribe(new StringBuilder(1000))).toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static UnaryOperator<Appendable> nothing() {
		return code -> code;
	}


	@SafeVarargs public static UnaryOperator<Appendable> list(final UnaryOperator<Appendable>... items) {
		return list(stream(items));
	}

	public static UnaryOperator<Appendable> list(final Stream<UnaryOperator<Appendable>> items) {
		return list(items.collect(toList())); // memoize to enable reuse
	}

	public static UnaryOperator<Appendable> list(final Collection<UnaryOperator<Appendable>> items) {
		return code -> items.stream()
				.map(item -> item.apply(code))
				.reduce(code, (x, y) -> code);
	}


	public static UnaryOperator<Appendable> list(
			final Stream<UnaryOperator<Appendable>> items, final CharSequence separator
	) {
		return list(items.collect(toList()), separator); // memoize to enable reuse
	}

	public static UnaryOperator<Appendable> list(
			final Collection<UnaryOperator<Appendable>> items, final CharSequence separator
	) {
		return code -> items.stream()
				.flatMap(item -> Stream.of(text(separator), item)).skip(1)
				.map(item -> item.apply(code))
				.reduce(code, (x, y) -> code);
	}


	public static UnaryOperator<Appendable> text(final Object value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return text(valueOf(value));
	}

	public static UnaryOperator<Appendable> text(final Value value) {

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return text(format(value));
	}

	public static UnaryOperator<Appendable> text(final CharSequence text) {

		if ( text == null ) {
			throw new NullPointerException("null text");
		}

		return code -> {
			try {

				return code.append(text);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		};
	}


	public static UnaryOperator<Appendable> text(final String format, final Object... args) {

		if ( format == null ) {
			throw new NullPointerException("null format");
		}

		return text(String.format(format, args));
	}

	@SafeVarargs public static UnaryOperator<Appendable> text(
			final String template, final UnaryOperator<Appendable>... args
	) {

		if ( template == null ) {
			throw new NullPointerException("null template");
		}

		return args.length == 0 ? text(template) : code -> {
			try {

				final Matcher matcher=VariablePattern.matcher(template);
				final Map<CharSequence, UnaryOperator<Appendable>> variables=new HashMap<>();
				final Iterator<UnaryOperator<Appendable>> iterator=stream(args).iterator();

				int last=0;

				while ( matcher.find() ) {

					final int start=matcher.start();
					final int end=matcher.end();

					code.append(template.subSequence(last, start)); // leading text

					final CharSequence name=template.subSequence(start, end);

					variables.computeIfAbsent(name, _name -> { // cached variable value

						if ( !iterator.hasNext() ) {
							throw new IllegalArgumentException(String.format(
									"missing argument for variable {%s}", _name
							));
						}

						return iterator.next();

					}).apply(code);

					last=end;
				}

				code.append(template.subSequence(last, template.length())); // trailing text

				if ( iterator.hasNext() ) {
					throw new IllegalArgumentException("redundant trailing arguments");
				}

				return code;

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Appendable code;

	private int indent;

	private int last;
	private int next;


	/**
	 * @param code the delegate appendable; ignored if {@code null}
	 */
	Scribe(final Appendable code) { this.code=code; }


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Appendable append(final CharSequence sequence) {

		if ( sequence == null ) {
			throw new NullPointerException("null sequence");
		}

		if ( code != null ) {
			for (int i=0, n=sequence.length(); i < n; ++i) { append(sequence.charAt(i)); }
		}

		return this;
	}

	@Override public Appendable append(final CharSequence sequence, final int start, final int end) {

		if ( sequence == null ) {
			throw new NullPointerException("null sequence");
		}

		if ( code != null ) {
			for (int i=start; i < end; ++i) { append(sequence.charAt(i)); }
		}

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

	private Scribe feed() {

		append('\n');
		append('\n');

		return this;
	}

	private Scribe newline() {
		if ( last == '{' ) { ++indent; }

		if ( last != '\n' || next != '\n' ) { write('\n'); }

		return this;
	}

	private Scribe indent() {

		if ( last != '\0' && last != '\n' ) { ++indent; }

		return this;
	}

	private Scribe outdent() {

		--indent;

		return this;
	}

	private Scribe space() {

		if ( last != '\0' && last != '\n' && last != ' ' ) { write(' '); }

		return this;
	}


	private Scribe other(final char c) {

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
