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
