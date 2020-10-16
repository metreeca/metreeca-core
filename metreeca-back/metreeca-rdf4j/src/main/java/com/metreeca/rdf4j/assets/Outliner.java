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

package com.metreeca.rdf4j.assets;

import com.metreeca.json.Focus;
import com.metreeca.json.Shape;
import com.metreeca.json.shapes.*;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Collection;
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toSet;


/**
 * Shape outliner.
 *
 * <p>Recursively extracts implied RDF statements from a shape.</p>
 */
final class Outliner extends Shape.Probe<Stream<Statement>> {

	private final Collection<Value> sources;


	public Outliner(final Value... sources) {
		this(asList(sources));
	}

	public Outliner(final Collection<Value> sources) {

		if ( sources == null ) {
			throw new NullPointerException("null sources");
		}

		if ( sources.contains(null) ) {
			throw new NullPointerException("null source");
		}

		this.sources=sources;
	}


	@Override public Stream<Statement> probe(final Shape shape) {
		return Stream.empty();
	}


	@Override public Stream<Statement> probe(final Clazz clazz) {
		return sources.stream()
				.filter(Resource.class::isInstance)
				.map(source -> statement((Resource)source, RDF.TYPE, clazz.iri()));
	}

	@Override public Stream<Statement> probe(final Field field) {

		final IRI iri=field.name();
		final Shape shape=field.shape();

		return Stream.concat(

				GraphProcessor.all(shape).map(targets -> values(targets.stream()).flatMap(target -> sources.stream().flatMap(source -> direct(iri)

						? source instanceof Resource ? Stream.of(statement((Resource)source, iri, target)) :
						Stream.empty()
						: target instanceof Resource ? Stream.of(statement((Resource)target, inverse(iri), source)) :
						Stream.empty()

				))).orElse(Stream.empty()),

				shape.map(new Outliner())

		);
	}

	@Override public Stream<Statement> probe(final And and) {
		return Stream.concat(

				and.shapes().stream()

						.flatMap(shape -> shape.map(this)),


				GraphProcessor.all(and).map(values -> and.shapes().stream()

						.flatMap(shape -> shape.map(new Outliner(values(values.stream()).collect(toSet()))))

				).orElseGet(Stream::empty)

		);
	}


	private Stream<Value> values(final Stream<Value> values) {
		return values.flatMap(value -> value instanceof Focus
				? sources.stream().filter(IRI.class::isInstance).map(source -> ((Focus)value).resolve((IRI)source))
				: Stream.of(value)
		);
	}

}
