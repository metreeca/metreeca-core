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

import java.util.Optional;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Guard.*;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;

import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

final class ShapePruner extends Shape.Probe<Shape> {

	private final boolean deep;


	ShapePruner(final boolean deep) { this.deep=deep; }


	@Override public Shape probe(final Field field) {
		return field.shape().map(this).map(shape ->
				deep && shape.empty() ? shape : field(field.alias(), field.iri(), shape)
		);
	}


	@Override public Shape probe(final Guard guard) {
		return guard.axis().equals(Mode)
				? guard.values().contains(deep ? Filter : Convey) ? and() : or()
				: guard;
	}

	@Override public Shape probe(final When when) {
		return when.test().map(this).map(test -> Optional
				.ofNullable(test.map(new ShapeEvaluator())) // constant? retain nested shapes w/o processing
				.map(value -> value.equals(TRUE) ? when.pass() : value.equals(FALSE) ? when.fail() : null)
				.orElseGet(() -> when(test, when.pass().map(this), when.fail().map(this)))
		);
	}

	@Override public Shape probe(final And and) {
		return and(and.shapes().stream().map(this));
	}

	@Override public Shape probe(final Or or) {
		return or(or.shapes().stream().map(this));
	}


	@Override public Shape probe(final Shape shape) {
		return and();
	}

}
