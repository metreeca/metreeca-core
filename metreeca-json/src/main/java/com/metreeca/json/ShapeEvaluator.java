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

package com.metreeca.json;

import com.metreeca.json.shapes.*;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;


final class ShapeEvaluator extends Shape.Probe<Boolean> {

	@Override public Boolean probe(final And and) {
		return and.shapes().stream()
				.map(shape -> shape.map(this))
				.reduce(true, (x, y) -> x == null || y == null ? null : x && y);
	}

	@Override public Boolean probe(final Or or) {
		return or.shapes().stream()
				.map(shape -> shape.map(this))
				.reduce(false, (x, y) -> x == null || y == null ? null : x || y);
	}

	@Override public Boolean probe(final When when) {

		final Boolean test=when.test().map(this);
		final Boolean pass=when.pass().map(this);
		final Boolean fail=when.fail().map(this);

		return TRUE.equals(test) ? pass
				: FALSE.equals(test) ? fail
				: TRUE.equals(pass) && TRUE.equals(fail) ? TRUE
				: FALSE.equals(pass) && FALSE.equals(fail) ? FALSE
				: null;
	}


	@Override public Boolean probe(final Shape shape) {
		return null;
	}

}