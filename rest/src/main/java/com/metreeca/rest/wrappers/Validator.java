/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

import com.metreeca.form.Focus;
import com.metreeca.form.Issue;
import com.metreeca.rest.*;
import com.metreeca.rest.bodies.RDFBody;

import org.eclipse.rdf4j.model.Statement;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.function.BiFunction;

import static com.metreeca.rest.Result.Error;
import static com.metreeca.rest.Result.Value;
import static com.metreeca.rest.bodies.RDFBody.rdf;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toList;


/**
 * Validating preprocessor.
 *
 * <p>Applies custom validation {@linkplain #Validator(Collection) rules} to the {@linkplain RDFBody RDF payload} of
 * incoming requests.</p>
 */
public final class Validator implements Wrapper {

	private final Collection<BiFunction<Request, Collection<Statement>, Collection<Issue>>> rules;


	@SafeVarargs public Validator(final BiFunction<Request, Collection<Statement>, Collection<Issue>>... rules) {
		this(asList(rules));
	}

	/**
	 * Creates a validating preprocessor.
	 *
	 * <p>Validation rules are handled a target request and its RDF payload and must return a non-null but possibly
	 * empty collection of validation issues; if the collection includes at least one {@linkplain Issue.Level#Error
	 * error}, the request fails with a {@link Response#UnprocessableEntity} status code; otherwise, the request is
	 * routed to the wrapped handler.</p>
	 *
	 * @param rules the custom validation rules to be applied to the {@linkplain RDFBody RDF payload} of incoming
	 *              requests
	 *
	 * @throws NullPointerException if {@code rules} is null or contains null values
	 */
	public Validator(final Collection<BiFunction<Request, Collection<Statement>, Collection<Issue>>> rules) {

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
		return request -> handler.handle(request.pipe(rdf(), model -> {

			final Focus report=Focus.focus(rules.stream()
					.flatMap(rule -> rule.apply(request, model).stream())
					.collect(toList()));

			return report.assess(Issue.Level.Error) ? Error(new Failure()

					.status(Response.UnprocessableEntity)
					.error(Failure.DataInvalid)
					.trace(report)

			) : Value(model);

		}));
	}

}
