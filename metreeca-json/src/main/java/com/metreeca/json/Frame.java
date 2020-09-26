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

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;
import static java.util.Collections.emptySet;
import static java.util.Collections.newSetFromMap;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

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

		return new Frame(focus);
	}

	public static Frame frame(final Resource focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null ) {
			throw new NullPointerException("null model");
		}

		return frame(focus, model, new ConcurrentHashMap<>());
	}


	private static Frame frame(
			final Resource focus, final Collection<Statement> model, final Map<Resource, Frame> trail
	) {
		return Optional.ofNullable(trail.get(focus)).orElseGet(() -> {

			final Frame frame=new Frame(focus);

			trail.put(focus, frame); // insert empty placeholder before scanning statements

			model.stream().filter(Objects::nonNull).forEachOrdered(statement -> {

				final Resource subject=statement.getSubject();
				final IRI predicate=statement.getPredicate();
				final Value object=statement.getObject();

				if ( subject.equals(focus) ) {

					frame.fields.compute(predicate, (iri, values) -> {

						final Set<Value> set=(values != null) ? values : new LinkedHashSet<>();

						set.add(object instanceof Resource ? frame((Resource)object, model, trail) : object);

						return set;

					});

				} else if ( object.equals(focus) ) {

					frame.fields.compute(inverse(predicate), (iri, values) -> {

						final Set<Value> set=(values != null) ? values : new LinkedHashSet<>();

						set.add(frame(subject, model, trail));

						return set;

					});

				}

			});

			return frame;

		});
	}


	//// Paths /////////////////////////////////////////////////////////////////////////////////////////////////////////


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

	private final Map<IRI, Set<Value>> fields=new LinkedHashMap<>();


	private Frame(final Resource focus) {
		this.focus=focus;
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
		return stream(newSetFromMap(new ConcurrentHashMap<>()));
	}


	private Stream<Statement> stream(final Set<Value> trail) {
		return trail.add(focus) ? fields.entrySet().stream().flatMap(entry -> {

			if ( direct(entry.getKey()) ) {

				final IRI predicate=entry.getKey();

				return entry.getValue().stream().flatMap(value -> {

					final boolean frame=value instanceof Frame;

					return Stream.concat(
							Stream.of(statement(focus, predicate, frame ? ((Frame)value).focus : value)),
							frame ? ((Frame)value).stream(trail) : Stream.empty()
					);
				});

			} else {

				final IRI predicate=inverse(entry.getKey());

				return entry.getValue().stream().flatMap(value -> {

					final boolean frame=value instanceof Frame;

					return Stream.concat(
							Stream.of(statement(frame ? ((Frame)value).focus : (Resource)value, predicate, focus)),
							frame ? ((Frame)value).stream(trail) : Stream.empty());
				});

			}

		}) : Stream.empty();
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

		final boolean direct=direct(field);

		final Frame frame=new Frame(focus);

		frame.fields.putAll(fields);

		frame.fields.put(field, values.peek(value -> {

			if ( value == null ) {
				throw new NullPointerException("null values");
			}

			if ( !(direct || value instanceof Resource) ) {
				throw new IllegalArgumentException("literal values for inverse field");
			}

		}).collect(toCollection(LinkedHashSet::new)));

		return frame;
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

		private Inverse(final String iri) { super(iri); }

	}

}
