/*
 * Copyright © 2013-2020 Metreeca srl. All rights reserved.
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


package com.metreeca.rdf;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.LinkedHashModel;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.Optional;
import java.util.stream.Stream;

import static com.metreeca.rdf.Path.direct;
import static com.metreeca.rdf.Path.union;
import static com.metreeca.rdf.Values.iri;


public final class Cell implements Resource {

	private static final long serialVersionUID=3100418201450076593L;

	private static final IRI SchemaName = iri("http://schema.org/", "name");
	private static final IRI SchemaDescription = iri("http://schema.org/", "description");

	private static final Path label=union(RDFS.LABEL, DC.TITLE, SchemaName);
	private static final Path notes=union(RDFS.COMMENT, DC.DESCRIPTION, SchemaDescription);


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Resource focus;
	private final Model model;


	public Cell(final Resource focus) {

		if ( focus == null ) {
			throw new NullPointerException("null subject");
		}

		this.focus=focus;
		this.model=new LinkedHashModel();
	}

	public Cell(final Resource focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null subject");
		}

		this.focus=focus;
		this.model=new LinkedHashModel(model);
	}


	public Resource focus() {
		return focus;
	}

	public Model model() {
		return model.unmodifiable();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public boolean contains(final IRI predicate, final Value object) {

		if (predicate == null) {
			throw new NullPointerException("null predicate");
		}

		if (object == null) {
			throw new NullPointerException("null object");
		}

		return model.parallelStream().anyMatch(statement
				-> statement.getPredicate().equals(predicate)
				&& statement.getObject().equals(object)
		);
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

	public Cell insert(final IRI predicate, final Value object) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( object instanceof Cell ) {

			model.add(focus, predicate, ((Cell)object).focus);
			model.addAll(((Cell)object).model);

		} else if ( object != null ) {

			model.add(focus, predicate, object);

		}

		return this;
	}

	public Cell insert(final IRI predicate, final Value... objects) {

		if ( predicate != null && objects != null ) {
			for (final Value object : objects) {
				insert(predicate, object);
			}
		}

		return this;
	}

	public Cell insert(final IRI predicate, final Iterable<? extends Value> objects) {

		if ( predicate != null && objects != null ) {
			for (final Value object : objects) {
				insert(predicate, object);
			}
		}

		return this;
	}

	public Cell insert(final IRI predicate, final Optional<? extends Value> object) {

		if ( predicate != null && object != null ) {
			object.ifPresent(value -> insert(predicate, value));
		}

		return this;
	}

	public Cell insert(final IRI predicate, final Stream<? extends Value> objects) {

		if ( predicate != null && objects != null ) {
			objects.forEach(value -> insert(predicate, value));
		}

		return this;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String stringValue() {
		return focus.stringValue();
	}

	@Override public String toString() {
		return focus
				+string(label).map(l -> " : "+l).orElse("")
				+string(notes).map(l -> " / "+l).orElse("");
	}

}
