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

package com.metreeca.core;

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
				failWithMessage("expected result <%s> to have error <%s>", actual, expected);
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
					failWithMessage("expected result <%s> to have error", actual);

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
				failWithMessage("expected result <%s> to have value <%s>", actual, expected);
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

					failWithMessage("expected result <%s> to have value", actual);

					return this;
				}, value -> {

					assertions.accept(value);

					return this;
				}

		);

		return this;
	}

}
