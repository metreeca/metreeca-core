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
import com.metreeca.form.shifts.Step;

import org.eclipse.rdf4j.model.Resource;
import org.eclipse.rdf4j.model.Statement;
import org.eclipse.rdf4j.model.Value;
import org.eclipse.rdf4j.model.vocabulary.RDF;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.things.Sets.set;
import static com.metreeca.form.things.Sets.union;
import static com.metreeca.form.things.Values.statement;

import static java.util.Arrays.asList;
import static java.util.stream.Collectors.toCollection;


/**
 * Shape outliner.
 *
 * <p>Recursively extracts implied RDF statements from a shape.</p>
 */
public final class Outliner extends Visitor<Set<Statement>> { // !!! review/optimize

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


	@Override public Set<Statement> probe(final Shape shape) { return set(); }


	@Override public Set<Statement> probe(final Clazz clazz) {

		final Set<Statement> statements=new LinkedHashSet<>();

		for (final Value source : sources) {
			if ( source instanceof Resource ) {
				statements.add(statement((Resource)source, RDF.TYPE, clazz.getIRI()));
			}
		}

		return statements;
	}

	@Override public Set<Statement> probe(final Trait trait) {

		final Step step=trait.getStep();
		final Shape shape=trait.getShape();

		return union(

				all(shape).map(targets -> {

					final Set<Statement> statements=new LinkedHashSet<>();

					for (final Value source : sources) {
						for (final Value target : targets) {
							if ( step.isInverse() ) {

								if ( target instanceof Resource ) {
									statements.add(statement((Resource)target, step.getIRI(), source));
								}

							} else {

								if ( source instanceof Resource ) {
									statements.add(statement((Resource)source, step.getIRI(), target));
								}

							}
						}
					}

					return statements;

				}).orElse(set()),

				shape.map(new Outliner()));
	}


	@Override public Set<Statement> probe(final And and) {
		return union(

				and.getShapes().stream()

						.flatMap(shape -> shape.map(this).stream())
						.collect(toCollection(LinkedHashSet::new)),

				all(and).map(values -> and.getShapes().stream()

						.flatMap(shape -> shape.map(new Outliner(values)).stream())
						.collect(toCollection(LinkedHashSet::new))

				).orElseGet(LinkedHashSet::new));
	}

}
