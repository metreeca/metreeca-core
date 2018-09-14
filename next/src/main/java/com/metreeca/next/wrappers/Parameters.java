/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
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

package com.metreeca.next.wrappers;

import com.metreeca.next.Handler;
import com.metreeca.next.Wrapper;

import java.io.UncheckedIOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import static java.util.Collections.unmodifiableMap;


/**
 * Request parameters parsing wrapper.
 *
 * @deprecated Work in progress
 */
@Deprecated final class Parameters implements Wrapper {

	@Override public Handler wrap(final Handler handler) {
		return request -> handler.handle(request.query().isEmpty() || !request.parameters().isEmpty() ?
				request : request.parameters(xxx(request.query())
		));
	}

	// !!! support URLEncodedForms in body
	//  private Supplier<Map<String, List<String>>> parameters=() -> parameters(method.equals(POST)
	//		&& getHeader("Content-Type").orElse("").startsWith(URLEncodedForm) // ignore charset parameter
	//		? getText() : query);


	private static Map<String, List<String>> xxx(final String query) {

		final Map<String, List<String>> parameters=new LinkedHashMap<>();

		final int length=query.length();

		for (int head=0, tail; head < length; head=tail+1) {
			try {

				final int equal=query.indexOf('=', head);
				final int ampersand=query.indexOf('&', head);

				tail=(ampersand >= 0) ? ampersand : length;

				final boolean split=equal >= 0 && equal < tail;

				final String label=URLDecoder.decode(query.substring(head, split ? equal : tail), "UTF-8");
				final String value=URLDecoder.decode(query.substring(split ? equal+1 : tail, tail), "UTF-8");

				parameters.compute(label, (name, values) -> {

					final List<String> strings=(values != null) ? values : new ArrayList<>();

					strings.add(value);

					return strings;

				});

			} catch ( final UnsupportedEncodingException unexpected ) {
				throw new UncheckedIOException(unexpected);
			}
		}

		return unmodifiableMap(parameters);
	}

}
