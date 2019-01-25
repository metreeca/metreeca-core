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
import com.metreeca.form.shapes.*;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;

import java.util.Collection;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Stream;

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

	private final Collection<Statement> model;
	private final Collection<Value> focus;


	public Extractor(final Collection<Statement> model, final Collection<Value> focus) {
		this.model=model;
		this.focus=focus;
	}


	@Override public Stream<Statement> probe(final Shape shape) {
		return Stream.empty();
	}


	@Override public Stream<Statement> probe(final Trait trait) {

		final IRI iri=trait.getIRI();

		final boolean direct=direct(iri);

		final Function<Statement, Value> source=direct ? Statement::getSubject : Statement::getObject;
		final Function<Statement, Value> target=direct ? Statement::getObject : Statement::getSubject;

		final Collection<Statement> restricted=model.stream()
				.filter(s -> focus.contains(source.apply(s)) && iri.equals(s.getPredicate()))
				.collect(toList());

		final Set<Value> focus=restricted.stream()
				.map(target)
				.collect(toSet());

		return Stream.concat(restricted.stream(), trait.getShape().map(new Extractor(model, focus)));
	}


	@Override public Stream<Statement> probe(final And and) {
		return and.getShapes().stream().flatMap(shape -> shape.map(this));
	}

	@Override public Stream<Statement> probe(final Or or) {
		return or.getShapes().stream().flatMap(shape -> shape.map(this));
	}

	@Override public Stream<Statement> probe(final Option option) {
		return Stream.concat(option.getPass().map(this), option.getFail().map(this));
	}

}
