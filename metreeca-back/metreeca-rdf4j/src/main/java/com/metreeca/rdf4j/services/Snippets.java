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

package com.metreeca.rdf4j.services;

import com.metreeca.rdf.Values;
import org.eclipse.rdf4j.model.IRI;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static com.metreeca.rdf.Values.direct;
import static com.metreeca.rdf.Values.inverse;
import static java.util.Arrays.stream;
import static java.util.Collections.emptyIterator;


/**
 * Source code generation utilities.
 */
final class Snippets {

	private static final Pattern VariablePattern=Pattern.compile("\\{\\w+}"); // e.g. {value}


	@FunctionalInterface public static interface Snippet {

		void accept(final Consumer<CharSequence> source, final BiFunction<Object, Object, Integer> identifiers);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String source(final String template, final Object... args) {
		return source(snippet(template, args));
	}

	public static String source(final Object... snippets) {

		final Appendable source=new Indenter(new StringBuilder(1000));
		final Map<Object, Integer> identifiers=new IdentityHashMap<>();

		snippet(snippets).accept(

				sequence -> {
					try {
						source.append(sequence);
					} catch ( final IOException e ) {
						throw new UncheckedIOException(e);
					}
				},

				(object, alias) -> {

					final Integer o=identifiers.computeIfAbsent(object, x -> identifiers.size());
					final Integer a=identifiers.computeIfAbsent(alias, x -> o);

					if ( !a.equals(o) ) {
						throw new IllegalStateException("alias is already linked to a different identifier");
					}

					return o;

				}

		);

		return source.toString();

	}


	//// SPARQL DSL ////////////////////////////////////////////////////////////////////////////////////////////////////

	static Snippet path(final Collection<IRI> path) {
		return list(path.stream().map(Values::format), '/');
	}

	static Snippet path(final Object source, final Collection<IRI> path, final Object target) {
		return source == null || path == null || path.isEmpty() || target == null ? nothing()
				: snippet(source, " ", path(path), " ", target, " .\n");
	}

	static Snippet edge(final Object source, final IRI iri, final Object target) {
		return source == null || iri == null || target == null ? nothing() : direct(iri)
				? snippet(source, " ", Values.format(iri), " ", target, " .\n")
				: snippet(target, " ", Values.format(inverse(iri)), " ", source, " .\n");
	}

	static Snippet var() {
		return var(new Object());
	}

	static Snippet var(final Object object) {
		return object == null ? nothing() : snippet((Object)"?", id(object));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static Snippet nothing() {
		return (source, identifiers) -> {};
	}

	static Snippet nothing(final Object... snippets) {
		return (source, identifiers) -> generate(snippets, sequence -> {}, identifiers);
	}


	static Snippet snippet(final String template, final Object... args) {
		return template == null || template.isEmpty() ? snippet(args) : (source, identifiers) -> {

			final Matcher matcher=VariablePattern.matcher(template);
			final Map<CharSequence, Object> variables=new HashMap<>();
			final Iterator<Object> iterator=(args == null) ? emptyIterator() : stream(args).iterator();

			int last=0;

			while ( matcher.find() ) {

				final int start=matcher.start();
				final int end=matcher.end();

				source.accept(template.subSequence(last, start)); // leading text

				generate(variables.computeIfAbsent( // cached variable value, if available
						template.subSequence(start, end),
						name -> iterator.hasNext() ? iterator.next() : null
				), source, identifiers);

				last=end;
			}

			source.accept(template.subSequence(last, template.length())); // trailing text

			while ( iterator.hasNext() ) {
				generate(iterator.next(), source, identifiers); // trailing args
			}

		};
	}

	static Snippet snippet(final Object... snippets) {
		return (source, identifiers) -> generate(snippets, source, identifiers);
	}


	public static Snippet list(final Stream<?> items, final Object separator) {
		return items == null ? nothing() : snippet(items.flatMap(item -> Stream.of(separator, item)).skip(1));
	}


	//// Identifiers ///////////////////////////////////////////////////////////////////////////////////////////////////

	public static Snippet id(final Object object) {
		return id(object, (Object[])null);
	}

	public static Snippet id(final Object object, final Object... aliases) {
		return object == null ? nothing() : (source, identifiers) -> {

			final Integer id=identifiers.apply(object, object);

			if ( aliases != null ) {
				for (final Object alias : aliases) {
					identifiers.apply(object, alias);
				}
			}

			source.accept(String.valueOf(id));

		};
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static void generate(
			final Object object,
			final Consumer<CharSequence> source,
			final BiFunction<Object, Object, Integer> identifiers
	) {

		if ( object instanceof Stream ) {

			((Stream<?>)object).forEach(o -> generate(o, source, identifiers));

		} else if ( object instanceof Iterable ) {

			((Iterable<?>)object).forEach(o -> generate(o, source, identifiers));

		} else if ( object instanceof Object[] ) {

			for (final Object o : (Object[])object) { generate(o, source, identifiers); }

		} else if ( object instanceof Snippet ) {

			((Snippet)object).accept(source, identifiers);

		} else if ( object != null ) {

			source.accept(object.toString());

		}

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private Snippets() {} // utility


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Indenter implements Appendable {

		private final Appendable target;

		private int indent;

		private int last;
		private int next;


		private Indenter(final Appendable target) { this.target=target; }


		@Override public Appendable append(final CharSequence sequence) throws IOException {

			final CharSequence s=sequence == null ? "null" : sequence;

			for (int i=0, n=s.length(); i < n; ++i) { append(s.charAt(i)); }

			return this;
		}

		@Override public Appendable append(final CharSequence sequence, final int start, final int end) throws IOException {

			final CharSequence s=sequence == null ? "null" : sequence;

			for (int i=start; i < end; ++i) { append(s.charAt(i)); }

			return this;
		}

		@Override public Appendable append(final char c) throws IOException {

			if ( c == '\f' ) {

				append('\n').append('\n');

			} else if ( c == '\t' ) {

				if ( last != '\0' && last != '\n' ) {
					++indent;
				}

			} else if ( c == '\b' ) {

				--indent;

			} else if ( c == '\n' ) {

				if ( last == '{' ) { ++indent; }

				if ( last != '\n' || next != '\n' ) {
					emit('\n');
				}

			} else if ( c == ' ' ) {

				if ( last != '\0' && last != '\n' && last != ' ' ) {
					emit(' ');
				}

			} else {

				if ( last == '\n' ) {

					if ( c == '}' ) { --indent; }

					for (int i=4*indent; i > 0; --i) {
						emit(' ');
					}
				}

				emit(c);
			}

			return this;
		}


		@Override public String toString() { return target.toString(); }


		private void emit(final char c) throws IOException {
			try {
				target.append(c);
			} finally {
				next=last;
				last=c;
			}
		}

	}

}
