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

package com.metreeca.form.things;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Stream;


/**
 * Source code generation utilities.
 */
public final class Sources {

	private static final Pattern VariablePattern=Pattern.compile("\\{\\w+}");

	private static final HashMap<String, String[]> templates=new HashMap<>();


	@FunctionalInterface public static interface Snippet {

		void accept(final Consumer<CharSequence> source, final BiFunction<Object, Object, Integer> identifiers);

	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static String source(final String template, final Object... args) {
		return source(template(template, args));
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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	public static Snippet nothing() {
		return (source, identifiers) -> {};
	}

	public static Snippet nothing(final Object... snippets) {
		return (source, identifiers) -> generate(snippets, sequence -> {}, identifiers);
	}


	public static Snippet snippet(final Object... snippets) {
		return (source, identifiers) -> generate(snippets, source, identifiers);
	}

	public static Snippet template(final String template, final Object... args) {
		if ( template == null || template.isEmpty() ) { return nothing(); } else {

			final String[] chunks=templates.computeIfAbsent(template, VariablePattern::split);

			return (source, identifiers) -> {

				for (int i=0; i < chunks.length; i++) {

					source.accept(chunks[i]);

					if ( i < args.length ) {
						generate(args[i], source, identifiers);
					}

				}
			};
		}
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


	//// SPARQL DSL ////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Snippet var(final Object object) {
		return object == null ? nothing() : snippet("?", id(object));
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

	private Sources() {} // utility


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

			if ( c == '\t' ) {

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
