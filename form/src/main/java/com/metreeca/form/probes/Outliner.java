/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it under the terms
 * of the GNU Affero General Public License as published by the Free Software Foundation,
 * either version 3 of the License, or(at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY;
 * without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License along with Metreeca.
 * If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.And;
import com.metreeca.form.shapes.Clazz;
import com.metreeca.form.shapes.Trait;
import com.metreeca.form.shifts.Step;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Collection;
import java.util.stream.Stream;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.things.Values.statement;

import static java.util.Arrays.asList;


/**
 * Shape outliner.
 *
 * <p>Recursively extracts implied RDF statements from a shape.</p>
 */
public final class Outliner extends Visitor<Stream<Statement>> {

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
				.filter(source -> source instanceof Resource)
				.map(source -> statement((Resource)source, RDF.TYPE, clazz.getIRI()));
	}

	@Override public Stream<Statement> probe(final Trait trait) {

		final Step step=trait.getStep();
		final Shape shape=trait.getShape();

		final boolean inverse=step.isInverse();
		final IRI iri=step.getIRI();

		return Stream.concat(

				all(shape).map(targets -> targets.stream().flatMap(target -> sources.stream().flatMap(source -> inverse

						? target instanceof Resource ? Stream.of(statement((Resource)target, iri, source)) : Stream.empty()
						: source instanceof Resource ? Stream.of(statement((Resource)source, iri, target)) : Stream.empty()

				))).orElse(Stream.empty()),

				shape.map(new Outliner())

		);
	}


	@Override public Stream<Statement> probe(final And and) {
		return Stream.concat(

				and.getShapes().stream()

						.flatMap(shape -> shape.map(this)),


				all(and).map(values -> and.getShapes().stream()

						.flatMap(shape -> shape.map(new Outliner(values)))

				).orElseGet(Stream::empty)

		);
	}

}
