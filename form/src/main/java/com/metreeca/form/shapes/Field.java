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

package com.metreeca.form.shapes;

import com.metreeca.form.Shape;
import com.metreeca.form.probes.Traverser;
import com.metreeca.form.things.Maps;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.stream.Stream;

import static com.metreeca.form.shapes.All.all;
import static com.metreeca.form.shapes.And.and;
import static com.metreeca.form.things.Values.format;

import static java.util.Collections.emptyMap;
import static java.util.Collections.singletonMap;
import static java.util.stream.Collectors.*;


/**
 * Field structural constraint.
 *
 * <p>States that the derived focus set generated by following a single step path is consistent with a given {@link
 * Shape shape}.</p>
 */
public final class Field implements Shape {

	public static Field field(final IRI iri) {
		return new Field(iri, and());
	}

	public static Field field(final IRI iri, final Value... values) {
		return new Field(iri, all(values));
	}

	public static Field field(final IRI iri, final Shape shape) {
		return new Field(iri, shape);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static Map<IRI, Shape> fields(final Shape shape) {
		return shape == null ? emptyMap() : shape.map(new FieldProbe());
	}


	private final IRI iri;
	private final Shape shape;


	private Field(final IRI iri, final Shape shape) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		this.iri=iri;
		this.shape=shape;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public IRI getIRI() {
		return iri;
	}

	public Shape getShape() {
		return shape;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Field
				&& iri.equals(((Field)object).iri)
				&& shape.equals(((Field)object).shape);
	}

	@Override public int hashCode() {
		return iri.hashCode()^shape.hashCode();
	}

	@Override public String toString() {
		return "field("+format(iri)+(shape.equals(and()) ? "" : ", "+shape)+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class FieldProbe extends Traverser<Map<IRI, Shape>> {

		@Override public Map<IRI, Shape> probe(final Shape shape) { return Maps.map();}


		@Override public Map<IRI, Shape> probe(final Field field) {
			return singletonMap(field.getIRI(), field.getShape());
		}


		@Override public Map<IRI, Shape> probe(final And and) {
			return fields(and.getShapes().stream());
		}

		@Override public Map<IRI, Shape> probe(final Or or) {
			return fields(or.getShapes().stream());
		}

		@Override public Map<IRI, Shape> probe(final When when) {
			return fields(Stream.of(when.getPass(), when.getFail()));
		}


		private Map<IRI, Shape> fields(final Stream<Shape> stream) {
			return stream

					// collect iri-to-shape mappings from nested shapes

					.flatMap(shape -> shape.map(this).entrySet().stream())

					// group by iri, collect to a set of shapes and convert to an optimized conjunction

					.collect(groupingBy(Map.Entry::getKey, LinkedHashMap::new, mapping(Map.Entry::getValue,
							collectingAndThen(toCollection(LinkedHashSet::new), set ->
									set.size() == 1 ? set.iterator().next() : and(set)))));
		}

	}

}