/*
 * Copyright © 2020 Metreeca srl. All rights reserved.
 */


package com.metreeca.rdf;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static com.metreeca.rdf.Path.direct;
import static com.metreeca.rdf.Path.union;
import static com.metreeca.rdf.Values.*;
import static java.util.Collections.emptySet;
import static java.util.Collections.unmodifiableCollection;
import static java.util.Objects.requireNonNull;


public final class Cell implements Resource {

	private static final long serialVersionUID=3100418201450076593L;

	private static final IRI SchemaName=iri("http://schema.org/", "name");
	private static final IRI SchemaDescription=iri("http://schema.org/", "description");

	private static final Path label=union(RDFS.LABEL, DC.TITLE, SchemaName);
	private static final Path notes=union(RDFS.COMMENT, DC.DESCRIPTION, SchemaDescription);


	public static Builder cell(final Resource focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return new Builder(focus, emptySet());
	}

	public static Builder cell(final Cell cell) {

		if ( cell == null ) {
			throw new NullPointerException("null cell");
		}

		return new Builder(cell.focus, cell.model);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Resource focus;
	private final Collection<Statement> model;


	private Cell(final Resource focus, final Collection<Statement> model) {
		this.focus=focus;
		this.model=unmodifiableCollection(model);
	}


	public Resource focus() {
		return focus;
	}

	public Collection<Statement> model() {
		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<String> label() {
		return string(label);
	}

	public Optional<String> notes() {
		return string(notes);
	}


	public Optional<String> string(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return string(direct(predicate));
	}

	public Optional<String> string(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return value(path).flatMap(Values::string);
	}


	public Stream<String> strings(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return strings(direct(predicate));
	}

	public Stream<String> strings(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return values(path).map(v -> Values.string(v).orElse(null)).filter(Objects::nonNull);
	}


	public Optional<BigInteger> integer(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return value(predicate).flatMap(Values::integer);
	}

	public Optional<BigDecimal> decimal(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return value(predicate).flatMap(Values::decimal);
	}


	public Optional<Value> value(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return value(direct(predicate));
	}

	public Optional<Value> value(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return values(path).findFirst();
	}


	public Stream<Value> values(final IRI predicate) {
		return values(direct(predicate));
	}

	public Stream<Value> values(final Path path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return path.follow(focus, model);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String stringValue() {
		return focus.stringValue();
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Cell
				&& focus.equals(((Cell)object).focus)
				&& model.equals(((Cell)object).model);
	}

	@Override public int hashCode() {
		return focus.hashCode()
				^model.hashCode();
	}

	@Override public String toString() {
		return focus
				+label().map(l -> " : "+l).orElse("")
				+notes().map(l -> " / "+l).orElse("");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Builder implements Supplier<Cell> {

		private final Resource focus;
		private final Collection<Statement> model;


		private Builder(final Resource focus, final Collection<Statement> model) {
			this.focus=focus;
			this.model=new LinkedHashSet<>(model);
		}


		@Override public Cell get() {
			return new Cell(focus, model);
		}


		public Builder insert(final Iterable<Statement> model) {

			if ( model == null ) {
				throw new NullPointerException("null model");
			}

			for (final Statement statement : model) {
				this.model.add(requireNonNull(statement, "null model statement"));
			}

			return this;
		}

		public Builder insert(final Stream<Statement> model) {

			if ( model == null ) {
				throw new NullPointerException("null model");
			}

			model
					.map(statement -> requireNonNull(statement, "null model statement"))
					.forEachOrdered(this.model::add);

			return this;
		}


		public Builder insert(final IRI predicate, final Value object) {

			if ( predicate == null ) {
				throw new NullPointerException("null predicate");
			}

			if ( object instanceof Cell ) {

				final Cell cell=(Cell)object;

				model.add(Values.direct(predicate)
						? statement(focus, predicate, cell.focus)
						: statement(cell.focus, inverse(predicate), focus)
				);

				model.addAll(cell.model);

			} else if ( object != null ) {

				if ( Values.direct(predicate) ) {

					model.add(statement(focus, predicate, object));

				} else if ( object instanceof Resource ) {

					model.add(statement((Resource)object, inverse(predicate), focus));

				}

			}

			return this;
		}

		public Builder insert(final IRI predicate, final Value... objects) {

			if ( predicate == null ) {
				throw new NullPointerException("null predicate");
			}

			if ( objects != null ) {
				for (final Value object : objects) {
					insert(predicate, object);
				}
			}

			return this;
		}

		public Builder insert(final IRI predicate, final Iterable<? extends Value> objects) {

			if ( predicate == null ) {
				throw new NullPointerException("null predicate");
			}

			if ( objects != null ) {
				for (final Value object : objects) {
					insert(predicate, object);
				}
			}

			return this;
		}

		public Builder insert(final IRI predicate, final Optional<? extends Value> object) {

			if ( predicate == null ) {
				throw new NullPointerException("null predicate");
			}

			if ( object != null ) {
				object.ifPresent(value -> insert(predicate, value));
			}

			return this;
		}

		public Builder insert(final IRI predicate, final Stream<? extends Value> objects) {

			if ( predicate == null ) {
				throw new NullPointerException("null predicate");
			}

			if ( objects != null ) {
				objects.forEach(value -> insert(predicate, value));
			}

			return this;
		}

	}

}
