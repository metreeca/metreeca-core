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

package com.metreeca.json.shapes;

import com.metreeca.json.Shape;

import org.eclipse.rdf4j.model.IRI;
import org.eclipse.rdf4j.model.Value;

import java.util.Arrays;
import java.util.Objects;
import java.util.stream.Stream;

import static com.metreeca.json.Values.format;
import static com.metreeca.json.Values.internal;
import static com.metreeca.json.shapes.All.all;
import static com.metreeca.json.shapes.And.and;
import static com.metreeca.json.shapes.Or.or;


/**
 * Field structural constraint.
 *
 * <p>States that the derived focus set generated by following a single step path is consistent with a given {@link
 * Shape shape}.</p>
 */
public final class Field extends Shape {

	public static Shape field(final String name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return field(name, all());
	}

	public static Shape field(final String name, final Object... values) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return field(name, all(values));
	}

	public static Shape field(final String name, final Shape... shapes) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null shapes");
		}

		return field(name, and(shapes));
	}

	public static Shape field(final String name, final Shape shape) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return field(internal(name), shape);
	}


	public static Shape field(final IRI name) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		return field(name, all());
	}

	public static Shape field(final IRI name, final Value... values) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		return field(name, all(values));
	}

	public static Shape field(final IRI name, final Shape... shapes) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( shapes == null || Arrays.stream(shapes).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null shapes");
		}

		return field(name, and(shapes));
	}

	public static Shape field(final IRI name, final Shape shape) {

		if ( name == null ) {
			throw new NullPointerException("null name");
		}

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.equals(or()) ? and() : new Field(name, shape);
	}


	public static Stream<Field> fields(final Shape shape) {

		if ( shape == null ) {
			throw new NullPointerException("null shape");
		}

		return shape.map(new FieldsProbe());
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final IRI name;
	private final Shape shape;


	private Field(final IRI name, final Shape shape) {
		this.name=name;
		this.shape=shape;
	}


	public IRI name() {
		return name;
	}

	public Shape shape() {
		return shape;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public <V> V map(final Probe<V> probe) {

		if ( probe == null ) {
			throw new NullPointerException("null probe");
		}

		return probe.probe(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Field
				&& name.equals(((Field)object).name)
				&& shape.equals(((Field)object).shape);
	}

	@Override public int hashCode() {
		return name.hashCode()^shape.hashCode();
	}

	@Override public String toString() {
		return "field("+format(name)+(shape.equals(and()) ? "" : ", "+shape)+")";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class FieldsProbe extends Probe<Stream<Field>> {

		@Override public Stream<Field> probe(final Field field) {
			return Stream.of(field);
		}

		@Override public Stream<Field> probe(final And and) {
			return and.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Field> probe(final Or or) {
			return or.shapes().stream().flatMap(shape -> shape.map(this));
		}

		@Override public Stream<Field> probe(final When when) {
			return Stream.of(when.pass(), when.fail()).flatMap(this);
		}

		@Override public Stream<Field> probe(final Shape shape) {
			return Stream.empty();
		}

	}

}
