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

import javax.json.Json;
import java.util.*;
import java.util.function.Function;

import static com.metreeca.rest.MessageException.status;
import static com.metreeca.rest.Response.UnprocessableEntity;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;


/**
 * Validating preprocessor.
 *
 * <p>Applies custom validation {@linkplain #validator(Function[]) rules} to incoming requests.</p>
 */
public final class Validator implements Wrapper {

	/**
	 * Creates a validating preprocessor.
	 *
	 * <p>Validation rules handle a target request and must return a non-null but possibly empty collection of
	 * validation issues; if the collection is not empty, the request fails with a {@link Response#UnprocessableEntity}
	 * status code; otherwise, the request is routed to the wrapped handler.</p>
	 *
	 * @param rules the custom validation rules to be applied to incoming requests
	 *
	 * @return a new validator
	 *
	 * @throws NullPointerException if {@code rules} is null or contains null values
	 */
	@SafeVarargs public static Validator validator(final Function<Request, Collection<String>>... rules) {
		return new Validator(asList(rules));
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
	 * @return a new validator
	 *
	 * @throws NullPointerException if {@code rules} is null or contains null values
	 */
	public static Validator validator(final Collection<Function<Request, Collection<String>>> rules) {

		if ( rules == null ) {
			throw new NullPointerException("null rules");
		}

		if ( rules.contains(null) ) {
			throw new NullPointerException("null rule");
		}

		return new Validator(rules);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Collection<Function<Request, Collection<String>>> rules;


	private Validator(final Collection<Function<Request, Collection<String>>> rules) {
		this.rules=new LinkedHashSet<>(rules);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public Handler wrap(final Handler handler) {
		return request -> Optional

				.of(rules.stream()
						.flatMap(rule -> rule.apply(request).stream())
						.collect(toList())
				)

				.filter(issues -> !issues.isEmpty())

				.map(issues -> Json.createObjectBuilder()
						.add("", Json.createArrayBuilder(issues)) // !!! align with JSON validator format
						.build()
				)

				.map(details -> request.reply(status(UnprocessableEntity, details)))

				.orElseGet(() -> handler.handle(request));
	}

}
