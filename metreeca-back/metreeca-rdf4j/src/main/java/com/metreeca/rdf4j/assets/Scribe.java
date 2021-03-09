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

package com.metreeca.rdf4j.assets;

import org.eclipse.rdf4j.model.Value;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.json.Values.format;

import static java.lang.String.valueOf;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;


/**
 * Source code generator.
 */
public abstract class Scribe {

	private static final Pattern VariablePattern=Pattern.compile("\\{\\w+}"); // e.g. {value}


	public static String code(final Scribe... scribes) {
		return list(scribes).scribe(

				new ScribeCode(new StringBuilder(1000)),
				new ScribeScope()

		).toString();

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Scribe nothing() {
		return scribe((code, scope) -> code);
	}

	public static Scribe nothing(final Scribe... scribes) {
		return scribe((code, scope) -> stream(scribes)
				.map(scribe -> scribe.scribe(new ScribeCode(null), scope))
				.reduce(code, (x, y) -> code)
		);
	}


	public static Scribe list(final Scribe... items) {
		return list(stream(items));
	}

	public static Scribe list(final Stream<Scribe> items) {
		return list(items.collect(toList())); // memoize to enable reuse
	}

	public static Scribe list(final Stream<Scribe> items, final CharSequence separator) {
		return list(items.collect(toList()), separator); // memoize to enable reuse
	}

	public static Scribe list(final Collection<Scribe> items) {
		return scribe((code, scope) -> items.stream()
				.map(item -> item.scribe(code, scope))
				.reduce(code, (x, y) -> code)
		);
	}

	public static Scribe list(final Collection<Scribe> items, final CharSequence separator) {
		return scribe((code, scope) -> items.stream()
				.flatMap(item -> Stream.of(text(separator), item)).skip(1)
				.map(item -> item.scribe(code, scope))
				.reduce(code, (x, y) -> code)
		);
	}


	public static Scribe id(final Object object, final Object... aliases) {
		return scribe((code, scope) -> {
			try {

				return code.append(valueOf(scope.apply(object, aliases)));

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		});
	}


	public static Scribe text(final Object value) {
		return text(valueOf(value));
	}

	public static Scribe text(final Value value) {
		return text(format(value));
	}

	public static Scribe text(final CharSequence text) {
		return scribe((code, scope) -> {
			try {

				return code.append(text);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		});
	}

	public static Scribe text(final String format, final Object... args) {
		return text(String.format(format, args));
	}

	public static Scribe text(final String template, final Scribe... args) {
		return args.length == 0 ? text(template) : scribe((code, scope) -> {
			try {

				final Matcher matcher=VariablePattern.matcher(template);
				final Map<CharSequence, Scribe> variables=new HashMap<>();
				final Iterator<Scribe> iterator=stream(args).iterator();

				int last=0;

				while ( matcher.find() ) {

					final int start=matcher.start();
					final int end=matcher.end();

					code.append(template.subSequence(last, start)); // leading text

					variables.computeIfAbsent( // cached variable value, if available
							template.subSequence(start, end),
							name -> iterator.hasNext() ? iterator.next() : nothing()
					).scribe(code, scope);

					last=end;
				}

				code.append(template.subSequence(last, template.length())); // trailing text

				while ( iterator.hasNext() ) {
					iterator.next().scribe(code, scope); // trailing args
				}

				return code;

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		});
	}


	private static Scribe scribe(final BiFunction<Appendable, BiFunction<Object, Object[], Integer>, Appendable> delegate) {
		return new Scribe() {
			@Override Appendable scribe(final Appendable code, final BiFunction<Object, Object[], Integer> scope) {
				return delegate.apply(code, scope);
			}
		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	abstract Appendable scribe(final Appendable code, final BiFunction<Object, Object[], Integer> scope);

}
