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
import java.util.stream.Stream;

import static com.metreeca.json.Values.*;
import static com.metreeca.json.shifts.Alt.alt;
import static com.metreeca.json.shifts.Seq.seq;

import static java.util.Collections.*;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Stream.concat;

/**
 * Linked data frame.
 *
 * <p>Describes a linked data graph centered on a focus resource.</p>
 */
public final class Frame {

	private static final Path Labels=alt(
			RDFS.LABEL, DC.TITLE, iri("http://schema.org/", "name")
	);

	private static final Path Notes=alt(
			RDFS.COMMENT, DC.DESCRIPTION, iri("http://schema.org/", "description")
	);


	public static Frame frame(final Resource focus) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		return new Frame(focus, emptySet());
	}

	public static Frame frame(final Resource focus, final Collection<Statement> model) {

		if ( focus == null ) {
			throw new NullPointerException("null focus");
		}

		if ( model == null || model.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null model or model statement");
		}

		return new Frame(focus, merge(focus, emptySet(), model));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private static Stream<Statement> link(final Object subject, final IRI predicate, final Object object) {

		if ( !(subject instanceof Resource) ) {
			throw new IllegalArgumentException("subject is not a resource");
		}

		if ( !(object instanceof Value) ) {
			throw new IllegalArgumentException("object is not a value");
		}

		return Stream.of(statement((Resource)subject, predicate, ((Value)object)));
	}

	private static Set<Statement> merge(
			final Resource focus, final Set<Statement> model, final Iterable<Statement> delta
	) {

		final Set<Statement> merged=new LinkedHashSet<>(model);

		final Collection<Resource> visited=new HashSet<>();
		final Queue<Resource> pending=new ArrayDeque<>(singleton(focus));

		while ( !pending.isEmpty() ) {

			final Resource value=pending.poll();

			if ( visited.add(value) ) {
				delta.forEach(s -> {
					if ( value.equals(s.getSubject()) || value.equals(s.getObject()) ) {

						pending.add(s.getSubject());
						pending.add(s.getPredicate());

						if ( s.getObject().isResource() ) {
							pending.add((Resource)s.getObject());
						}

						merged.add(s);
					}
				});
			}
		}

		return merged;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	private final Resource focus;
	private final Set<Statement> model;


	private Frame(final Resource focus, final Set<Statement> model) {
		this.focus=focus;
		this.model=unmodifiableSet(model);
	}


	public boolean empty() {
		return model.isEmpty();
	}


	public Optional<String> label() {
		return string(Labels);
	}

	public Optional<String> notes() {
		return string(Notes);
	}


	/**
	 * Retrieves the frame focus.
	 *
	 * @return theIRI of the frame focus resource.
	 */
	public Resource focus() {
		return focus;
	}

	public Set<Statement> model() {
		return model;
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Stream<Statement> statements() {
		return model.stream();
	}


	public Frame statements(final Statement... statements) {

		if ( statements == null || Arrays.stream(statements).anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null statements");
		}

		return new Frame(focus, merge(focus, model, Arrays.stream(statements).collect(toList())));
	}

	public Frame statements(final Collection<Statement> statements) {

		if ( statements == null || statements.stream().anyMatch(Objects::isNull) ) {
			throw new NullPointerException("null statements");
		}

		return new Frame(focus, merge(focus, model, statements));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<Boolean> bool(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return bool(seq(predicate));
	}

	public Optional<Boolean> bool(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return value(shift).flatMap(Values::bool);
	}


	public Frame bool(final IRI predicate, final Boolean bool) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return bool == null ? this : value(predicate, Values.literal(bool));
	}

	public Frame bool(final IRI predicate, final Optional<Boolean> bool) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( bool == null ) {
			throw new NullPointerException("null bool");
		}

		return bool.map(object -> value(predicate, Values.literal(object))).orElse(this);
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<BigInteger> integer(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return integer(seq(predicate));
	}

	public Optional<BigInteger> integer(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return value(shift).flatMap(Values::integer);
	}


	public Stream<BigInteger> integers(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return integers(seq(predicate));
	}

	public Stream<BigInteger> integers(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return values(shift).map(Values::integer).filter(Optional::isPresent).map(Optional::get);
	}


	public Frame integer(final IRI predicate, final Number integer) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return integer == null ? this : integers(predicate, Stream.of(integer));
	}

	public Frame integer(final IRI predicate, final Optional<Number> integer) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( integer == null ) {
			throw new NullPointerException("null integer");
		}

		return integer.map(object -> integers(predicate, Stream.of(object))).orElse(this);
	}


	public Frame integers(final IRI predicate, final Number... integers) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( integers == null ) {
			throw new NullPointerException("null integers");
		}

		return integers.length == 0 ? this : integers(predicate, Arrays.stream(integers));
	}

	public Frame integers(final IRI predicate, final Collection<Number> integers) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( integers == null ) {
			throw new NullPointerException("null integers");
		}

		return integers.isEmpty() ? this : integers(predicate, integers.stream());
	}

	public Frame integers(final IRI predicate, final Stream<Number> integers) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( integers == null ) {
			throw new NullPointerException("null integers");
		}

		return values(predicate, integers.map(value

				-> value instanceof BigInteger ? (BigInteger)value
				: value instanceof BigDecimal ? ((BigDecimal)value).toBigInteger()
				: BigInteger.valueOf(value.longValue())

		).map(Values::literal));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<BigDecimal> decimal(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return decimal(seq(predicate));
	}

	public Optional<BigDecimal> decimal(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return value(shift).flatMap(Values::decimal);
	}


	public Stream<BigDecimal> decimals(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return decimals(seq(predicate));
	}

	public Stream<BigDecimal> decimals(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return values(shift).map(Values::decimal).filter(Optional::isPresent).map(Optional::get);
	}


	public Frame decimal(final IRI predicate, final Number decimal) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return decimal == null ? this : decimals(predicate, Stream.of(decimal));
	}

	public Frame decimal(final IRI predicate, final Optional<Number> decimal) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( decimal == null ) {
			throw new NullPointerException("null decimal");
		}

		return decimal.map(object -> decimals(predicate, Stream.of(object))).orElse(this);
	}


	public Frame decimals(final IRI predicate, final Number... decimals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( decimals == null ) {
			throw new NullPointerException("null decimals");
		}

		return decimals.length == 0 ? this : decimals(predicate, Arrays.stream(decimals));
	}

	public Frame decimals(final IRI predicate, final Collection<Number> decimals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( decimals == null ) {
			throw new NullPointerException("null decimals");
		}

		return decimals.isEmpty() ? this : decimals(predicate, decimals.stream());
	}

	public Frame decimals(final IRI predicate, final Stream<Number> decimals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( decimals == null ) {
			throw new NullPointerException("null decimals");
		}


		return values(predicate, decimals.map(value

				-> value instanceof BigDecimal ? (BigDecimal)value
				: value instanceof BigInteger ? new BigDecimal((BigInteger)value)
				: BigDecimal.valueOf(value.doubleValue())

		).map(Values::literal));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<String> string(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return string(seq(predicate));
	}

	public Optional<String> string(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return value(shift).map(Value::stringValue);
	}


	public Stream<String> strings(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return strings(seq(predicate));
	}

	public Stream<String> strings(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return values(shift).map(Value::stringValue);
	}


	public Frame string(final IRI predicate, final String string) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return string == null ? this : strings(predicate, Stream.of(string));
	}

	public Frame string(final IRI predicate, final Optional<String> string) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( string == null ) {
			throw new NullPointerException("null string");
		}

		return string.map(object -> strings(predicate, Stream.of(object))).orElse(this);
	}


	public Frame strings(final IRI predicate, final String... strings) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( strings == null ) {
			throw new NullPointerException("null strings");
		}

		return strings.length == 0 ? this : strings(predicate, Arrays.stream(strings));
	}

	public Frame strings(final IRI predicate, final Collection<String> strings) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( strings == null ) {
			throw new NullPointerException("null strings");
		}

		return strings.isEmpty() ? this : strings(predicate, strings.stream());
	}

	public Frame strings(final IRI predicate, final Stream<String> strings) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( strings == null ) {
			throw new NullPointerException("null strings");
		}

		return values(predicate, strings.map(Values::literal));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<Literal> literal(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return literal(seq(predicate));
	}

	public Optional<Literal> literal(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return literals(shift).findFirst();
	}


	public Stream<Literal> literals(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return literals(seq(predicate));
	}

	public Stream<Literal> literals(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return shift.apply(singleton(focus), model)
				.filter(Value::isLiteral)
				.map(Literal.class::cast);
	}


	public Frame literal(final IRI predicate, final Literal literal) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return literal == null ? this : literals(predicate, Stream.of(literal));
	}

	public Frame literal(final IRI predicate, final Optional<? extends Literal> literal) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( literal == null ) {
			throw new NullPointerException("null literal");
		}

		return literal.map(object -> literals(predicate, Stream.of(object))).orElse(this);
	}


	public Frame literals(final IRI predicate, final Literal... literals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( literals == null ) {
			throw new NullPointerException("null literals");
		}

		return literals.length == 0 ? this : literals(predicate, Arrays.stream(literals));
	}

	public Frame literals(final IRI predicate, final Collection<? extends Literal> literals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( literals == null ) {
			throw new NullPointerException("null literals");
		}

		return literals.isEmpty() ? this : literals(predicate, literals.stream());
	}

	public Frame literals(final IRI predicate, final Stream<? extends Literal> literals) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( literals == null ) {
			throw new NullPointerException("null literals");
		}

		return new Frame(focus, concat(model.stream(), literals.filter(Objects::nonNull)

				.flatMap(literal -> traverse(predicate,
						direct -> link(focus, direct, literal),
						inverse -> link(literal, inverse, focus)
				))

		).collect(toCollection(LinkedHashSet::new)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<Value> value(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return value(seq(predicate));
	}

	public Optional<Value> value(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return values(shift).findFirst();
	}


	public Stream<Value> values(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return values(seq(predicate));
	}

	public Stream<Value> values(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return shift.apply(singleton(focus), model);
	}


	public Frame value(final IRI predicate, final Value value) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return value == null ? this : values(predicate, Stream.of(value));
	}

	public Frame value(final IRI predicate, final Optional<? extends Value> value) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( value == null ) {
			throw new NullPointerException("null value");
		}

		return value.map(object -> values(predicate, Stream.of(object))).orElse(this);
	}


	public Frame values(final IRI predicate, final Value... values) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return values.length == 0 ? this : values(predicate, Arrays.stream(values));
	}

	public Frame values(final IRI predicate, final Collection<? extends Value> values) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return values.isEmpty() ? this : values(predicate, values.stream());
	}

	public Frame values(final IRI predicate, final Stream<? extends Value> values) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( values == null ) {
			throw new NullPointerException("null values");
		}

		return new Frame(focus, concat(model.stream(), values.filter(Objects::nonNull)

				.flatMap(value -> traverse(predicate,
						direct -> link(focus, direct, value),
						inverse -> link(value, inverse, focus)
				))

		).collect(toCollection(LinkedHashSet::new)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Optional<Frame> frame(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return frame(seq(predicate));
	}

	public Optional<Frame> frame(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return frames(shift).findFirst();
	}


	public Stream<Frame> frames(final IRI predicate) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return frames(seq(predicate));
	}

	public Stream<Frame> frames(final Shift shift) {

		if ( shift == null ) {
			throw new NullPointerException("null shift");
		}

		return shift.apply(singleton(focus), model)
				.filter(Value::isResource)
				.map(value -> frame((Resource)value, model));
	}


	public Frame frame(final IRI predicate, final Frame frame) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		return frame == null ? this : frames(predicate, Stream.of(frame));
	}

	public Frame frame(final IRI predicate, final Optional<Frame> frame) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( frame == null ) {
			throw new NullPointerException("null frame");
		}

		return frame.map(object -> frames(predicate, Stream.of(object))).orElse(this);
	}


	public Frame frames(final IRI predicate, final Frame... frames) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( frames == null ) {
			throw new NullPointerException("null frames");
		}

		return frames.length == 0 ? this : frames(predicate, Arrays.stream(frames));
	}

	public Frame frames(final IRI predicate, final Collection<Frame> frames) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( frames == null ) {
			throw new NullPointerException("null frames");
		}

		return frames.isEmpty() ? this : frames(predicate, frames.stream());
	}

	public Frame frames(final IRI predicate, final Stream<Frame> frames) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( frames == null ) {
			throw new NullPointerException("null frames");
		}

		return new Frame(focus, concat(model.stream(), frames.filter(Objects::nonNull)

				.flatMap(frame -> traverse(predicate,
						direct -> concat(link(focus, direct, frame.focus), frame.model.stream()),
						inverse -> concat(link(frame.focus, inverse, focus), frame.model.stream())
				))

		).collect(toCollection(LinkedHashSet::new)));
	}


	//// !!! ///////////////////////////////////////////////////////////////////////////////////////////////////////////

	public Frame objects(final IRI predicate, final Stream<Object> objects) {

		if ( predicate == null ) {
			throw new NullPointerException("null predicate");
		}

		if ( objects == null ) {
			throw new NullPointerException("null objects");
		}

		return new Frame(focus, concat(model.stream(), objects.filter(Objects::nonNull)

				.flatMap(object -> traverse(predicate,

						direct -> object instanceof Frame
								? concat(link(focus, direct, ((Frame)object).focus), ((Frame)object).model.stream())
								: link(focus, direct, object),

						inverse -> object instanceof Frame
								? concat(link(((Frame)object).focus, inverse, focus), ((Frame)object).model.stream())
								: link(object, inverse, focus)

				))

		).collect(toCollection(LinkedHashSet::new)));
	}


	////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

	@Override public boolean equals(final Object object) {
		return this == object || object instanceof Frame
				&& focus.equals(((Frame)object).focus)
				&& model.equals(((Frame)object).model);
	}

	@Override public int hashCode() {
		return focus.hashCode()
				^model.hashCode();
	}

	@Override public String toString() {
		return format(focus)
				+label().map(l -> " : "+l).orElse("")
				+notes().map(l -> " / "+l).orElse("")
				+(model.isEmpty() ? "" : " { … }");
	}

}
