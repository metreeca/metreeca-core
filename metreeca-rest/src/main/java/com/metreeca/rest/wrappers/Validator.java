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

package com.metreeca.rest.wrappers;

import com.metreeca.rest.*;
import com.metreeca.tree.Trace;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.Function;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.UnprocessableEntity;
import static com.metreeca.tree.Trace.trace;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;


/**
 * Validating preprocessor.
 *
 * <p>Applies custom validation {@linkplain #Validator(Collection) rules} to incoming requests.</p>
 */
public final class Validator implements Wrapper {

	private final Collection<Function<Request, Collection<String>>> rules;


	@SafeVarargs public Validator(final Function<Request, Collection<String>>... rules) {
		this(asList(rules));
	}

	/**
	 * Creates a validating preprocessor.
	 *
	 * <p>Validation rules handle a target request and must return a non-null but possibly empty collection of
	 * validation issues; if the collection is not empty, the request fails with a
	 * {@link Response#UnprocessableEntity} status code; otherwise, the request is routed to the wrapped handler.</p>
	 *
	 * @param rules the custom validation rules to be applied to incoming requests
	 *
	 * @throws NullPointerException if {@code rules} is null or contains null values
	 */
	public Validator(final Collection<Function<Request, Collection<String>>> rules) {

		if ( rules == null ) {
			throw new NullPointerException("null rules");
		}

		if ( rules.contains(null) ) {
			throw new NullPointerException("null rule");
		}

		this.rules=new LinkedHashSet<>(rules);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> {

			final Trace trace=trace(rules.stream()
					.flatMap(rule -> rule.apply(request).stream())
					.map(Trace::trace)
					.collect(toList())
			);

			return trace.isEmpty()
					? handler.handle(request)
					: request.reply(status(UnprocessableEntity, trace.toJSON()));

		};
	}

}
