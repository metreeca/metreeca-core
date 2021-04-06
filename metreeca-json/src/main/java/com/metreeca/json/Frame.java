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

import com.metreeca.json.shifts.Path;

import org.eclipse.rdf4j.model.*;
import org.eclipse.rdf4j.model.vocabulary.DC;
import org.eclipse.rdf4j.model.vocabulary.RDFS;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shifts.Alt.alt;
import static com.metreeca.json.shifts.Seq.seq;
import static com.metreeca.json.shifts.Step.step;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toCollection;

/**
 * Linked data frame.
 *
 * <p>Describes a linked data graph centered on a focus value.</p>
 */
public final class Frame {

	private static final Path Labels=alt(
			RDFS.LABEL, DC.TITLE, iri("http://schema.org/", "name")
	);

	private static final Path Notes=alt(
			RDFS.COMMENT, DC.DESCRIPTION, iri("http://schema.org/", "description")
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


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Value focus;
	private final Set<Statement> model;


	private Frame(final Value focus, final Set<Statement> model) {
		this.focus=focus;
		this.model=model;
	}


	public Optional<String> label() {
		return get(Labels).string();
	}

	public Optional<String> notes() {
		return get(Notes).string();
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

	public Reader get(final IRI step) {

		if ( step == null ) {
			throw new NullPointerException("null step");
		}

		return get(step(step));
	}

	public Reader get(final IRI... steps) {

		if ( steps == null || Arrays.stream(steps).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null steps");
		}

		return get(seq(steps));
	}

	public Reader get(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return new Reader(shift.apply(singleton(focus), model).collect(toCollection(LinkedHashSet::new)), model);
	}

	public Writer set(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return new Writer(this, predicate);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public String toString() {
		return format(focus)
				+label().map(l -> " : "+l).orElse("")
				+notes().map(l -> " / "+l).orElse("");
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	/**
	 * Frame read operations.
	 */
	public static final class Reader {

		private final Set<Value> values;
		private final Set<Statement> model;


		private Reader(final Set<Value> values, final Set<Statement> model) {
			this.values=values;
			this.model=model;
		}


		public Optional<Boolean> _boolean() {
			return value(Values::_boolean);
		}


		public Optional<BigInteger> integer() {
			return value(Values::integer);
		}

		public Stream<BigInteger> integers() {
			return values(Values::integer);
		}


		public Optional<BigDecimal> decimal() {
			return value(Values::decimal);
		}

		public Stream<BigDecimal> decimals() {
			return values(Values::decimal);
		}


		public Optional<String> string() {
			return value(Values::string);
		}

		public Stream<String> strings() {
			return values(Values::string);
		}


		public Optional<Value> value() {
			return values().findFirst();
		}

		public Stream<Value> values() {
			return values.stream();
		}


		public <V> Optional<V> value(final Function<Value, Optional<V>> mapper) {

			if ( mapper == null ) {
				throw new NullPointerException("null mapper");
			}

			return values().findFirst().flatMap(mapper);
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

	/**
	 * Frame write operations.
	 */
	public static final class Writer {

		private final Frame frame;
		private final IRI predicate;


		private Writer(final Frame frame, final IRI predicate) {
			this.frame=frame;
			this.predicate=predicate;
		}


		public Frame _boolean(final Boolean value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return value(literal(value));
		}

		public Frame _boolean(final Optional<Boolean> value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return value(value.map(Values::literal));
		}


		public Frame decimal(final BigDecimal value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return value(literal(value));
		}

		public Frame decimal(final Optional<BigDecimal> value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return value(value.map(Values::literal));
		}

		public Frame decimals(final BigDecimal... values) {

			if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values(Arrays.stream(values).map(Values::literal));
		}

		public Frame decimals(final Collection<BigDecimal> values) {

			if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values(values.stream().map(Values::literal));
		}

		public Frame decimals(final Stream<BigDecimal> values) {

			if ( values == null ) {
				throw new NullPointerException("null values");
			}

			return values(values.map(Values::literal));
		}


		public Frame integer(final BigInteger value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return value(literal(value));
		}

		public Frame integer(final Optional<BigInteger> values) {

			if ( values == null ) {
				throw new NullPointerException("null values");
			}

			return value(values.map(Values::literal));
		}

		public Frame integers(final BigInteger... values) {

			if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values(Arrays.stream(values).map(Values::literal));
		}

		public Frame integers(final Collection<BigInteger> values) {

			if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values(values.stream().map(Values::literal));
		}

		public Frame integers(final Stream<BigInteger> values) {

			if ( values == null ) {
				throw new NullPointerException("null values");
			}

			return values(values.map(Values::literal));
		}


		public Frame string(final String value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return value(literal(value));
		}

		public Frame string(final Optional<String> value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return value(value.map(Values::literal));
		}

		public Frame strings(final String... values) {

			if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values(Arrays.stream(values).map(Values::literal));
		}

		public Frame strings(final Collection<String> values) {

			if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values(values.stream().map(Values::literal));
		}

		public Frame strings(final Stream<String> values) {

			if ( values == null ) {
				throw new NullPointerException("null values");
			}

			return values(values.map(Values::literal));
		}


		public Frame value(final Value value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return new Frame(frame.focus, Stream.concat(frame.model.stream(), traverse(predicate,

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

			return new Frame(frame.focus, Stream.concat(frame.model.stream(), traverse(predicate,

					direct -> values.map(value -> {

						if ( value == null ) {
							throw new NullPointerException("null values");
						}

						if ( !(frame.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for direct field");
						}

						return statement((Resource)frame.focus, predicate, value);

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


		public Frame frame(final Frame value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return new Frame(this.frame.focus, Stream.concat(this.frame.model.stream(), traverse(predicate,

					direct -> {

						if ( !(this.frame.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for direct predicate");
						}

						return Stream.concat(
								Stream.of(statement((Resource)this.frame.focus, direct, value.focus)),
								value.model.stream()
						);

					},

					inverse -> {

						if ( !(value.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for inverse predicate");
						}

						return Stream.concat(
								Stream.of(statement((Resource)value.focus, inverse, this.frame.focus)),
								value.model.stream()
						);

					}

			)).collect(toCollection(LinkedHashSet::new)));
		}

		public Frame frame(final Optional<Frame> value) {

			if ( value == null ) {
				throw new NullPointerException("null value");
			}

			return value.map(this::frame).orElse(this.frame);
		}

		public Frame frames(final Frame... values) {

			if ( values == null || Arrays.stream(values).anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values.length == 0 ? frame : frames(Arrays.stream(values));
		}

		public Frame frames(final Collection<Frame> values) {

			if ( values == null || values.stream().anyMatch(Objects::isNull) ) {
				throw new NullPointerException("null values");
			}

			return values.isEmpty() ? frame : frames(values.stream());
		}

		public Frame frames(final Stream<Frame> values) {

			if ( values == null ) {
				throw new NullPointerException("null values");
			}

			return new Frame(frame.focus, Stream.concat(frame.model.stream(), traverse(predicate,

					direct -> values.flatMap(frame -> {

						if ( frame == null ) {
							throw new NullPointerException("null values");
						}

						if ( !(this.frame.focus instanceof Resource) ) {
							throw new IllegalArgumentException("literal focus value for direct field");
						}

						return Stream.concat(
								Stream.of(statement((Resource)this.frame.focus, direct, frame.focus)),
								frame.model.stream()
						);

					}),

					inverse -> values.flatMap(frame -> {

						if ( frame == null ) {
							throw new NullPointerException("null values");
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
