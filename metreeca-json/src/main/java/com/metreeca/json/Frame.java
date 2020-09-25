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

package com.metreeca.json;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.impl.SimpleIRI;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.json.Values.iri;
import static com.metreeca.json.Values.statement;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

/**
 * Graph frame.
 *
 * <p>Describes a graph frame centered on a focus IRI.</p>
 */
public final class Frame implements Resource {

	private static final long serialVersionUID=-498626125890617199L;

	private static final IRI SchemaName=iri("http://schema.org/", "name");
	private static final IRI SchemaDescription=iri("http://schema.org/", "description");

	private static final Function<Frame, Stream<Value>> Labels=alt(RDFS.LABEL, DC.TITLE, SchemaName);
	private static final Function<Frame, Stream<Value>> Notes=alt(RDFS.COMMENT, DC.DESCRIPTION, SchemaDescription);


	public static Frame frame(final Resource focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return new Frame(focus, emptyMap());
	}

	public static Frame frame(final Resource focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return (Frame)value(focus, model, newSetFromMap(new ConcurrentHashMap<>()));
	}


	private static Value value(final Value focus, final Collection<Statement> model, final Collection<Value> trail) {
		return focus instanceof Resource && trail.add(focus) ? new Frame((Resource)focus, model.stream()

				.filter(Objects::nonNull)

				.map(statement -> {

					final Resource subject=statement.getSubject();
					final IRI predicate=statement.getPredicate();
					final Value object=statement.getObject();

					if ( subject.equals(focus) ) {

						return new SimpleImmutableEntry<>(predicate, value(object, model, trail));

					} else if ( object.equals(focus) ) {

						return new SimpleImmutableEntry<>(inv(predicate), value(subject, model, trail));

					} else {

						return null;

					}

				})

				.filter(Objects::nonNull)

				.collect(groupingBy(Map.Entry::getKey, LinkedHashMap::new,
						mapping(Map.Entry::getValue, toCollection(LinkedHashSet::new))
				))

		) : focus;
	}


	//// Paths /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static IRI inv(final IRI iri) {

		if ( iri == null ) {
			throw new NullPointerException("null iri");
		}

		return new Inverse(iri);
	}


	public static Function<Frame, Stream<Value>> seq(final IRI path) {
		return frame -> frame.fields.getOrDefault(path, emptySet()).stream();
	}

	public static Function<Frame, Stream<Value>> seq(final IRI... path) {
		return seq(Arrays.stream(path).map(Frame::seq).collect(toList()));
	}

	public static Function<Frame, Stream<Value>> seq(final Iterable<Function<Frame, Stream<Value>>> paths) {
		return frame -> {

			Stream<Value> values=Stream.of(frame);

			for (final Function<Frame, Stream<Value>> path : paths) {
				values=values.filter(Frame.class::isInstance).map(Frame.class::cast).flatMap(path);
			}

			return values;
		};
	}


	public static Function<Frame, Stream<Value>> alt(final IRI... paths) {
		return alt(Arrays.stream(paths).map(Frame::seq).collect(toList()));
	}

	public static Function<Frame, Stream<Value>> alt(final Collection<Function<Frame, Stream<Value>>> paths) {
		return frame -> paths.stream().flatMap(step -> step.apply(frame));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Resource focus;
	private final Map<IRI, Set<Value>> fields;


	private Frame(final Resource focus, final Map<IRI, Set<Value>> fields) {
		this.focus=focus;
		this.fields=fields;
	}


	public Resource focus() {
		return focus;
	}


	public String label() {
		return get(Labels).findFirst().flatMap(Values::string).orElse("");
	}

	public String notes() {
		return get(Notes).findFirst().flatMap(Values::string).orElse("");
	}


	public Collection<Statement> model() {
		return stream().collect(toCollection(LinkedHashSet::new));
	}

	public Stream<Statement> stream() {
		return fields.entrySet().stream().flatMap(entry -> {

			final IRI key=entry.getKey();
			final IRI predicate=iri(key.getNamespace(), key.getLocalName());

			final boolean inverse=key instanceof Inverse;

			return entry.getValue().stream().flatMap(value -> {

				final boolean frame=value instanceof Frame;

				final Statement statement=inverse
						? statement(frame ? ((Frame)value).focus : (Resource)value, predicate, focus)
						: statement(focus, predicate, frame ? ((Frame)value).focus : value);

				return Stream.concat(Stream.of(statement), frame ? ((Frame)value).stream() : Stream.empty());

			});

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Stream<Value> get(final IRI field) {

		if ( field == null ) {
			throw new NullPointerException("null field IRI");
		}

		return get(seq(field));
	}

	public Stream<Value> get(final Function<Frame, Stream<Value>> getter) {

		if ( getter == null ) {
			throw new NullPointerException("null getter");
		}

		return Objects.requireNonNull(getter.apply(this), "null getter return value")
				.map(value -> value instanceof Frame ? ((Frame)value).focus : value);
	}


	public Frame set(final IRI field, final Value... values) {
		return set(field, Arrays.stream(values));
	}

	public Frame set(final IRI field, final Optional<? extends Value> value) {
		return set(field, value.map(Stream::of).orElseGet(Stream::empty));
	}

	public Frame set(final IRI field, final Collection<? extends Value> values) {
		return set(field, values.stream());
	}

	public Frame set(final IRI field, final Stream<? extends Value> values) {

		if ( field == null ) {
			throw new NullPointerException("null field");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		final Map<IRI, Set<Value>> fields=new LinkedHashMap<>(this.fields);
		final Set<Value> update=new LinkedHashSet<>();

		values.forEachOrdered(value -> {

			if ( value == null ) {
				throw new NullPointerException("null values");
			}

			if ( field instanceof Inverse && !(value instanceof Resource) ) {
				throw new IllegalArgumentException("literal values for inverse field");
			}

			update.add(value);

		});

		fields.put(field, update);

		return new Frame(focus, fields);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String stringValue() {
		return focus.stringValue();
	}


	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Resource && focus.equals(object);
	}

	@Override public int hashCode() {
		return focus.hashCode();
	}

	@Override public String toString() {
		return focus
				+get(Labels).findFirst().map(l -> " : "+l).orElse("")
				+get(Notes).findFirst().map(l -> " / "+l).orElse("");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static final class Inverse extends SimpleIRI {

		private static final long serialVersionUID=7576383707001017160L;


		private Inverse(final IRI iri) { super(iri.stringValue()); }


		@Override public boolean equals(final Object object) {
			return object == this || object instanceof Inverse && super.equals(object);
		}

		@Override public int hashCode() { return -super.hashCode(); }

		@Override public String toString() {
			return "^"+super.toString();
		}

	}

}
