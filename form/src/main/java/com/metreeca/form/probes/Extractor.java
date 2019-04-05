/*
 * Copyright © 2013-2019 Metreeca srl. All rights reserved.
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

package com.metreeca.form.probes;

import com.metreeca.form.Shape;
import com.metreeca.form.shapes.*;

import org.eclipse.rdf4j.model.*;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static com.metreeca.form.things.Values.direct;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;


/**
 * Shape extractor.
 *
 * <p>Recursively extracts from a model and an initial collection of source values all the statements compatible with a
 * shape.</p>
 */
public final class Extractor extends Traverser<Stream<Statement>> {

	private final Collection<Resource> focus;
	private final Iterable<Statement> model;


	public Extractor(final Iterable<Statement> model, final Collection<? extends Resource> focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		this.focus=new LinkedHashSet<>(focus);
		this.model=model;
	}


	@Override public Stream<Statement> probe(final Shape shape) {
		return Stream.empty();
	}


	@Override public Stream<Statement> probe(final Field field) {

		final IRI iri=field.getIRI();

		final boolean direct=direct(iri);

		final Function<Statement, Value> source=direct ? Statement::getSubject : Statement::getObject;
		final Function<Statement, Value> target=direct ? Statement::getObject : Statement::getSubject;

		final Collection<Statement> restricted=StreamSupport.stream(model.spliterator(), false)
				.filter(s -> focus.contains(source.apply(s)) && iri.equals(s.getPredicate()))
				.collect(toList());

		final Set<Resource> focus=restricted
				.stream()
				.map(target)
				.filter(v -> v instanceof Resource)
				.map( v -> (Resource)v)
				.collect(toSet());

		return Stream.concat(restricted.stream(), field.getShape().map(new Extractor(model, focus)));
	}


	@Override public Stream<Statement> probe(final And and) {
		return and.getShapes().stream().flatMap(shape -> shape.map(this));
	}

	@Override public Stream<Statement> probe(final Or or) {
		return or.getShapes().stream().flatMap(shape -> shape.map(this));
	}

	@Override public Stream<Statement> probe(final When when) {
		return Stream.concat(when.getPass().map(this), when.getFail().map(this));
	}

}
