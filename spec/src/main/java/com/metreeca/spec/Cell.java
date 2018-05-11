/*
 * Copyright © 2013-2018 Metreeca srl. All rights reserved.
 *
 * This file is part of Metreeca.
 *
 * Metreeca is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published
 * by the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Metreeca is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with Metreeca. If not, see <http://www.gnu.org/licenses/>.
 */

package com.metreeca.spec;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.util.Literals;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Optional;
import java.util.function.Supplier;

import static com.metreeca.spec.Values.statement;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.unmodifiableCollection;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;


public final class Cell { // !!! immutable

	public static final Supplier<IllegalStateException> Missing=() -> new IllegalStateException("missing cell value");


	public static Cell cell(final Value... values) {
		return cell(new LinkedHashSet<>(asList(values)));
	}

	public static Cell cell(final Collection<Value> values) {
		return cell(new LinkedHashSet<>(), values);
	}

	public static Cell cell(final Collection<Statement> model, final Value... values) {
		return cell(model, new LinkedHashSet<>(asList(values)));
	}

	public static Cell cell(final Collection<Statement> model, final Collection<Value> values) {

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		if ( model.contains(null) ) {
			throw new NullPointerException("null model statement");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		if ( values.contains(null) ) {
			throw new NullPointerException("null value");
		}

		return new Cell(values, model, new Context() {});
	}


	private final Collection<Value> values;
	private final Collection<Statement> model;

	private final Context context;


	private Cell(final Collection<Value> values, final Collection<Statement> model, final Context context) {

		this.values=values;
		this.model=model;

		this.context=context;
	}


	public Collection<Cell> cells() {
		return values.stream().map(value -> new Cell(singleton(value), model, context)).collect(toList());
	}


	public Optional<Value> value() {
		return values.stream().findFirst();
	}

	public Collection<Value> values() {
		return unmodifiableCollection(values);
	}

	public Collection<Statement> model() {
		return unmodifiableCollection(model);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<String> string() {
		return value()
				.filter(v -> v instanceof Literal)
				.map(v -> Literals.getLabel(v, ""));
	}

	public Optional<Boolean> bool() {
		return value()
				.filter(v -> v instanceof Literal)
				.map(v -> Literals.getBooleanValue(v, false));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Cell follow(final IRI predicate, final boolean reverse) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return reverse ? reverse(predicate) : forward(predicate);
	}

	public Cell forward(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		final Collection<Value> source=values;
		final Collection<Value> target=model.stream()
				.filter(statement -> statement.getPredicate().equals(predicate) && source.contains(statement.getSubject()))
				.map(Statement::getObject)
				.collect(toCollection(LinkedHashSet::new));

		return new Cell(target, model, new Context() {

			@Override public void insert(final Value value) {
				source.forEach(subject -> {
					if ( subject instanceof Resource ) { model.add(statement((Resource)subject, predicate, value)); }
				});
			}

			@Override public void remove(final Value value) {
				source.forEach(subject -> {
					if ( subject instanceof Resource ) { model.remove(statement((Resource)subject, predicate, value)); }
				});
			}

		});
	}

	public Cell reverse(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		final Collection<Value> source=values;
		final Collection<Value> target=model.stream()
				.filter(statement -> statement.getPredicate().equals(predicate) && source.contains(statement.getObject()))
				.map(Statement::getSubject)
				.collect(toCollection(LinkedHashSet::new));

		return new Cell(target, model, new Context() {

			@Override public void insert(final Value value) {
				source.forEach(object -> {
					if ( value instanceof Resource ) { model.add(statement((Resource)value, predicate, object)); }
				});
			}

			@Override public void remove(final Value value) {
				source.forEach(object -> {
					if ( value instanceof Resource ) { model.remove(statement((Resource)value, predicate, object)); }
				});
			}

		});
	}


	public Cell insert(final Value... values) {
		return insert(asList(values));
	}

	public Cell insert(final Iterable<Value> values) {

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		values.forEach(value -> {
			if ( this.values.add(value) ) { context.insert(value); }
		});

		return this;
	}


	public Cell remove(final Value... values) {
		return remove(asList(values));
	}

	public Cell remove(final Iterable<Value> values) {

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		values.forEach(value -> {
			if ( this.values.remove(value) ) { context.remove(value); }
		});

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Cell
				&& values.equals(((Cell)object).values)
				&& model.equals(((Cell)object).model);
	}

	@Override public int hashCode() {
		return values.hashCode()^model.hashCode();
	}

	@Override public String toString() {
		return values.stream().map(Values::format).collect(joining(", ", "[", "]"))+" {"
				+(values.isEmpty() ? "" : model.stream()
				.filter(edge -> values.contains(edge.getSubject()) || values.contains(edge.getObject()))
				.map(Values::format)
				.collect(joining("\n\t", "\n\t", "\n")))
				+"}";
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static interface Context {

		public default void insert(final Value value) {}

		public default void remove(final Value value) {}

	}

}
