/*
 * Copyright © 2019 Metreeca srl. All rights reserved.
 */

package com.metreeca.rest;

import org.assertj.core.api.AbstractAssert;

import java.util.Objects;
import java.util.function.Consumer;


public final class ResultAssert<V, E> extends AbstractAssert<ResultAssert<V, E>, Result<V, E>> {

	public static <V, E> ResultAssert<V, E>  assertThat(final Result<V, E> result) {
		return new ResultAssert(result);
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
