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

package com.metreeca.rest;

import org.assertj.core.api.AbstractAssert;

import java.util.Objects;
import java.util.function.Consumer;


public final class ResultAssert<V, E> extends AbstractAssert<ResultAssert<V, E>, Result<V, E>> {

	public static <V, E> ResultAssert<V, E>  assertThat(final Result<V, E> result) {
		return new ResultAssert<>(result);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private ResultAssert(final Result<V, E> actual) {
		super(actual, ResultAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public ResultAssert<V, E> hasValue() {
		return hasValue(value -> {});
	}

	public ResultAssert<V, E> hasValue(final V expected) {
		return hasValue(value -> {
			if ( !Objects.equals(value, expected)) {
				failWithMessage("expected result <%s> to have value <%s>", actual, expected);
			}
		});
	}

	public ResultAssert<V, E> hasValue(final Consumer<V> assertions) {

		if ( assertions == null ) {
			throw new NullPointerException("null assertions");
		}

		isNotNull();

		actual.use(assertions, error -> failWithMessage("expected result <%s> to have value", actual));

		return this;
	}

	public ResultAssert<V, E> hasError() {
		return hasError(value -> {});
	}

	public ResultAssert<V, E> hasError(final E expected) {
		return hasError(error -> {
			if ( !Objects.equals(error, expected)) {
				failWithMessage("expected result <%s> to have error <%s>", actual, expected);
			}
		});
	}

	public ResultAssert<V, E> hasError(final Consumer<E> assertions) {

		if ( assertions == null ) {
			throw new NullPointerException("null assertions");
		}

		isNotNull();

		actual.use(value -> failWithMessage("expected result <%s> to have error", actual), assertions);

		return this;
	}

}
