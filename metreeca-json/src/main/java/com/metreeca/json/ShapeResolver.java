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

import org.eclipse.rdf4j.model.Value;

import java.util.*;

import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Any.any;
import static com.metreeca.json.shapes.Field.field;
import static com.metreeca.json.shapes.Link.link;
import static com.metreeca.json.shapes.MaxExclusive.maxExclusive;
import static com.metreeca.json.shapes.MaxInclusive.maxInclusive;
import static com.metreeca.json.shapes.MinExclusive.minExclusive;
import static com.metreeca.json.shapes.MinInclusive.minInclusive;
import static com.metreeca.json.shapes.Or.or;
import static com.metreeca.json.shapes.Range.range;
import static com.metreeca.json.shapes.When.when;

import static java.util.stream.Collectors.toCollection;

final class ShapeResolver extends Shape.Probe<Shape> {

	private final Value focus;


	ShapeResolver(final Value focus) {
		this.focus=focus;
	}


	private Value value(final Value value) {
		return value instanceof Focus
				? ((Focus)value).resolve(focus)
				: value;
	}

	private Set<Value> values(final Collection<Value> values) {
		return values.stream()
				.map(this::value)
				.collect(toCollection(LinkedHashSet::new));
	}


	@Override public Shape probe(final Range range) {
		return range(values(range.values()));
	}


	@Override public Shape probe(final MinExclusive minExclusive) {
		return minExclusive(value(minExclusive.limit()));
	}

	@Override public Shape probe(final MaxExclusive maxExclusive) {
		return maxExclusive(value(maxExclusive.limit()));
	}

	@Override public Shape probe(final MinInclusive minInclusive) {
		return minInclusive(value(minInclusive.limit()));
	}

	@Override public Shape probe(final MaxInclusive maxInclusive) {
		return maxInclusive(value(maxInclusive.limit()));
	}


	@Override public Shape probe(final All all) {
		return all(values(all.values()));
	}

	@Override public Shape probe(final Any any) {
		return any(values(any.values()));
	}


	@Override public Shape probe(final Link link) {
		return link(link.iri(), link.shape().map(this));
	}

	@Override public Shape probe(final Field field) {
		return field(field.label(), field.iri(), field.shape().map(this));
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
