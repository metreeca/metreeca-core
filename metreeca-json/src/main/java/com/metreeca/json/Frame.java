/*
 * Copyright © 2013-2021 Metreeca srl
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

package com.metreeca.json;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

/**
 * Graph frame.
 *
 * <p>Describes a subgraph centered on a set of focus values.</p>
 */
public final class Frame {

	/**
	 * An IRI scheme for inverse predicates ({@value}).
	 */
	private static final String InverseScheme="inverse:";

	private static final IRI SchemaName=iri("http://schema.org/", "name");
	private static final IRI SchemaDescription=iri("http://schema.org/", "description");

	private static final BiFunction<Value, Collection<Statement>, Stream<Value>> Labels=alt(
			RDFS.LABEL, DC.TITLE, SchemaName
	);

	private static final BiFunction<Value, Collection<Statement>, Stream<Value>> Notes=alt(
			RDFS.COMMENT, DC.DESCRIPTION, SchemaDescription
	);


	public static Frame frame(final Value focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return new Frame(focus, emptySet());
	}

	public static Frame frame(final Value focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null || model.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null model or model statement");
		}

		final Set<Statement> statements=new LinkedHashSet<>();

		final Collection<Value> visited=new HashSet<>();
		final Queue<Value> pending=new ArrayDeque<>(singleton(focus));

		while ( !pending.isEmpty() ) {

			final Value value=pending.poll();

			if ( visited.add(value) ) {
				model.forEach(s -> {
					if ( value.equals(s.getSubject()) || value.equals(s.getObject()) ) {

						pending.add(s.getSubject());
						pending.add(s.getPredicate());
						pending.add(s.getObject());

						statements.add(s);
					}
				});
			}
		}

		return new Frame(focus, statements);
	}


	//// Paths /////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static BiFunction<Value, Collection<Statement>, Stream<Value>> seq(final IRI path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return traverse(path,

				direct -> (focus, model) -> model.stream()
						.filter(s -> focus.equals(s.getSubject()) && direct.equals(s.getPredicate()))
						.map(Statement::getObject),

				inverse -> (focus, model) -> model.stream()
						.filter(s -> inverse.equals(s.getPredicate()) && focus.equals(s.getObject()))
						.map(Statement::getSubject)

		);
	}

	public static BiFunction<Value, Collection<Statement>, Stream<Value>> seq(final IRI... path) {

		if ( path == null || Arrays.stream(path).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null path");
		}

		return seq(Arrays.stream(path).map(Frame::seq).collect(toList()));
	}

	public static BiFunction<Value, Collection<Statement>, Stream<Value>> seq(
			final Collection<BiFunction<Value, Collection<Statement>, Stream<Value>>> paths) {

		if ( paths == null || paths.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return (focus, model) -> {

			Stream<Value> values=Stream.of(focus);

			for (final BiFunction<Value, Collection<Statement>, Stream<Value>> path : paths) {
				values=values.flatMap(value -> path.apply(value, model));
			}

			return values;
		};
	}


	public static BiFunction<Value, Collection<Statement>, Stream<Value>> alt(final IRI... paths) {

		if ( paths == null || Arrays.stream(paths).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return alt(Arrays.stream(paths).map(Frame::seq).collect(toList()));
	}

	public static BiFunction<Value, Collection<Statement>, Stream<Value>> alt(
			final Collection<BiFunction<Value, Collection<Statement>, Stream<Value>>> paths) {

		if ( paths == null || paths.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null paths");
		}

		return (focus, model) -> paths.stream().flatMap(path -> path.apply(focus, model));
	}


	//// Inverse Predicates ////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Checks predicate direction.
	 *
	 * @param predicate the IRI identifying the predicate
	 *
	 * @return {@code true} if {@code predicate} identifies a direct predicate; {@code false} if {@code predicate}
	 * identifies an {@link #inverse(IRI) inverse} predicate
	 *
	 * @throws NullPointerException if {@code predicate} is null
	 */
	public static boolean direct(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return !predicate.stringValue().startsWith(InverseScheme);
	}

	/**
	 * Inverts the direction of a predicate.
	 *
	 * @param predicate the IRI identifying the predicate
	 *
	 * @return the inverse version of {@code predicate}
	 *
	 * @throws NullPointerException if {@code predicate} is null
	 */
	public static IRI inverse(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		final String label=predicate.stringValue();

		return label.startsWith(InverseScheme)
				? iri(label.substring(InverseScheme.length()))
				: iri(InverseScheme+label);
	}


	/**
	 * Traverses a predicate.
	 *
	 * @param predicate the IRI identifying the predicate to be traversed
	 * @param direct    a predicate mapper to be executed if {@code predicate} is {@link #direct(IRI) direct}
	 * @param inverse   a predicate mapper to be executed if {@code predicate} is {@link #inverse(IRI) inverse}
	 * @param <V>       the type of the value returned by predicate mappers
	 *
	 * @return the value returned by the predicate mapper selected according to the direction of {@code predicate}
	 *
	 * @throws NullPointerException if any argument is null
	 */
	public static <V> V traverse(final IRI predicate, final Function<IRI, V> direct, final Function<IRI, V> inverse) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( direct == null ) {
			throw new NullPointerException("null direct");
		}

		if ( inverse == null ) {
			throw new NullPointerException("null inverse");
		}

		return predicate.stringValue().startsWith(InverseScheme)
				? inverse.apply(iri(predicate.stringValue().substring(InverseScheme.length())))
				: direct.apply(predicate);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Value focus;
	private final Set<Statement> model;


	private Frame(final Value focus, final Set<Statement> model) {
		this.focus=focus;
		this.model=model;
	}


	public Optional<String> label() {
		return get(Labels).value(Values::string);
	}

	public Optional<String> notes() {
		return get(Notes).value(Values::string);
	}


	public Value focus() {
		return focus;
	}

	public Set<Statement> model() {
		return unmodifiableSet(model);
	}


	public Stream<Statement> stream() {
		return model.stream();
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Getter get(final IRI path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return get(seq(path));
	}

	public Getter get(final BiFunction<? super Value, ? super Collection<Statement>, Stream<Value>> path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return new Getter(path.apply(focus, model).collect(toCollection(LinkedHashSet::new)), model);
	}

	public Setter set(final IRI path) {

		if ( path == null ) {
			throw new NullPointerException("null path");
		}

		return new Setter(this, path);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String toString() {
		return format(focus)
				+label().map(l -> " : "+l).orElse("")
				+notes().map(l -> " / "+l).orElse("");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public static final class Getter {

		private final Set<Value> values;
		private final Set<Statement> model;


		private Getter(final Set<Value> values, final Set<Statement> model) {
			this.values=values;
			this.model=model;
		}


		public Optional<Value> value() {
			return values().findFirst();
		}

		public <V> Optional<V> value(final Function<Value, Optional<V>> mapper) {

			if ( mapper == null ) {
				throw new NullPointerException("null mapper");
			}

			return values(mapper).findFirst();
		}


		public Stream<Value> values() {
			return values.stream();
		}

		public <V> Stream<V> values(final Function<Value, Optional<V>> mapper) {

			if ( mapper == null ) {
				throw new NullPointerException("null mapper");
			}

			return values.stream()
					.map(mapper)
					.map(Objects::requireNonNull)
					.filter(Optional::isPresent)
					.map(Optional::get);
		}


		public Optional<Frame> frame() {
			return frames().findFirst();
		}

		public Stream<Frame> frames() {
			return values.stream().map(value -> new Frame(value, model));
		}

	}

	public static final class Setter {

		private final Frame frame;
		private final IRI path;


		private Setter(final Frame frame, final IRI path) {
			this.frame=frame;
			this.path=path;
		}


		public Frame value(final Value value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return new Frame(frame.focus, Stream.concat(frame.model.stream(), traverse(path,

					direct -> {

						if ( !(frame.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for direct predicate");
						}

						return Stream.of(statement((Resource)frame.focus, direct, value));

					},

					inverse -> {

						if ( !(value instanceof Resource) ) {
							throw new IllegalArgumentException("literal value for inverse predicate");
						}

						return Stream.of(statement((Resource)value, inverse, frame.focus));

					}

			)).collect(toCollection(LinkedHashSet::new)));
		}

		public Frame value(final Optional<? extends Value> value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return value.map(this::value).orElse(frame);
		}


		public Frame values(final Value... values) {

			if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values.length == 0 ? frame : values(Arrays.stream(values));
		}

		public Frame values(final Collection<? extends Value> values) {

			if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values.isEmpty() ? frame : values(values.stream());
		}

		public Frame values(final Stream<? extends Value> values) {

			if ( values == null ) {
				throw new NullPointerException("null values");
			}

			return new Frame(frame.focus, Stream.concat(frame.model.stream(), traverse(path,

					direct -> values.map(value -> {

						if ( value == null ) {
							throw new NullPointerException("null values");
						}

						if ( !(frame.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for direct field");
						}

						return statement((Resource)frame.focus, path, value);

					}),

					inverse -> values.map(value -> {

						if ( value == null ) {
							throw new NullPointerException("null values");
						}

						if ( !(value instanceof Resource) ) {
							throw new IllegalArgumentException("literal value for inverse field");
						}

						return statement((Resource)value, inverse, frame.focus);

					})

			)).collect(toCollection(LinkedHashSet::new)));
		}


		public Frame frame(final Frame frame) {

			if ( frame == null ) {
				throw new NullPointerException("null frame");
			}

			return new Frame(this.frame.focus, Stream.concat(this.frame.model.stream(), traverse(path,

					direct -> {

						if ( !(this.frame.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for direct predicate");
						}

						return Stream.concat(
								Stream.of(statement((Resource)this.frame.focus, direct, frame.focus)),
								frame.model.stream()
						);

					},

					inverse -> {

						if ( !(frame.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for inverse predicate");
						}

						return Stream.concat(
								Stream.of(statement((Resource)frame.focus, inverse, this.frame.focus)),
								frame.model.stream()
						);

					}

			)).collect(toCollection(LinkedHashSet::new)));
		}

		public Frame frame(final Optional<Frame> frame) {

			if ( frame == null ) {
				throw new NullPointerException("null frame");
			}

			return frame.map(this::frame).orElse(this.frame);
		}


		public Frame frames(final Frame... frames) {

			if ( frames == null || Arrays.stream(frames).anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return frames.length == 0 ? frame : frames(Arrays.stream(frames));
		}

		public Frame frames(final Collection<Frame> frames) {

			if ( frames == null || frames.stream().anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return frames.isEmpty() ? frame : frames(frames.stream());
		}

		public Frame frames(final Stream<Frame> frames) {

			if ( frames == null ) {
				throw new NullPointerException("null frames");
			}

			return new Frame(frame.focus, Stream.concat(frame.model.stream(), traverse(path,

					direct -> frames.flatMap(frame -> {

						if ( frame == null ) {
							throw new NullPointerException("null frames");
						}

						if ( !(this.frame.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for direct field");
						}

						return Stream.concat(
								Stream.of(statement((Resource)this.frame.focus, direct, frame.focus)),
								frame.model.stream()
						);

					}),

					inverse -> frames.flatMap(frame -> {

						if ( frame == null ) {
							throw new NullPointerException("null frames");
						}

						if ( !(frame.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for inverse field");
						}

						return Stream.concat(
								Stream.of(statement((Resource)frame.focus, inverse, this.frame.focus)),
								frame.model.stream()
						);

					})

			)).collect(toCollection(LinkedHashSet::new)));
		}

	}

}
