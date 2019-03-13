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

import com.metreeca.form.Form;
import com.metreeca.form.Shape;
import com.metreeca.form.probes.Traverser;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Stream;

import static com.metreeca.form.things.Maps.entry;
import static com.metreeca.form.things.Values.format;
import static com.metreeca.form.things.Values.literal;

import static java.util.Collections.emptyMap;
import static java.util.stream.Collectors.toMap;


/**
 * Non-validating annotation constraint.
 *
 * <p>States that the enclosing shape has a given value for an annotation property.</p>
 */
public final class Meta implements Shape {

	public static Meta alias(final String value) {
		return new Meta(Form.Alias, literal(value));
	}

	public static Meta label(final String value) {
		return new Meta(Form.Label, literal(value));
	}

	public static Meta notes(final String value) {
		return new Meta(Form.Notes, literal(value));
	}

	public static Meta placeholder(final String value) {
		return new Meta(Form.Placeholder, literal(value));
	}

	public static Meta dflt(final Value value) {
		return new Meta(Form.Default, value);
	}

	public static Meta hint(final IRI value) {
		return new Meta(Form.Hint, value);
	}

	public static Meta group(final Value value) {
		return new Meta(Form.Group, value);
	}


	public static Meta meta(final IRI label, final Value value) {
		return new Meta(label, value);
	}


	public static Map<IRI, Value> metas(final Shape shape) {
		return shape == null ? emptyMap() : shape.map(new MetaProbe()).collect(toMap(
				Map.Entry::getKey,
				Map.Entry::getValue,
				(x, y) -> Objects.equals(x, y) ? x : null
		));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI iri;
	private final Value value;


	private Meta(final IRI iri, final Value value) {

		if ( iri == null ) {
			throw new NullPointerException("null IRI");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		this.iri=iri;
		this.value=value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public IRI getIRI() {
		return iri;
	}

	public Value getValue() {
		return value;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <T> T map(final Probe<T> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Meta
				&& iri.equals(((Meta)object).iri)
				&& value.equals(((Meta)object).value);
	}

	@Override public int hashCode() {
		return iri.hashCode()^value.hashCode();
	}

	@Override public String toString() {
		return "meta("+format(iri)+"="+format(value)+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class MetaProbe extends Traverser<Stream<Map.Entry<IRI, Value>>> {

		@Override public Stream<Map.Entry<IRI, Value>> probe(final Shape shape) {
			return Stream.empty();
		}


		@Override public Stream<Map.Entry<IRI, Value>> probe(final Meta meta) {
			return Stream.of(entry(meta.getIRI(), meta.getValue()));
		}


		@Override public Stream<Map.Entry<IRI, Value>> probe(final Field field) {
			return Stream.empty();
		}


		@Override public Stream<Map.Entry<IRI, Value>> probe(final And and) {
			return and.getShapes().stream().flatMap(s -> s.map(this));
		}

		@Override public Stream<Map.Entry<IRI, Value>> probe(final Or or) {
			return or.getShapes().stream().flatMap(s -> s.map(this));
		}

		@Override public Stream<Map.Entry<IRI, Value>> probe(final When when) {
			return Stream.of(when.getPass(), when.getFail()).flatMap(s -> s.map(this));
		}

	}

}
