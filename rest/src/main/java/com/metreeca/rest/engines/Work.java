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

import java.io.IOException;


final class Work {

	static Appendable indent(final Appendable target) {

		if ( target == null ) {
			throw new NullPointerException("null target");
		}

		return new IndentingAppendable(target);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class IndentingAppendable implements Appendable {

		private final Appendable target;

		private int indent;

		private int last;
		private int next;


		private IndentingAppendable(final Appendable target) { this.target=target; }


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
