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

package com.metreeca.form.shapes;

import com.metreeca.form.Shape;
import com.metreeca.form.probes.Traverser;
import com.metreeca.form.shifts.Step;
import com.metreeca.form.things.Maps;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Stream;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.shifts.Step.step;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.*;


/**
 * Trait structural constraint.
 *
 * <p>States that the derived focus set generated by a {@link Step step} shift is consistent with a given {@link Shape
 * shape}.</p>
 */
public final class Trait implements Shape {

	public static Trait trait(final IRI iri) {
		return trait(step(iri));
	}

	public static Trait trait(final IRI iri, final Value... values) {
		return trait(step(iri), all(values));
	}

	public static Trait trait(final IRI iri, final Shape shape) {
		return trait(step(iri), shape);
	}


	public static Trait trait(final Step step) {
		return trait(step, and());
	}

	public static Trait trait(final Step step, final Shape shape) {
		return new Trait(step, shape);
	}


	public static Map<Step, Shape> traits(final Shape shape) {
		return shape == null ? emptyMap() : shape.map(new TraitProbe());
	}


	private final Step step;
	private final Shape shape;


	private Trait(final Step step, final Shape shape) {

		if ( step == null ) {
			throw new NullPointerException("null step");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.step=step;
		this.shape=shape;
	}


	public Step getStep() {
		return step;
	}

	public Shape getShape() {
		return shape;
	}


	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Trait
				&& step.equals(((Trait)object).step)
				&& shape.equals(((Trait)object).shape);
	}

	@Override public int hashCode() {
		return step.hashCode()^shape.hashCode();
	}

	@Override public String toString() {
		return "trait("+step+(shape.equals(and()) ? "" : ", "+shape)+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class TraitProbe extends Traverser<Map<Step, Shape>> {

		@Override public Map<Step, Shape> probe(final Shape shape) { return Maps.map();}


		@Override public Map<Step, Shape> probe(final Trait trait) {
			return singletonMap(trait.getStep(), trait.getShape());
		}


		@Override public Map<Step, Shape> probe(final And and) {
			return traits(and.getShapes().stream());
		}

		@Override public Map<Step, Shape> probe(final Or or) {
			return traits(or.getShapes().stream());
		}

		@Override public Map<Step, Shape> probe(final Option option) {
			return traits(Stream.of(option.getPass(), option.getFail()));
		}


		private Map<Step, Shape> traits(final Stream<Shape> stream) {
			return stream

					// collect step-to-shape mappings from nested shapes

					.flatMap(shape -> shape.map(this).entrySet().stream())

					// group by step, collect to a set of shapes and convert to an optimized conjunction

					.collect(groupingBy(Map.Entry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue,
							collectingAndThen(toCollection(LinkedHashSet::new), set ->
									set.size() == 1 ? set.iterator().next() : and(set)))));
		}

	}

}
