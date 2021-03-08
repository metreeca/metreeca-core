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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;

import java.util.Optional;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.MovedPermanently;

import static java.util.function.UnaryOperator.identity;

/**
 * Pattern-based relocating preprocessor.
 *
 * <p>Iteratively applies pattern-based {@linkplain #rewrite(String, String) rewrite} rules to incoming
 * {@linkplain Request#item() request items} and redirecting them if actually modified.</p>
 */
public final class Relocator implements Wrapper {

	/**
	 * Creates a relocating preprocessor.
	 *
	 * @return a new relocating preprocessor redirecting requests with a {@link Response#MovedPermanently} status code
	 */
	public static Relocator relocator() {
		return relocator(MovedPermanently);
	}

	/**
	 * Creates a relocating preprocessor with a custom status code.
	 *
	 * @param status the status code to be used for relocation
	 *
	 * @return a new relocating preprocessor redirecting requests with the given {@code status} code
	 *
	 * @throws IllegalArgumentException if {@code status} is less than 100 or greater than 599
	 */
	public static Relocator relocator(final int status) {

		if ( status < 100 || status > 599 ) {
			throw new IllegalArgumentException("illegal status code ["+status+"]");
		}

		return new Relocator(status);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final int status;

	private Function<String, String> rewriter=identity();


	private Relocator(final int status) {
		this.status=status;
	}


	/**
	 * Append a rewriting rule.
	 *
	 * @param pattern     the pattern request item are to be matched against; applies to whole request item strings
	 * @param replacement the replacement pattern for rewriting request items matching {@code pattern}
	 *
	 * @return this relocating preprocessor
	 *
	 * @throws NullPointerException if either {@code pattern} or {@code replacement} is null
	 */
	public Relocator rewrite(final String pattern, final String replacement) {

		if ( pattern == null ) {
			throw new NullPointerException("null pattern");
		}

		if ( replacement == null ) {
			throw new NullPointerException("null replacement");
		}

		return rewrite(Pattern.compile(pattern), replacement);
	}

	/**
	 * Append a rewriting rule.
	 *
	 * @param pattern     the pattern request item are to be matched against; applies to whole request item strings
	 * @param replacement the replacement pattern for rewriting request items matching {@code pattern}
	 *
	 * @return this relocating preprocessor
	 *
	 * @throws NullPointerException if either {@code pattern} or {@code replacement} is null
	 */
	public Relocator rewrite(final Pattern pattern, final String replacement) {

		if ( pattern == null ) {
			throw new NullPointerException("null pattern");
		}

		if ( replacement == null ) {
			throw new NullPointerException("null replacement");
		}

		rewriter=rewriter.andThen(url -> Optional
				.of(pattern.matcher(url))
				.filter(Matcher::matches)
				.map(matcher -> matcher.replaceAll(replacement))
				.orElse(url)
		);

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> {

			final String original=request.item();
			final String location=rewriter.apply(original);

			return location.equals(original)
					? handler.handle(request)
					: request.reply(status(status, location));

		};
	}

}
