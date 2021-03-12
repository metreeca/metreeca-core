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

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.stream.Stream;

import static com.metreeca.json.Values.statement;
import static com.metreeca.json.Values.traverse;
import static com.metreeca.json.shapes.All.all;

import static java.util.Arrays.stream;


final class ShapeOutliner extends Shape.Probe<Stream<Statement>> {

	private final Value[] sources;


	ShapeOutliner(final Value... sources) {
		this.sources=sources;
	}


	@Override public Stream<Statement> probe(final Clazz clazz) {
		return stream(sources)
				.filter(Resource.class::isInstance)
				.map(source -> statement((Resource)source, RDF.TYPE, clazz.iri()));
	}


	@Override public Stream<Statement> probe(final Field field) {
		return Stream.concat(

				all(field.shape())

						.map(targets -> values(targets.stream()).flatMap(target ->
								stream(sources).flatMap(source -> traverse(field.iri(),

										iri -> source instanceof Resource ?
												Stream.of(statement((Resource)source, iri, target)) : Stream.empty(),

										iri -> target instanceof Resource ?
												Stream.of(statement((Resource)target, iri, source)) : Stream.empty()

										)
								)))

						.orElse(Stream.empty()),

				field.shape().map(new ShapeOutliner())

		);
	}


	@Override public Stream<Statement> probe(final And and) {
		return Stream.concat(

				and.shapes().stream()

						.flatMap(shape -> shape.map(this)),

				all(and).map(values -> and.shapes().stream()

						.flatMap(shape -> shape.map(new ShapeOutliner(values(values.stream()).toArray(Value[]::new))))

				).orElseGet(Stream::empty)

		);
	}


	@Override public Stream<Statement> probe(final Shape shape) {
		return Stream.empty();
	}


	private Stream<Value> values(final Stream<Value> values) {
		return values.flatMap(value -> value instanceof Focus
				? stream(sources).filter(IRI.class::isInstance).map(source -> ((Focus)value).resolve((IRI)source))
				: Stream.of(value)
		);
	}

}
