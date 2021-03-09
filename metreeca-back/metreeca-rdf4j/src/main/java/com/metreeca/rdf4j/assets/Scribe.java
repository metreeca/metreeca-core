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
import java.util.function.UnaryOperator;
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
@FunctionalInterface public interface Scribe extends UnaryOperator<Appendable> {

	public static final Pattern VariablePattern=Pattern.compile("\\{\\w+}"); // e.g. {value} // !!! hide


	public static String code(final Scribe... scribes) {
		return list(scribes).apply(new ScribeCode(new StringBuilder(1000))).toString();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Scribe nothing() {
		return code -> code;
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
		return code -> items.stream()
				.map(item -> item.apply(code))
				.reduce(code, (x, y) -> code);
	}

	public static Scribe list(final Collection<Scribe> items, final CharSequence separator) {
		return code -> items.stream()
				.flatMap(item -> Stream.of(text(separator), item)).skip(1)
				.map(item -> item.apply(code))
				.reduce(code, (x, y) -> code);
	}


	public static Scribe text(final Object value) {
		return text(valueOf(value));
	}

	public static Scribe text(final Value value) {
		return text(format(value));
	}

	public static Scribe text(final CharSequence text) {
		return code -> {
			try {

				return code.append(text);

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		};
	}

	public static Scribe text(final String format, final Object... args) {
		return text(String.format(format, args));
	}

	public static Scribe text(final String template, final Scribe... args) {
		return args.length == 0 ? text(template) : code -> {
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
					).apply(code);

					last=end;
				}

				code.append(template.subSequence(last, template.length())); // trailing text

				while ( iterator.hasNext() ) {
					iterator.next().apply(code); // trailing args
				}

				return code;

			} catch ( final IOException e ) {

				throw new UncheckedIOException(e);

			}
		};
	}

}
