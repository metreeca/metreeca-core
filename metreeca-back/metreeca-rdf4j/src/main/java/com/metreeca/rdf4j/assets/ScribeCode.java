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

import java.io.IOException;
import java.io.UncheckedIOException;

final class ScribeCode implements Appendable {

	private final Appendable code;

	private int indent;

	private int last;
	private int next;


	/**
	 * @param code the delegate appendable; ignored if {@code null}
	 */
	ScribeCode(final Appendable code) { this.code=code; }


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

	private ScribeCode feed() {

		append('\n');
		append('\n');

		return this;
	}

	private ScribeCode newline() {
		if ( last == '{' ) { ++indent; }

		if ( last != '\n' || next != '\n' ) { write('\n'); }

		return this;
	}

	private ScribeCode indent() {

		if ( last != '\0' && last != '\n' ) { ++indent; }

		return this;
	}

	private ScribeCode outdent() {

		--indent;

		return this;
	}

	private ScribeCode space() {

		if ( last != '\0' && last != '\n' && last != ' ' ) { write(' '); }

		return this;
	}


	private ScribeCode other(final char c) {

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
