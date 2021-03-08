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

import java.util.*;

import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Lang.lang;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.When.when;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;

final class ShapeLocalizer extends Shape.Probe<Shape> {

	private final Set<String> tags;


	ShapeLocalizer(final Collection<String> tags) {
		this.tags=new HashSet<>(tags);
	}


	@Override public Shape probe(final Lang lang) {
		return Optional
				.of(lang.tags().stream().filter(tags::contains).collect(toSet()))
				.filter(set -> !set.isEmpty())
				.map(Lang::lang)
				.orElseGet(() -> tags.isEmpty() ? lang : lang(tags));
	}


	@Override public Shape probe(final Field field) {
		return field(field.alias(), field.iri(), field.shape().map(this));
	}


	@Override public Shape probe(final When when) {
		return when(
				when.test().map(this),
				when.pass().map(this),
				when.fail().map(this)
		);
	}

	@Override public Shape probe(final And and) {
		return and(and.shapes().stream().map(this).collect(toList()));
	}

	@Override public Shape probe(final Or or) {
		return or(or.shapes().stream().map(this).collect(toList()));
	}


	@Override public Shape probe(final Shape shape) {
		return shape;
	}

}
