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

import java.util.function.Supplier;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;

final class ShapeLabeller extends Shape.Probe<Shape> {

	private final Supplier<String> labels;


	ShapeLabeller(final Supplier<String> labels) {
		this.labels=labels;
	}


	@Override public Shape probe(final Link link) {
		return link(link.iri(), link.shape().map(this));
	}

	@Override public Shape probe(final Field field) {
		return field(labels.get(), field.iri(), field.shape().map(this));
	}


	@Override public Shape probe(final When when) {
		return when(when.test().map(this), when.pass().map(this), when.fail().map(this));
	}

	@Override public Shape probe(final And and) {
		return and(and.shapes().stream().map(this));
	}

	@Override public Shape probe(final Or or) {
		return or(or.shapes().stream().map(this));
	}


	@Override public Shape probe(final Shape shape) {
		return shape;
	}

}
