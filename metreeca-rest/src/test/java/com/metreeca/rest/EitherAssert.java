/*
 * Copyright © 2013-2020 Metreeca srl
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

package com.metreeca.rest;

import org.assertj.core.api.AbstractAssert;

import java.util.Objects;
import java.util.function.Consumer;


public final class EitherAssert<L, R> extends AbstractAssert<EitherAssert<L, R>, Either<R, L>> {

	public static <L, R> EitherAssert<L, R> assertThat(final Either<R, L> either) {
		return new EitherAssert<>(either);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private EitherAssert(final Either<R, L> actual) {
		super(actual, EitherAssert.class);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public EitherAssert<L, R> hasLeft() {
		return hasLeft(value -> {});
	}

	public EitherAssert<L, R> hasLeft(final R expected) {
		return hasLeft(error -> {
			if ( !Objects.equals(error, expected) ) {
				failWithMessage("expected result <%s> to have left value <%s>", actual, expected);
			}
		});
	}

	public EitherAssert<L, R> hasLeft(final Consumer<R> assertions) {

		if ( assertions == null ) {
			throw new NullPointerException("null assertions");
		}

		isNotNull();

		actual.fold(

				error -> {

					assertions.accept(error);

					return this;

				}, value -> {
					failWithMessage("expected result <%s> to have left value", actual);

					return this;

				}

		);

		return this;
	}


	public EitherAssert<L, R> hasRight() {
		return hasRight(value -> {});
	}

	public EitherAssert<L, R> hasRight(final L expected) {
		return hasRight(value -> {
			if ( !Objects.equals(value, expected) ) {
				failWithMessage("expected result <%s> to have right value <%s>", actual, expected);
			}
		});
	}

	public EitherAssert<L, R> hasRight(final Consumer<L> assertions) {

		if ( assertions == null ) {
			throw new NullPointerException("null assertions");
		}

		isNotNull();

		actual.fold(

				error -> {

					failWithMessage("expected result <%s> to have right value", actual);

					return this;

				}, value -> {

					assertions.accept(value);

					return this;
				}

		);

		return this;
	}

}
