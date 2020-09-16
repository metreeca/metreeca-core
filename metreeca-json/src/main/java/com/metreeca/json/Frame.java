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
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.AbstractMap.SimpleImmutableEntry;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static java.util.stream.Collectors.*;

/**
 * Graph frame value.
 *
 * <p>Describes a graph frame centered on a focus IRI.</p>
 */
public final class Frame implements Resource {

	private static final long serialVersionUID=-498626125890617199L;

	private static final IRI SchemaName=iri("http://schema.org/", "name");
	private static final IRI SchemaDescription=iri("http://schema.org/", "description");

	private static final Function<Frame, Group> Labels=alt(RDFS.LABEL, DC.TITLE, SchemaName);
	private static final Function<Frame, Group> Notes=alt(RDFS.COMMENT, DC.DESCRIPTION, SchemaDescription);


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

						return new SimpleImmutableEntry<>(inverse(predicate), value(subject, model, trail));

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

	public static Function<Frame, Group> seq(final IRI path) {
		return frame -> frame.get(path);
	}

	public static Function<Frame, Group> seq(final IRI... path) {
		return seq(Arrays.stream(path).map(Frame::seq).collect(toList()));
	}

	public static Function<Frame, Group> seq(final Collection<Function<Frame, Group>> paths) {
		return frame -> {

			Set<Value> values=singleton(frame);

			for (final Iterator<Function<Frame, Group>> steps=paths.iterator(); steps.hasNext(); ) {

				final Function<Frame, Group> step=steps.next();

				values=values.stream()

						.filter(Frame.class::isInstance)
						.map(Frame.class::cast)

						.flatMap(value -> {
							return step.apply(value).values();
						})

						.collect(toCollection(LinkedHashSet::new));
			}

			return new Group(values);
		};
	}


	public static Function<Frame, Group> alt(final IRI... paths) {
		return alt(Arrays.stream(paths).map(Frame::seq).collect(toList()));
	}

	public static Function<Frame, Group> alt(final Collection<Function<Frame, Group>> paths) {
		return frame -> new Group(paths.stream()
				.flatMap(step -> step.apply(frame).values())
				.collect(toCollection(LinkedHashSet::new))
		);
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
		return get(Labels).string().orElse("");
	}

	public String notes() {
		return get(Notes).string().orElse("");
	}


	public Collection<Statement> model() {
		return stream().collect(toCollection(LinkedHashSet::new));
	}

	public Stream<Statement> stream() {
		return fields.entrySet().stream().flatMap(entry -> {

			final IRI predicate=entry.getKey();
			final IRI inverse=inverse(predicate);

			final boolean direct=direct(predicate);

			return entry.getValue().stream().flatMap(value -> {

				final boolean frame=value instanceof Frame;

				final Statement statement=direct
						? statement(focus, predicate, frame ? ((Frame)value).focus : value)
						: statement(frame ? ((Frame)value).focus : (Resource)value, inverse, focus);

				return Stream.concat(Stream.of(statement), frame ? ((Frame)value).stream() : Stream.empty());

			});

		});
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Group get(final IRI field) {

		if ( field == null ) {
			throw new NullPointerException("null field IRI");
		}

		return new Group(fields.getOrDefault(field, emptySet()));
	}

	public Group get(final Function<Frame, Group> getter) {

		if ( getter == null ) {
			throw new NullPointerException("null getter");
		}

		return Objects.requireNonNull(getter.apply(this), "null getter return value");
	}


	public Frame set(final IRI field, final Value... values) {
		return set(field, asList(values));
	}

	public Frame set(final IRI field, final List<Value> values) {

		if ( field == null ) {
			throw new NullPointerException("null field");
		}

		if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null values");
		}

		if ( !direct(field) && values.stream().anyMatch(v -> !(v instanceof Resource)) ) {
			throw new IllegalArgumentException("literal values for inverse field");
		}

		final Map<IRI, Set<Value>> map=new LinkedHashMap<>(fields);

		map.put(field, new LinkedHashSet<>(values));

		return new Frame(focus, map);
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
				+get(Labels).value().map(l -> " : "+l).orElse("")
				+get(Notes).value().map(l -> " / "+l).orElse("");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Group {

		private final Set<Value> values;


		private Group(final Set<Value> values) {
			this.values=values;
		}


		public Optional<Boolean> bool() { return value(Values::bool); }


		public Optional<BigInteger> integer() { return value(Values::integer); }

		public Stream<BigInteger> integers() { return values(Values::integer); }


		public Optional<BigDecimal> decimal() { return value(Values::decimal); }

		public Stream<BigDecimal> decimals() { return values(Values::decimal); }


		public Optional<String> string() { return value(Values::string); }

		public Stream<String> strings() { return values(Values::string); }


		public <V> Optional<V> value(final Function<Value, Optional<V>> mapper) {
			return value().flatMap(mapper);
		}

		public <V> Stream<V> values(final Function<Value, Optional<V>> mapper) {
			return values().map(mapper).filter(Optional::isPresent).map(Optional::get);
		}


		public Optional<Value> value() {
			return values().findFirst();
		}

		public Stream<Value> values() {
			return values.stream().map(value -> value instanceof Frame ? ((Frame)value).focus : value);
		}

	}

}
