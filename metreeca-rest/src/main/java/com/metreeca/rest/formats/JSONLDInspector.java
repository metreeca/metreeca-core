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

package com.metreeca.rest.formats;

import com.metreeca.json.Shape;
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.*;
import java.util.stream.Stream;

import static com.metreeca.json.shapes.Guard.*;

import static java.lang.String.format;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toSet;

abstract class JSONLDInspector<V> extends Shape.Probe<V> {

	static Shape driver(final Shape shape) { // !!! caching
		return shape

				.redact(Role)
				.redact(Task)
				.redact(View)
				.redact(Mode, Convey) // remove internal filtering shapes

				.expand(); // add inferred constraints to drive json shorthands
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	static boolean tagged(final Shape shape) {
		return datatype(shape).filter(RDF.LANGSTRING::equals).isPresent();
	}

	static boolean localized(final Shape shape) {
		return (shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new JSONLDInspector<Object>() {

			@Override public Object probe(final Localized localized) { return localized; }

		}))).isPresent();
	}


	static Optional<IRI> datatype(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new JSONLDInspector<IRI>() {

			@Override public IRI probe(final Datatype datatype) { return datatype.iri(); }

		}));
	}

	static Optional<Set<String>> langs(final Shape shape) {
		return shape == null ? Optional.empty() : Optional.ofNullable(shape.map(new JSONLDInspector<Set<String>>() {

			@Override public Set<String> probe(final Lang lang) { return lang.tags(); }

		}));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////


	@Override public V probe(final Link link) {
		return link.shape().map(this);
	}


	@Override public V probe(final When when) {
		return value(Stream.of(when.pass(), when.fail()));
	}

	@Override public V probe(final And and) {
		return value(and.shapes().stream());
	}

	@Override public V probe(final Or or) {
		return value(or.shapes().stream());
	}


	private V value(final Stream<Shape> shapes) {

		final Set<V> values=shapes
				.map(shape -> shape.map(this))
				.filter(Objects::nonNull)
				.collect(toSet());

		if ( values.size() > 1 ) {
			throw new IllegalArgumentException(format(
					"conflicting values {%s}", values.stream().map(Object::toString).collect(joining(", "))
			));
		}

		return values.isEmpty() ? null : values.iterator().next();

	}

}
